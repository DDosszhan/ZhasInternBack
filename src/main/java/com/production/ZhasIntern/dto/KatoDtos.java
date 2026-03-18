package com.production.ZhasIntern.dto;

import java.util.List;

public class KatoDtos {

    public record KatoNodeOption(
            Integer id,
            Integer parent,
            Integer level,
            Long code,
            String nameRu,
            String nameKz
    ) {
    }

    public record KatoListResponse(List<KatoNodeOption> items) {
        public KatoListResponse {
            items = items == null ? List.of() : items;
        }
    }
}
