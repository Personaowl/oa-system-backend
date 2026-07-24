package com.personaowl.oa.asset.application;

import com.personaowl.oa.asset.api.dto.*;
import com.personaowl.oa.asset.domain.AssetUserScope;
import com.personaowl.oa.asset.domain.FixedAsset;
import com.personaowl.oa.asset.domain.OfficeSupply;
import com.personaowl.oa.asset.domain.SupplyRequest;
import com.personaowl.oa.asset.infrastructure.AssetAccessMapper;
import com.personaowl.oa.asset.infrastructure.FixedAssetMapper;
import com.personaowl.oa.asset.infrastructure.OfficeSupplyMapper;
import com.personaowl.oa.asset.infrastructure.SupplyRequestMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AssetManagementService {
    private static final DateTimeFormatter REQUEST_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AssetAccessService accessService;
    private final AssetAccessMapper accessMapper;
    private final OfficeSupplyMapper supplyMapper;
    private final SupplyRequestMapper requestMapper;
    private final FixedAssetMapper fixedAssetMapper;

    public AssetManagementService(AssetAccessService accessService, AssetAccessMapper accessMapper,
                                  OfficeSupplyMapper supplyMapper, SupplyRequestMapper requestMapper,
                                  FixedAssetMapper fixedAssetMapper) {
        this.accessService = accessService;
        this.accessMapper = accessMapper;
        this.supplyMapper = supplyMapper;
        this.requestMapper = requestMapper;
        this.fixedAssetMapper = fixedAssetMapper;
    }

    @Transactional(readOnly = true)
    public List<SupplyResponse> listSupplies(Long userId, String roles, String permissions, String keyword) {
        var access = accessService.resolve(userId, roles, permissions);
        return supplyMapper.findAll(access.inventoryManager(), normalize(keyword)).stream().map(this::toSupply).toList();
    }

    @Transactional
    public SupplyResponse createSupply(Long userId, String roles, String permissions, SupplyUpsertRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        OfficeSupply supply = mapSupply(new OfficeSupply(), request);
        LocalDateTime now = LocalDateTime.now();
        supply.setCreatedBy(userId); supply.setUpdatedBy(userId); supply.setCreatedAt(now); supply.setUpdatedAt(now);
        supply.setVersion(0); supply.setDeleted(0);
        supplyMapper.insert(supply);
        return toSupply(requireSupply(supply.getId()));
    }

    @Transactional
    public SupplyResponse updateSupply(Long userId, String roles, String permissions, Long id, SupplyUpsertRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        OfficeSupply existing = requireSupply(id);
        if (request.version() == null) throw invalid("用品版本不能为空");
        mapSupply(existing, request);
        existing.setVersion(request.version());
        if (supplyMapper.updateWithVersion(existing, userId) == 0) throw conflict();
        return toSupply(requireSupply(id));
    }

    @Transactional
    public SupplyRequestResponse applySupply(Long userId, String roles, String permissions, SupplyApplyRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        if (access.user().getDepartmentId() == null) throw invalid("请先为当前账号分配部门");
        OfficeSupply supply = requireSupply(request.supplyId());
        if (supply.getStatus() != 1) throw invalid("该办公用品已停用");
        if (request.quantity() > supply.getStockQuantity()) throw invalid("申请数量不能超过当前库存");
        SupplyRequest entity = new SupplyRequest();
        LocalDateTime now = LocalDateTime.now();
        entity.setRequestNo("SR" + now.format(REQUEST_TIME) + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
        entity.setApplicantId(userId); entity.setDepartmentId(access.user().getDepartmentId()); entity.setSupplyId(request.supplyId());
        entity.setQuantity(request.quantity()); entity.setReason(request.reason().trim()); entity.setStatus("PENDING");
        entity.setCreatedAt(now); entity.setUpdatedAt(now); entity.setVersion(0); entity.setDeleted(0);
        requestMapper.insert(entity);
        return toRequest(requireSupplyRequest(entity.getId()));
    }

    @Transactional(readOnly = true)
    public List<SupplyRequestResponse> listMine(Long userId, String roles, String permissions) {
        accessService.resolve(userId, roles, permissions);
        return requestMapper.findMine(userId).stream().map(this::toRequest).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplyRequestResponse> listReviewable(Long userId, String roles, String permissions, boolean pendingOnly) {
        var access = accessService.resolve(userId, roles, permissions);
        if (!access.departmentReviewer()) return List.of();
        return requestMapper.findReviewable(access.inventoryManager(), access.visibleAssetDepartmentIds(), pendingOnly)
            .stream().map(this::toRequest).toList();
    }

    @Transactional
    public SupplyRequestResponse review(Long userId, String roles, String permissions, Long id, SupplyReviewRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        SupplyRequest entity = requireSupplyRequest(id);
        if (!access.canReview(entity.getDepartmentId())) throw forbidden("无权审批该部门的申领单");
        if (!"PENDING".equals(entity.getStatus())) throw invalid("当前申领单已经处理");
        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        String status;
        if ("APPROVE".equals(decision)) {
            if (supplyMapper.deductStock(entity.getSupplyId(), entity.getQuantity(), userId) == 0) throw invalid("库存不足或用品已停用");
            status = "APPROVED";
        } else if ("REJECT".equals(decision)) {
            status = "REJECTED";
        } else {
            throw invalid("审批决定必须为 APPROVE 或 REJECT");
        }
        if (requestMapper.review(id, status, userId, normalize(request.comment())) == 0) throw conflict();
        return toRequest(requireSupplyRequest(id));
    }

    @Transactional
    public SupplyRequestResponse issue(Long userId, String roles, String permissions, Long id) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        if (requestMapper.issue(id, userId) == 0) throw invalid("只有已审批的申领单可以确认发放");
        return toRequest(requireSupplyRequest(id));
    }

    @Transactional
    public void cancel(Long userId, String roles, String permissions, Long id) {
        accessService.resolve(userId, roles, permissions);
        if (requestMapper.cancel(id, userId) == 0) throw invalid("只有本人待审批的申领单可以撤销");
    }

    @Transactional(readOnly = true)
    public List<FixedAssetResponse> listFixedAssets(Long userId, String roles, String permissions, String keyword, String status) {
        var access = accessService.resolve(userId, roles, permissions);
        return fixedAssetMapper.findVisible(access.allAssetAccess(), access.visibleAssetDepartmentIds(), userId,
            normalize(keyword), normalizeStatusFilter(status)).stream().map(this::toFixedAsset).toList();
    }

    @Transactional
    public FixedAssetResponse createFixedAsset(Long userId, String roles, String permissions, FixedAssetUpsertRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        FixedAsset asset = mapFixedAsset(new FixedAsset(), request, false);
        if ("IN_USE".equals(asset.getStatus())) throw invalid("新建资产不能直接设为使用中，请创建后通过领用分配功能设置保管人");
        LocalDateTime now = LocalDateTime.now();
        asset.setCreatedBy(userId); asset.setUpdatedBy(userId); asset.setCreatedAt(now); asset.setUpdatedAt(now);
        asset.setVersion(0); asset.setDeleted(0);
        fixedAssetMapper.insert(asset);
        return toFixedAsset(requireFixedAsset(asset.getId()));
    }

    @Transactional
    public FixedAssetResponse updateFixedAsset(Long userId, String roles, String permissions, Long id, FixedAssetUpsertRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        FixedAsset asset = requireFixedAsset(id);
        if (request.version() == null) throw invalid("资产版本不能为空");
        String requestedStatus = normalizeAssetStatus(request.status());
        if ("IN_USE".equals(requestedStatus) && !"IN_USE".equals(asset.getStatus())) throw invalid("请使用领用分配功能设置保管人");
        if ("IN_USE".equals(asset.getStatus()) && !"IN_USE".equals(requestedStatus)) throw invalid("请先归还资产，再修改状态");
        mapFixedAsset(asset, request, true); asset.setVersion(request.version());
        if (fixedAssetMapper.updateWithVersion(asset, userId) == 0) throw conflict();
        return toFixedAsset(requireFixedAsset(id));
    }

    @Transactional
    public FixedAssetResponse assignFixedAsset(Long userId, String roles, String permissions, Long id, FixedAssetAssignRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        AssetUserScope custodian = accessMapper.findUser(request.custodianId());
        if (custodian == null || custodian.getDepartmentId() == null) throw invalid("领用人不存在或尚未分配部门");
        if (fixedAssetMapper.assign(id, request.custodianId(), custodian.getDepartmentId(), normalize(request.location()), userId, request.version()) == 0)
            throw invalid("仅闲置资产可以分配，或资产版本已变化");
        return toFixedAsset(requireFixedAsset(id));
    }

    @Transactional
    public FixedAssetResponse returnFixedAsset(Long userId, String roles, String permissions, Long id, FixedAssetReturnRequest request) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        String location = StringUtils.hasText(request.location()) ? request.location().trim() : "总部资产库";
        if (fixedAssetMapper.returnAsset(id, location, userId, request.version()) == 0) throw invalid("仅使用中的资产可以归还，或资产版本已变化");
        return toFixedAsset(requireFixedAsset(id));
    }

    @Transactional
    public void deleteFixedAsset(Long userId, String roles, String permissions, Long id) {
        var access = accessService.resolve(userId, roles, permissions);
        requireInventoryManager(access);
        if (fixedAssetMapper.softDelete(id, userId) == 0) throw invalid("使用中的资产不能删除，请先归还");
    }

    @Transactional(readOnly = true)
    public AssetOverviewResponse overview(Long userId, String roles, String permissions) {
        var access = accessService.resolve(userId, roles, permissions);
        List<SupplyResponse> supplies = supplyMapper.findAll(access.inventoryManager(), null).stream().map(this::toSupply).toList();
        List<SupplyRequest> requests = access.departmentReviewer()
            ? requestMapper.findReviewable(access.inventoryManager(), access.visibleAssetDepartmentIds(), true)
            : requestMapper.findMine(userId).stream().filter(item -> "PENDING".equals(item.getStatus())).toList();
        List<FixedAsset> assets = fixedAssetMapper.findVisible(access.allAssetAccess(), access.visibleAssetDepartmentIds(), userId, null, null);
        return new AssetOverviewResponse(supplies.size(), supplies.stream().filter(SupplyResponse::lowStock).count(), requests.size(),
            assets.size(), assets.stream().filter(a -> "IN_USE".equals(a.getStatus())).count(), assets.stream().filter(a -> "IDLE".equals(a.getStatus())).count(),
            access.inventoryManager(), access.departmentReviewer());
    }

    private OfficeSupply mapSupply(OfficeSupply supply, SupplyUpsertRequest request) {
        supply.setName(request.name().trim()); supply.setCategory(request.category().trim()); supply.setUnit(request.unit().trim());
        supply.setStockQuantity(request.stockQuantity()); supply.setSafetyStock(request.safetyStock()); supply.setStatus(request.status() == 0 ? 0 : 1);
        return supply;
    }

    private FixedAsset mapFixedAsset(FixedAsset asset, FixedAssetUpsertRequest request, boolean updating) {
        asset.setAssetCode(request.assetCode().trim()); asset.setName(request.name().trim()); asset.setCategory(request.category().trim());
        asset.setSpecification(normalize(request.specification())); asset.setPurchaseDate(request.purchaseDate()); asset.setOriginalValue(request.originalValue());
        asset.setStatus(normalizeAssetStatus(request.status())); asset.setLocation(normalize(request.location())); asset.setRemark(normalize(request.remark()));
        if (!updating) { asset.setCustodianId(null); asset.setDepartmentId(null); }
        return asset;
    }

    private OfficeSupply requireSupply(Long id) { OfficeSupply value=supplyMapper.findActiveById(id); if(value==null) throw invalid("办公用品不存在"); return value; }
    private SupplyRequest requireSupplyRequest(Long id) { SupplyRequest value=requestMapper.findActiveById(id); if(value==null) throw invalid("申领单不存在"); return value; }
    private FixedAsset requireFixedAsset(Long id) { FixedAsset value=fixedAssetMapper.findActiveById(id); if(value==null) throw invalid("固定资产不存在"); return value; }
    private void requireInventoryManager(AssetAccessService.AccessSnapshot access) { if(!access.inventoryManager()) throw forbidden("仅管理员或 HR 可以维护资产台账"); }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_ARGUMENT, message); }
    private BusinessException forbidden(String message) { return new BusinessException(ErrorCode.FORBIDDEN, message); }
    private BusinessException conflict() { return new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "数据已被其他成员更新，请刷新后重试"); }
    private String normalize(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String normalizeStatusFilter(String value) { return StringUtils.hasText(value) ? normalizeAssetStatus(value) : null; }
    private String normalizeAssetStatus(String status) { String value=normalize(status); if(value==null) throw invalid("资产状态不能为空"); value=value.toUpperCase(Locale.ROOT); if(!List.of("IDLE","IN_USE","REPAIR","SCRAPPED").contains(value)) throw invalid("资产状态不正确"); return value; }
    private SupplyResponse toSupply(OfficeSupply s) { return new SupplyResponse(s.getId(),s.getName(),s.getCategory(),s.getUnit(),s.getStockQuantity(),s.getSafetyStock(),s.getStatus(),s.getStockQuantity()<=s.getSafetyStock(),s.getVersion(),s.getUpdatedAt()); }
    private SupplyRequestResponse toRequest(SupplyRequest r) { return new SupplyRequestResponse(r.getId(),r.getRequestNo(),r.getApplicantId(),r.getApplicantName(),r.getDepartmentId(),r.getDepartmentName(),r.getSupplyId(),r.getSupplyName(),r.getSupplyUnit(),r.getQuantity(),r.getReason(),r.getStatus(),r.getReviewerName(),r.getReviewComment(),r.getReviewedAt(),r.getIssuedAt(),r.getCreatedAt(),r.getVersion()); }
    private FixedAssetResponse toFixedAsset(FixedAsset a) { return new FixedAssetResponse(a.getId(),a.getAssetCode(),a.getName(),a.getCategory(),a.getSpecification(),a.getPurchaseDate(),a.getOriginalValue(),a.getStatus(),a.getCustodianId(),a.getCustodianName(),a.getDepartmentId(),a.getDepartmentName(),a.getLocation(),a.getRemark(),a.getVersion(),a.getUpdatedAt()); }
}
