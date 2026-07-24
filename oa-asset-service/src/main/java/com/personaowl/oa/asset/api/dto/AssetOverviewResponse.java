package com.personaowl.oa.asset.api.dto;

public record AssetOverviewResponse(
    long supplyKinds, long lowStockKinds, long pendingRequests,
    long fixedAssetCount, long inUseAssets, long idleAssets,
    boolean inventoryManager, boolean departmentReviewer
) {}
