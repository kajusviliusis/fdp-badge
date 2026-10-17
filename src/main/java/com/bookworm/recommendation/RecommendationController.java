package com.bookworm.recommendation;

import java.util.List;

import com.bookworm.book.BookSummaryResponse;
import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<BookSummaryResponse> recommendations(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return recommendationService.recommendFor(userId, limit);
    }
}
