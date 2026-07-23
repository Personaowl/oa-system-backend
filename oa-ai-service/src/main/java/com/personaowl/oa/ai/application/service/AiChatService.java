package com.personaowl.oa.ai.application.service;

import com.personaowl.oa.ai.domain.vo.AiChatLogVO;
import com.personaowl.oa.ai.domain.vo.AiChatResponseVO;
import com.personaowl.oa.ai.domain.vo.AiChatSessionVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import reactor.core.publisher.Flux;

public interface AiChatService {

    AiChatResponseVO chat(Long userId, String traceId, String question, Long sessionId, String knowledgeDomain, Integer topK, Boolean stream);
    Flux<String> chatStream(Long userId, String traceId, String question, Long sessionId, String knowledgeDomain, Integer topK);
    PageResultVO<AiChatSessionVO> pageSessions(Long userId, Integer page, Integer size, String keyword, String status);

    AiChatSessionVO getSession(Long userId, Long sessionId);

    AiChatSessionVO archiveSession(Long userId, Long sessionId);

    void deleteSession(Long userId, Long sessionId);

    PageResultVO<AiChatLogVO> pageLogs(Long userId, Integer page, Integer size, Long queryUserId, String keyword, String knowledgeDomain, Boolean hitFlag);

    AiChatLogVO getLog(Long userId, Long logId);
}
