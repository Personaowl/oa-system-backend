package com.personaowl.oa.ai.api;

import com.personaowl.oa.ai.application.service.AiKnowledgeDocService;
import com.personaowl.oa.ai.dto.AiKnowledgeDocCreateDTO;
import com.personaowl.oa.ai.dto.AiKnowledgeDocQueryDTO;
import com.personaowl.oa.ai.dto.AiKnowledgeDocUpdateDTO;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeChunkVO;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeDocVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/knowledge-docs")
@Validated
public class AiKnowledgeDocController {

    private final AiKnowledgeDocService aiKnowledgeDocService;

    public AiKnowledgeDocController(AiKnowledgeDocService aiKnowledgeDocService) {
        this.aiKnowledgeDocService = aiKnowledgeDocService;
    }

    private static final Long ANONYMOUS_USER_ID = 0L;

    private Long defaultUserId(Long userId) {
        return userId != null ? userId : ANONYMOUS_USER_ID;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiKnowledgeDocVO create(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                   @RequestPart("file") MultipartFile file,
                                   @Valid AiKnowledgeDocCreateDTO request) {
        return aiKnowledgeDocService.create(defaultUserId(userId), request.docTitle(), request.docDomain(), request.docVersion(), request.effectiveDate(), request.sourceType(), file.getOriginalFilename(), new byte[0]);
    }

    @GetMapping
    public PageResultVO<AiKnowledgeDocVO> page(@Valid AiKnowledgeDocQueryDTO query) {
        return aiKnowledgeDocService.page(query.page(), query.size(), query.keyword(), query.docDomain(), query.status());
    }

    @GetMapping("/{id}")
    public AiKnowledgeDocVO detail(@PathVariable Long id) {
        return aiKnowledgeDocService.get(id);
    }

    @PutMapping("/{id}")
    public AiKnowledgeDocVO update(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody AiKnowledgeDocUpdateDTO request) {
        return aiKnowledgeDocService.update(defaultUserId(userId), id, request.docTitle(), request.docDomain(), request.docVersion(), request.effectiveDate());
    }

    @PostMapping("/{id}/approve")
    public AiKnowledgeDocVO approve(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                    @PathVariable Long id,
                                    @RequestParam(required = false) String remark) {
        return aiKnowledgeDocService.approve(defaultUserId(userId), id, remark);
    }

    @PostMapping("/{id}/reindex")
    public AiKnowledgeDocVO reindex(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                    @PathVariable Long id) {
        return aiKnowledgeDocService.reindex(defaultUserId(userId), id);
    }

    @PostMapping("/{id}/retire")
    public AiKnowledgeDocVO retire(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                   @PathVariable Long id) {
        return aiKnowledgeDocService.retire(defaultUserId(userId), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                       @PathVariable Long id) {
        aiKnowledgeDocService.delete(defaultUserId(userId), id);
    }

    @GetMapping("/{id}/chunks")
    public PageResultVO<AiKnowledgeChunkVO> chunks(@PathVariable Long id,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return aiKnowledgeDocService.chunks(id, page, size);
    }
}
