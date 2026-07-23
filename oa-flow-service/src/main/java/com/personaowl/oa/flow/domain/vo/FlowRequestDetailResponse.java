package com.personaowl.oa.flow.domain.vo;

import java.util.List;

public record FlowRequestDetailResponse(
        FlowRequestResponse request,
        List<FlowActionResponse> timeline
) {
}
