package com.bookworm.book;

import java.time.Instant;
import java.util.List;

/**
 * Sales figures the catalog needs from the order history, defined here so that the catalog does not depend on
 * the order feature.
 */
public interface SalesReport {

    /**
     * Ids of the best-selling books among orders paid within the period, most copies first (ties by lowest id).
     * Unpaid, cancelled and out-of-period orders are ignored; books without sales do not appear.
     *
     * @param from  start of the period, inclusive
     * @param until end of the period, exclusive
     */
    List<Long> topSellingBookIds(Instant from, Instant until, int limit);
}
