package com.personaowl.oa.document.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.document.api.dto.DocumentCreateRequest;
import com.personaowl.oa.document.api.dto.DocumentDetailResponse;
import com.personaowl.oa.document.api.dto.DocumentSummaryResponse;
import com.personaowl.oa.document.api.dto.DocumentUpdateRequest;
import com.personaowl.oa.document.application.SharedDocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class SharedDocumentController {

    private final SharedDocumentService documentService;

    public SharedDocumentController(SharedDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<DocumentSummaryResponse>> list(
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String keyword,
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        return ApiResponse.success(
            documentService.listDocuments(
                userId, roles, permissions, departmentId, keyword
            ),
            traceId
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDetailResponse> detail(
        @PathVariable Long id,
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        return ApiResponse.success(
            documentService.getDocument(userId, roles, permissions, id),
            traceId
        );
    }

    @PostMapping
    public ApiResponse<DocumentDetailResponse> create(
        @Valid @RequestBody DocumentCreateRequest request,
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        return ApiResponse.success(
            documentService.createDocument(userId, roles, permissions, request),
            traceId
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<DocumentDetailResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody DocumentUpdateRequest request,
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        return ApiResponse.success(
            documentService.updateDocument(userId, roles, permissions, id, request),
            traceId
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @PathVariable Long id,
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        documentService.deleteDocument(userId, roles, permissions, id);
        return ApiResponse.success(null, traceId);
    }
}
