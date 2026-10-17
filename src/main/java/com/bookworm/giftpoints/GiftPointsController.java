package com.bookworm.giftpoints;

import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations/gift-points")
public class GiftPointsController {

    private final GiftPointsService giftPointsService;

    public GiftPointsController(GiftPointsService giftPointsService) {
        this.giftPointsService = giftPointsService;
    }

    @PostMapping("/redeem")
    public GiftPointsRedemptionResponse previewRedemption(@CurrentUserId Long userId,
                                                          @Valid @RequestBody GiftPointsRedemptionRequest request) {
        return giftPointsService.previewRedemption(userId, request);
    }
}
