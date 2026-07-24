package com.personaowl.oa.asset.api;

import com.personaowl.oa.asset.api.dto.*;
import com.personaowl.oa.asset.application.AssetManagementService;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetManagementController {
    private final AssetManagementService assetService;

    public AssetManagementController(AssetManagementService assetService) {
        this.assetService = assetService;
    }

    @GetMapping("/overview")
    public ApiResponse<AssetOverviewResponse> overview(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(assetService.overview(userId, roles, permissions), traceId);
    }

    @GetMapping("/supplies")
    public ApiResponse<List<SupplyResponse>> listSupplies(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @RequestParam(required = false) String keyword) {
        return ApiResponse.success(assetService.listSupplies(userId, roles, permissions, keyword), traceId);
    }

    @PostMapping("/supplies")
    public ApiResponse<SupplyResponse> createSupply(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @Valid @RequestBody SupplyUpsertRequest request) {
        return ApiResponse.success(assetService.createSupply(userId, roles, permissions, request), traceId);
    }

    @PutMapping("/supplies/{id}")
    public ApiResponse<SupplyResponse> updateSupply(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id, @Valid @RequestBody SupplyUpsertRequest request) {
        return ApiResponse.success(assetService.updateSupply(userId, roles, permissions, id, request), traceId);
    }

    @GetMapping("/supply-requests/mine")
    public ApiResponse<List<SupplyRequestResponse>> listMine(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(assetService.listMine(userId, roles, permissions), traceId);
    }

    @GetMapping("/supply-requests/reviewable")
    public ApiResponse<List<SupplyRequestResponse>> listReviewable(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @RequestParam(defaultValue = "true") boolean pendingOnly) {
        return ApiResponse.success(assetService.listReviewable(userId, roles, permissions, pendingOnly), traceId);
    }

    @PostMapping("/supply-requests")
    public ApiResponse<SupplyRequestResponse> applySupply(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @Valid @RequestBody SupplyApplyRequest request) {
        return ApiResponse.success(assetService.applySupply(userId, roles, permissions, request), traceId);
    }

    @PostMapping("/supply-requests/{id}/review")
    public ApiResponse<SupplyRequestResponse> review(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id, @Valid @RequestBody SupplyReviewRequest request) {
        return ApiResponse.success(assetService.review(userId, roles, permissions, id, request), traceId);
    }

    @PostMapping("/supply-requests/{id}/issue")
    public ApiResponse<SupplyRequestResponse> issue(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id) {
        return ApiResponse.success(assetService.issue(userId, roles, permissions, id), traceId);
    }

    @PostMapping("/supply-requests/{id}/cancel")
    public ApiResponse<Void> cancel(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id) {
        assetService.cancel(userId, roles, permissions, id);
        return ApiResponse.success(null, traceId);
    }

    @GetMapping("/fixed-assets")
    public ApiResponse<List<FixedAssetResponse>> listFixedAssets(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String status) {
        return ApiResponse.success(assetService.listFixedAssets(userId, roles, permissions, keyword, status), traceId);
    }

    @PostMapping("/fixed-assets")
    public ApiResponse<FixedAssetResponse> createFixedAsset(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @Valid @RequestBody FixedAssetUpsertRequest request) {
        return ApiResponse.success(assetService.createFixedAsset(userId, roles, permissions, request), traceId);
    }

    @PutMapping("/fixed-assets/{id}")
    public ApiResponse<FixedAssetResponse> updateFixedAsset(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id, @Valid @RequestBody FixedAssetUpsertRequest request) {
        return ApiResponse.success(assetService.updateFixedAsset(userId, roles, permissions, id, request), traceId);
    }

    @PostMapping("/fixed-assets/{id}/assign")
    public ApiResponse<FixedAssetResponse> assign(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id, @Valid @RequestBody FixedAssetAssignRequest request) {
        return ApiResponse.success(assetService.assignFixedAsset(userId, roles, permissions, id, request), traceId);
    }

    @PostMapping("/fixed-assets/{id}/return")
    public ApiResponse<FixedAssetResponse> returnAsset(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id, @Valid @RequestBody FixedAssetReturnRequest request) {
        return ApiResponse.success(assetService.returnFixedAsset(userId, roles, permissions, id, request), traceId);
    }

    @DeleteMapping("/fixed-assets/{id}")
    public ApiResponse<Void> deleteFixedAsset(@RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
        @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
        @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
        @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
        @PathVariable Long id) {
        assetService.deleteFixedAsset(userId, roles, permissions, id);
        return ApiResponse.success(null, traceId);
    }
}
