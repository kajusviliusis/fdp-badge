package com.bookworm.giftpoints;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param rupeesPerPoint amount a customer pays (in ₹) to earn one gift point
 */
@ConfigurationProperties(prefix = "bookworm.gift-points")
public record GiftPointsProperties(@DefaultValue("10") int rupeesPerPoint) {
}
