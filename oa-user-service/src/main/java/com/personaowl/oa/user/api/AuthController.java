package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.user.api.dto.CurrentUserResponse;
import com.personaowl.oa.user.api.dto.LoginRequest;
import com.personaowl.oa.user.api.dto.LoginResponse;
import com.personaowl.oa.user.api.dto.RegisterRequest;
import com.personaowl.oa.user.api.dto.UpdateAccountRequest;
import com.personaowl.oa.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
