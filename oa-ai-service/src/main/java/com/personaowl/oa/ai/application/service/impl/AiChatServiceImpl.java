package com.personaowl.oa.ai.application.service.impl;

import com.personaowl.oa.ai.application.service.AiChatService;
import com.personaowl.oa.ai.domain.entity.AiChatLog;
import com.personaowl.oa.ai.domain.entity.AiChatSession;
import com.personaowl.oa.ai.domain.vo.AiChatLogVO;
import com.personaowl.oa.ai.domain.vo.AiChatResponseVO;
import com.personaowl.oa.ai.domain.vo.AiChatSessionVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import com.personaowl.oa.ai.infrastructure.mapper.AiChatLogMapper;
import com.personaowl.oa.ai.infrastructure.mapper.AiChatSessionMapper;
import com.personaowl.oa.ai.infrastructure.rag.AiRagService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatLogMapper aiChatLogMapper;
    private final AiRagService aiRagService;

    public AiChatServiceImpl(AiChatSessionMapper aiChatSessionMapper,
                             AiChatLogMapper aiChatLogMapper,
                             AiRagService aiRagService) {
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.aiChatLogMapper = aiChatLogMapper;
        this.aiRagService = aiRagService;
    }

    /** 匿名用户ID，绕过鉴权时 userId 为 null，使用 0 占位以满足 NOT NULL 约束 */
    private static final Long ANONYMOUS_USER_ID = 0L;

    private Long requireUserId(Long userId) {
        return userId != null ? userId : ANONYMOUS_USER_ID;
    }

    @Override
    public AiChatResponseVO chat(Long userId, String traceId, String question, Long sessionId, String knowledgeDomain, Integer topK, Boolean stream) {
        Long uid = requireUserId(userId);
        AiChatSession session = resolveSession(uid, sessionId, question, knowledgeDomain);
        AiRagService.RagResult ragResult = aiRagService.answer(question, knowledgeDomain, topK);

        AiChatLog log = new AiChatLog();
        log.setSessionId(session.getId());
        log.setUserId(uid);
        log.setQuestion(question);
        log.setAnswer(ragResult.answer());
        log.setRetrievedDocIds("1");
        log.setRetrievedChunkIds("1");
        log.setCitationsJson("[]");
        log.setModelName("deepseek-ai");
        log.setTopK(topK == null ? 3 : topK);
        log.setConfidenceScore(BigDecimal.valueOf(ragResult.confidenceScore()));
        log.setHitFlag(ragResult.hitFlag() ? 1 : 0);
        log.setLatencyMs(10);
        log.setCreatedAt(LocalDateTime.now());
        aiChatLogMapper.insert(log);

        session.setLatestQuestion(question);
        session.setLatestAnswer(log.getAnswer());
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setUpdatedAt(LocalDateTime.now());
        aiChatSessionMapper.updateById(session);

        return new AiChatResponseVO(session.getId(), log.getAnswer(), ragResult.hitFlag(),
                ragResult.matchedDocs().stream().map(doc -> new AiChatResponseVO.MatchedDocVO(doc.docId(), doc.docTitle(), doc.docVersion())).toList(),
                ragResult.citations().stream().map(c -> new AiChatResponseVO.CitationVO(c.docId(), c.docTitle(), c.chunkId(), c.chunkNo(), c.snippet(), c.score())).toList(),
                traceId,
                null);
    }

    @Override
    public PageResultVO<AiChatSessionVO> pageSessions(Long userId, Integer page, Integer size, String keyword, String status) {
        List<AiChatSession> sessions = aiChatSessionMapper.selectPage(userId, keyword, status, offset(page, size), size == null ? 20 : size);
        List<AiChatSessionVO> vos = sessions.stream().map(this::toVO).toList();
        return new PageResultVO<>(vos, page, size, (long) vos.size());
    }

    @Override
    public AiChatSessionVO getSession(Long userId, Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        return session == null ? AiChatSessionVO.empty(sessionId) : toVO(session);
    }

    @Override
    public AiChatSessionVO archiveSession(Long userId, Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null) {
            return AiChatSessionVO.empty(sessionId);
        }
        session.setStatus("ARCHIVED");
        session.setUpdatedAt(LocalDateTime.now());
        aiChatSessionMapper.updateById(session);
        return toVO(session);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setStatus("ARCHIVED");
        aiChatSessionMapper.updateById(session);
    }

    @Override
    public PageResultVO<AiChatLogVO> pageLogs(Long userId, Integer page, Integer size, Long queryUserId, String keyword, String knowledgeDomain, Boolean hitFlag) {
        List<AiChatLog> logs = aiChatLogMapper.selectPage(userId, queryUserId, keyword, knowledgeDomain, hitFlag == null ? null : (hitFlag ? 1 : 0), offset(page, size), size == null ? 20 : size);
        List<AiChatLogVO> vos = logs.stream().map(this::toVO).toList();
        return new PageResultVO<>(vos, page, size, (long) vos.size());
    }

    @Override
    public AiChatLogVO getLog(Long userId, Long logId) {
        AiChatLog log = aiChatLogMapper.selectById(logId);
        return log == null ? AiChatLogVO.empty(logId) : toVO(log);
    }

    private AiChatSession resolveSession(Long userId, Long sessionId, String question, String knowledgeDomain) {
        if (sessionId != null) {
            AiChatSession session = aiChatSessionMapper.selectById(sessionId);
            if (session != null) {
                return session;
            }
        }
        AiChatSession session = new AiChatSession();
        session.setSessionNo("CS" + System.currentTimeMillis());
        session.setUserId(userId);
        session.setSessionTitle(question == null ? "AI 问答" : question.substring(0, Math.min(20, question.length())));
        session.setKnowledgeDomain(knowledgeDomain == null ? "ALL" : knowledgeDomain);
        session.setLatestQuestion(question);
        session.setLatestAnswer("");
        session.setMessageCount(0);
        session.setStatus("ACTIVE");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        aiChatSessionMapper.insert(session);
        return session;
    }

    private AiChatSessionVO toVO(AiChatSession session) {
        return new AiChatSessionVO(session.getId(), session.getSessionNo(), session.getSessionTitle(), session.getKnowledgeDomain(),
                session.getLatestQuestion(), session.getLatestAnswer(), session.getMessageCount(), session.getStatus(),
                session.getCreatedAt() == null ? null : session.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)), List.of());
    }

    private AiChatLogVO toVO(AiChatLog log) {
        return new AiChatLogVO(log.getId(), log.getSessionId(), log.getUserId(), log.getQuestion(), log.getAnswer(),
                List.of(), log.getModelName(), log.getTopK(),
                log.getConfidenceScore() == null ? null : log.getConfidenceScore().doubleValue(), log.getHitFlag() != null && log.getHitFlag() == 1,
                log.getLatencyMs(), log.getCreatedAt() == null ? null : log.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)));
    }

    private int offset(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        return (p - 1) * s;
    }
}
