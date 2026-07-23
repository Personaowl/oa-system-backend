package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import com.personaowl.oa.user.api.dto.DepartmentResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import com.personaowl.oa.user.service.DepartmentService;
import com.personaowl.oa.user.service.OrganizationExcelExportService;
import com.personaowl.oa.user.service.UserManagementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
public class OrganizationExportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DepartmentService departmentService;
    private final UserManagementService userService;
    private final OrganizationExcelExportService excelExportService;
    private final PermissionGuard permissionGuard;

    public OrganizationExportController(DepartmentService departmentService,
                                        UserManagementService userService,
                                        OrganizationExcelExportService excelExportService,
                                        PermissionGuard permissionGuard) {
        this.departmentService = departmentService;
        this.userService = userService;
        this.excelExportService = excelExportService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/api/v1/departments/export")
    public ResponseEntity<byte[]> exportDepartments(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions) {
        permissionGuard.require(permissions, "sys:dept:list");
        List<DepartmentResponse> departments = departmentService.listDepartmentsForExport();
        return excelResponse(excelExportService.exportDepartments(departments),
                "departments.xlsx", "部门数据_" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx");
    }

    @GetMapping("/api/v1/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions) {
        permissionGuard.require(permissions, "sys:user:list");
        List<UserResponse> users = userService.listUsersForExport(operatorId, roles, keyword, departmentId);
        boolean includeSalary = hasPermission(permissions, "sys:salary:view");
        return excelResponse(excelExportService.exportUsers(users, includeSalary),
                "employees.xlsx", "员工数据_" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String fallbackName, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content);
    }

    private boolean hasPermission(String permissionsHeader, String requiredPermission) {
        if (permissionsHeader == null || permissionsHeader.isBlank()) return false;
        return Arrays.stream(permissionsHeader.split("[,;\\s]+"))
                .anyMatch(value -> value.equals("system:admin") || value.equals(requiredPermission));
    }
}
