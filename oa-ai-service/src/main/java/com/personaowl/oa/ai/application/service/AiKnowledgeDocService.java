package com.personaowl.oa.ai.application.service;

import com.personaowl.oa.ai.domain.vo.AiKnowledgeChunkVO;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeDocVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface AiKnowledgeDocService {

    AiKnowledgeDocVO create(Long userId, MultipartFile file, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate, String sourceType);

    PageResultVO<AiKnowledgeDocVO> page(Integer page, Integer size, String keyword, String docDomain, String status);

    AiKnowledgeDocVO get(Long id);

    AiKnowledgeDocVO update(Long userId, Long id, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate);

    AiKnowledgeDocVO approve(Long userId, Long id, String remark);

    AiKnowledgeDocVO reindex(Long userId, Long id);

    AiKnowledgeDocVO retire(Long userId, Long id);

    void delete(Long userId, Long id);

    PageResultVO<AiKnowledgeChunkVO> chunks(Long docId, Integer page, Integer size);

    AiKnowledgeDocVO create(Long userId, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate, String sourceType, String fileName, byte[] fileBytes);
}
