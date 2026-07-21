package com.personaowl.oa.notice.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.notice.domain.dto.NoticeCreateRequest;
import com.personaowl.oa.notice.domain.dto.NoticeQueryRequest;
import com.personaowl.oa.notice.domain.dto.NoticeUpdateRequest;
import com.personaowl.oa.notice.domain.vo.NoticeDetailVO;
import com.personaowl.oa.notice.domain.vo.NoticeListItemVO;
import com.personaowl.oa.notice.domain.vo.NoticePageVO;
import com.personaowl.oa.notice.domain.vo.NoticeUnreadCountVO;
import com.personaowl.oa.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody NoticeCreateRequest request,
                                      @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                      @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                      @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:create");
        return ApiResponse.success(noticeService.create(request, requireUserId(userId)), traceId);
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> update(@PathVariable Long id,
                                      @Valid @RequestBody NoticeUpdateRequest request,
                                      @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                      @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                      @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:update");
        return ApiResponse.success(noticeService.update(id, request, requireUserId(userId)), traceId);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id,
                                      @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                      @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                      @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:delete");
        return ApiResponse.success(noticeService.delete(id, requireUserId(userId)), traceId);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Object> publish(@PathVariable Long id,
                                       @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                       @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                       @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:publish");
        return ApiResponse.success(noticeService.publish(id, requireUserId(userId)), traceId);
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<Object> offline(@PathVariable Long id,
                                       @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                       @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                       @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:offline");
        return ApiResponse.success(noticeService.offline(id, requireUserId(userId)), traceId);
    }

    @GetMapping
    public ApiResponse<NoticePageVO<NoticeListItemVO>> listAdmin(@Valid NoticeQueryRequest request,
                                                                 @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                                                 @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                                                 @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:list");
        return ApiResponse.success(noticeService.listAdmin(request, requireUserId(userId)), traceId);
    }

    @GetMapping("/{id}")
    public ApiResponse<NoticeDetailVO> detail(@PathVariable Long id,
                                              @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                              @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
                                              @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requirePermission(permissions, "notice:view");
        return ApiResponse.success(noticeService.getById(id, requireUserId(userId), true), traceId);
    }

    @GetMapping("/public")
    public ApiResponse<NoticePageVO<NoticeListItemVO>> publicList(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(noticeService.listPublished(null, requireUserId(userId)), traceId);
    }

    @GetMapping("/public/{id}")
    public ApiResponse<NoticeDetailVO> publicDetail(@PathVariable Long id,
                                                    @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                                    @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(noticeService.getById(id, requireUserId(userId), false), traceId);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NoticeDetailVO> read(@PathVariable Long id,
                                            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
                                            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(noticeService.read(id, requireUserId(userId)), traceId);
    }

    @GetMapping("/public/unread-count")
    public ApiResponse<NoticeUnreadCountVO> unreadCount(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(noticeService.unreadCount(requireUserId(userId)), traceId);
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少用户上下文");
        }
        return userId;
    }

    private void requirePermission(String permissions, String permission) {
        if (!hasPermission(permissions, permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasPermission(String permissions, String permission) {
        if (permissions == null || permissions.isBlank()) {
            return false;
        }
        Set<String> permissionSet = new HashSet<>(Arrays.asList(permissions.split("[,;\\s]+")));
        return permissionSet.contains(permission);
    }
}
