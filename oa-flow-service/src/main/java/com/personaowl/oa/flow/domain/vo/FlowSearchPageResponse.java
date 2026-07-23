package com.personaowl.oa.flow.domain.vo;

import java.util.List;

public record FlowSearchPageResponse(long total, List<FlowSearchItemResponse> records) {
}
