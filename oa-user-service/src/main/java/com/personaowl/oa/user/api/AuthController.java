package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.user.api.dto.CurrentUserResponse;
import com.personaowl.oa.user.api.dto.LoginRequest;
import com.personaowl.oa.user.api.dto.LoginResponse;
import com.personaowl.oa.user.api.dto.RegisterRequest;
import com.personaowl.oa.user.api.dto.UpdateAccountRequest;
import com.personaowl.oa.user.service.AvatarStorageService;
import com.personaowl.oa.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private final AvatarStorageService avatarStorageService;

    public AuthController(AuthService authService, AvatarStorageService avatarStorageService) {
        this.authService = authService;
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(authService.login(request), traceId);
    }

    @PostMapping("/auth/register")
    public ApiResponse<CurrentUserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(authService.register(request), traceId);
    }

    @GetMapping("/users/me")
    public ApiResponse<CurrentUserResponse> currentUser(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(authService.currentUser(userId), traceId);
    }

    @PutMapping("/users/me/account")
    public ApiResponse<CurrentUserResponse> updateAccount(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @Valid @RequestBody UpdateAccountRequest request,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(authService.updateAccount(userId, request), traceId);
    }

    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CurrentUserResponse> uploadAvatar(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        avatarStorageService.store(userId, file);
        return ApiResponse.success(authService.currentUser(userId), traceId);
    }

    @GetMapping("/users/me/avatar")
    public ResponseEntity<byte[]> avatar(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId) {
        AvatarStorageService.AvatarContent content = avatarStorageService.load(userId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_TYPE, content.mediaType())
                .body(content.bytes());
    }

    @DeleteMapping("/users/me/avatar")
    public ApiResponse<CurrentUserResponse> deleteAvatar(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        avatarStorageService.remove(userId);
        return ApiResponse.success(authService.currentUser(userId), traceId);
    }
}
