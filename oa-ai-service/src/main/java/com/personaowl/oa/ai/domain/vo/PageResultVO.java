package com.personaowl.oa.ai.domain.vo;

import java.util.List;

public record PageResultVO<T>(List<T> list, Integer page, Integer size, Integer total) {
}
