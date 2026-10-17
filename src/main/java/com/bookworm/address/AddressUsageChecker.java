package com.bookworm.address;

/**
 * Tells the address feature whether an address is still referenced elsewhere, so that it can refuse to delete it.
 * Implemented by features that hold addresses (currently orders), which keeps this package free of
 * dependencies on them.
 */
public interface AddressUsageChecker {

    boolean isInUse(Long addressId);
}
