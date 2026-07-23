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
import reactor.core.publisher.Flux;

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
        return doChat(userId, traceId, question, sessionId, knowledgeDomain, topK);
    }

    @Override
    public Flux<String> chatStream(Long userId, String traceId, String question, Long sessionId, String knowledgeDomain, Integer topK) {
        Long uid = requireUserId(userId);
        AiChatSession session = resolveSession(uid, sessionId, question, knowledgeDomain);
        return aiRagService.answerStream(question, knowledgeDomain, topK)
                .switchIfEmpty(Flux.just(""))
                .map(token -> buildStreamChunk(session.getId(), token, false))
                .concatWith(Flux.just(buildStreamChunk(session.getId(), "", true)))
                .concatWith(Flux.just("data: [DONE]\n\n"))
                .doOnComplete(() -> persistStreamChat(uid, traceId, question, session, knowledgeDomain, topK));
    }

    @Override
    public Flux<String> streamOpenAiLike(AiChatResponseVO response) {
        String content = response.answer() == null ? "" : response.answer();
        String model = "Pro/zai-org/GLM-4.7";
        long created = java.time.Instant.now().getEpochSecond();
        String id = java.util.UUID.randomUUID().toString();
        return Flux.concat(
                Flux.just(sseChunk(openAiChunk(id, created, model, "assistant", content, null))),
                Flux.just(sseChunk(openAiChunk(id, created + 1, model, null, null, "stop"))),
                Flux.just("data: [DONE]\n\n")
        );
    }

    private String buildStreamChunk(Long sessionId, String token, boolean done) {
        String id = java.util.UUID.randomUUID().toString();
        long created = java.time.Instant.now().getEpochSecond();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":\"").append(id).append("\",")
                .append("\"object\":\"chat.completion.chunk\",")
                .append("\"created\":").append(created).append(',')
                .append("\"model\":\"Pro/zai-org/GLM-4.7\",")
                .append("\"sessionId\":").append(sessionId).append(',')
                .append("\"choices\":[{\"index\":0,\"delta\":{");
        if (!done) {
            sb.append("\"role\":\"assistant\",\"content\":\"").append(escapeJson(token)).append("\"");
        }
        sb.append("},\"finish_reason\":").append(done ? "\"stop\"" : "null").append("}]}\n\n");
        return "data: " + sb;
    }

    private void persistStreamChat(Long userId, String traceId, String question, AiChatSession session, String knowledgeDomain, Integer topK) {
        AiChatResponseVO response = doChat(userId, traceId, question, session.getId(), knowledgeDomain, topK);
        session.setLatestAnswer(response.answer());
        aiChatSessionMapper.updateById(session);
    }

    private String openAiChunk(String id, long created, String model, String role, String content, String finishReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":\"").append(id).append("\",")
                .append("\"object\":\"chat.completion.chunk\",")
                .append("\"created\":").append(created).append(',')
                .append("\"model\":\"").append(model).append("\",")
                .append("\"choices\":[{")
                .append("\"index\":0,")
                .append("\"delta\":{");
        boolean hasDelta = false;
        if (role != null) {
            sb.append("\"role\":\"").append(role).append("\"");
            hasDelta = true;
        }
        if (content != null) {
            if (hasDelta) sb.append(',');
            sb.append("\"content\":\"").append(escapeJson(content)).append("\"");
            hasDelta = true;
        }
        sb.append("},\"");
        sb.append("finish_reason\":").append(finishReason == null ? "null" : "\"" + finishReason + "\"");
        sb.append("}]}" );
        return sb.toString();
    }

    private String sseChunk(String json) {
        return "data: " + json + "\n\n";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private AiChatResponseVO doChat(Long userId, String traceId, String question, Long sessionId, String knowledgeDomain, Integer topK) {
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
        Long uid = requireUserId(userId);
        List<AiChatSession> sessions = aiChatSessionMapper.selectPage(uid, keyword, status, offset(page, size), size == null ? 20 : size);
        List<AiChatSessionVO> vos = sessions.stream().map(this::toVO).toList();
        return new PageResultVO<>(vos, page, size, vos.size());
    }

    @Override
    public AiChatSessionVO getSession(Long userId, Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null) {
            return AiChatSessionVO.empty(sessionId);
        }
        return toVO(session, aiChatLogMapper.selectBySessionId(sessionId));
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
        return new PageResultVO<>(vos, page, size, vos.size());
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
        return toVO(session, List.of());
    }

    private AiChatSessionVO toVO(AiChatSession session, List<AiChatLog> logs) {
        List<AiChatSessionVO.AiChatMessageVO> messages = logs.stream()
                .flatMap(log -> java.util.stream.Stream.of(
                        new AiChatSessionVO.AiChatMessageVO("user", log.getQuestion(), List.of(), toOffsetDateTime(log.getCreatedAt())),
                        new AiChatSessionVO.AiChatMessageVO("assistant", log.getAnswer(), List.of(), toOffsetDateTime(log.getCreatedAt()))))
                .toList();
        return new AiChatSessionVO(session.getId(), session.getSessionNo(), session.getSessionTitle(), session.getKnowledgeDomain(),
                session.getLatestQuestion(), session.getLatestAnswer(), session.getMessageCount(), session.getStatus(),
                toOffsetDateTime(session.getCreatedAt()), messages);
    }

    private AiChatLogVO toVO(AiChatLog log) {
        return new AiChatLogVO(log.getId(), log.getSessionId(), log.getUserId(), log.getQuestion(), log.getAnswer(),
                List.of(), log.getModelName(), log.getTopK(),
                log.getConfidenceScore() == null ? null : log.getConfidenceScore().doubleValue(), log.getHitFlag() != null && log.getHitFlag() == 1,
                log.getLatencyMs(), log.getCreatedAt() == null ? null : log.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)));
    }

    private java.time.OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(java.time.ZoneOffset.ofHours(8));
    }

    private int offset(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        return (p - 1) * s;
    }
}
