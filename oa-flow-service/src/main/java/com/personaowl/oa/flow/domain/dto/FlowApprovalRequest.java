package com.personaowl.oa.flow.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlowApprovalRequest(
        @NotBlank(message = "审批决定不能为空") String decision,
        @Size(max = 500, message = "审批意见不能超过500个字符") String comment
) {
}
