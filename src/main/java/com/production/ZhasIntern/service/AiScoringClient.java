package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.RecommendationDtos;

import java.util.Optional;

public interface AiScoringClient {
    Optional<RecommendationDtos.AiScoringResponseDto> score(RecommendationDtos.AiScoringRequestDto request);
}
