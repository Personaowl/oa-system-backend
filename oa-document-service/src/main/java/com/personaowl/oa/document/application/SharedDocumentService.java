package com.personaowl.oa.document.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.document.api.dto.DocumentCreateRequest;
import com.personaowl.oa.document.api.dto.DocumentDetailResponse;
import com.personaowl.oa.document.api.dto.DocumentSummaryResponse;
import com.personaowl.oa.document.api.dto.DocumentUpdateRequest;
import com.personaowl.oa.document.api.dto.DocumentWorkspaceResponse;
import com.personaowl.oa.document.domain.DocumentWorkspace;
import com.personaowl.oa.document.domain.SharedDocument;
import com.personaowl.oa.document.infrastructure.DocumentAccessMapper;
import com.personaowl.oa.document.infrastructure.SharedDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SharedDocumentService {

    private static final int MAX_CONTENT_LENGTH = 1_000_000;
    private static final String EMPTY_DOCUMENT = """
        {"type":"doc","content":[{"type":"paragraph"}]}
        """;

    private final SharedDocumentMapper documentMapper;
    private final DocumentAccessMapper accessMapper;
    private final DocumentAccessService accessService;
    private final ObjectMapper objectMapper;

    public SharedDocumentService(
        SharedDocumentMapper documentMapper,
        DocumentAccessMapper accessMapper,
        DocumentAccessService accessService,
        ObjectMapper objectMapper
    ) {
        this.documentMapper = documentMapper;
        this.accessMapper = accessMapper;
        this.accessService = accessService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentWorkspaceResponse> listWorkspaces(
        Long userId,
        String roles,
        String permissions
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        if (access.accessibleDepartmentIds().isEmpty()) {
            return List.of();
        }
        return accessMapper.findWorkspaces(access.accessibleDepartmentIds())
            .stream()
            .map(workspace -> toWorkspaceResponse(workspace, access))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> listDocuments(
        Long userId,
        String roles,
        String permissions,
        Long departmentId,
        String keyword
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        List<Long> departmentIds = access.accessibleDepartmentIds();
        if (departmentId != null) {
            requireAccess(access, departmentId);
            departmentIds = List.of(departmentId);
        }
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return documentMapper.findVisible(departmentIds, normalizedKeyword)
            .stream()
            .map(document -> toSummary(document, access))
            .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(
        Long userId,
        String roles,
        String permissions,
        Long documentId
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        SharedDocument document = requireDocument(documentId);
        requireAccess(access, document.getDepartmentId());
        return toDetail(document, access);
    }

    @Transactional
    public DocumentDetailResponse createDocument(
        Long userId,
        String roles,
        String permissions,
        DocumentCreateRequest request
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        requireManage(access, request.departmentId());

        SharedDocument document = new SharedDocument();
        LocalDateTime now = LocalDateTime.now();
        document.setDepartmentId(request.departmentId());
        document.setTitle(normalizeTitle(request.title()));
        document.setContentJson(EMPTY_DOCUMENT.trim());
        document.setCreatedBy(userId);
        document.setUpdatedBy(userId);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        document.setVersion(0);
        document.setDeleted(0);
        documentMapper.insert(document);
        return toDetail(requireDocument(document.getId()), access);
    }

    @Transactional
    public DocumentDetailResponse updateDocument(
        Long userId,
        String roles,
        String permissions,
        Long documentId,
        DocumentUpdateRequest request
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        SharedDocument existing = requireDocument(documentId);
        requireAccess(access, existing.getDepartmentId());
        String contentJson = writeContent(request.content());
        int updated = documentMapper.updateWithVersion(
            documentId,
            normalizeTitle(request.title()),
            contentJson,
            userId,
            request.version()
        );
        if (updated == 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "文档已被其他成员更新，请刷新后再继续编辑"
            );
        }
        return toDetail(requireDocument(documentId), access);
    }

    @Transactional
    public void deleteDocument(
        Long userId,
        String roles,
        String permissions,
        Long documentId
    ) {
        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(userId, roles, permissions);
        SharedDocument existing = requireDocument(documentId);
        requireManage(access, existing.getDepartmentId());
        documentMapper.softDelete(documentId, userId);
    }

    private SharedDocument requireDocument(Long documentId) {
        if (documentId == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文档 ID 不能为空");
        }
        SharedDocument document = documentMapper.findActiveById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文档不存在或已删除");
        }
        return document;
    }

    private void requireAccess(
        DocumentAccessService.AccessSnapshot access,
        Long departmentId
    ) {
        if (!access.canAccess(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该部门工作空间");
        }
    }

    private void requireManage(
        DocumentAccessService.AccessSnapshot access,
        Long departmentId
    ) {
        if (!access.canManage(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅部门主管或管理员可以管理文档");
        }
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文档标题不能为空");
        }
        return normalized;
    }

    private String writeContent(JsonNode content) {
        try {
            String result = objectMapper.writeValueAsString(content);
            if (result.length() > MAX_CONTENT_LENGTH) {
                throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "文档内容不能超过 1 MB"
                );
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文档内容格式不正确");
        }
    }

    private JsonNode readContent(String contentJson) {
        try {
            return objectMapper.readTree(contentJson);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档内容读取失败");
        }
    }

    private DocumentWorkspaceResponse toWorkspaceResponse(
        DocumentWorkspace workspace,
        DocumentAccessService.AccessSnapshot access
    ) {
        return new DocumentWorkspaceResponse(
            workspace.getDepartmentId(),
            workspace.getDepartmentName(),
            workspace.getMemberCount(),
            workspace.getDocumentCount(),
            access.canManage(workspace.getDepartmentId())
        );
    }

    private DocumentSummaryResponse toSummary(
        SharedDocument document,
        DocumentAccessService.AccessSnapshot access
    ) {
        return new DocumentSummaryResponse(
            document.getId(),
            document.getDepartmentId(),
            document.getDepartmentName(),
            document.getTitle(),
            document.getCreatedByName(),
            document.getUpdatedByName(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getVersion(),
            access.canManage(document.getDepartmentId())
        );
    }

    private DocumentDetailResponse toDetail(
        SharedDocument document,
        DocumentAccessService.AccessSnapshot access
    ) {
        return new DocumentDetailResponse(
            document.getId(),
            document.getDepartmentId(),
            document.getDepartmentName(),
            document.getTitle(),
            readContent(document.getContentJson()),
            document.getCreatedByName(),
            document.getUpdatedByName(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getVersion(),
            access.canManage(document.getDepartmentId())
        );
    }
}
