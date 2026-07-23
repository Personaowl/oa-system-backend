package com.personaowl.oa.document.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.document.api.dto.DocumentWorkspaceResponse;
import com.personaowl.oa.document.application.SharedDocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-workspaces")
public class DocumentWorkspaceController {

    private final SharedDocumentService documentService;

    public DocumentWorkspaceController(SharedDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<DocumentWorkspaceResponse>> list(
        @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId
    ) {
        return ApiResponse.success(
            documentService.listWorkspaces(userId, roles, permissions),
            traceId
        );
    }
}
