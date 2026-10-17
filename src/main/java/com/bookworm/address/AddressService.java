package com.bookworm.address;

import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final List<AddressUsageChecker> usageCheckers;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository,
                          List<AddressUsageChecker> usageCheckers) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.usageCheckers = usageCheckers;
    }

    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderById(userId).stream().map(AddressResponse::from).toList();
    }

    public AddressResponse get(Long userId, Long addressId) {
        return AddressResponse.from(getOwned(userId, addressId));
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        Address address = new Address(userRepository.getAuthenticated(userId), request);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = getOwned(userId, addressId);
        address.update(request);
        return AddressResponse.from(address);
    }

    /**
     * @throws ConflictException if an order still references the address
     */
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = getOwned(userId, addressId);
        if (usageCheckers.stream().anyMatch(checker -> checker.isInUse(addressId))) {
            throw new ConflictException("Address is associated with an existing order and cannot be deleted.");
        }
        addressRepository.delete(address);
    }

    /**
     * Resolves the delivery address for checkout. Unlike {@link #getOwned}, an address that belongs to someone
     * else is reported as not found, matching the order endpoints' contract and not revealing that it exists.
     *
     * @throws ResourceNotFoundException if the address does not exist or is not the user's
     */
    public Address getForCheckout(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(address -> address.isOwnedBy(userId))
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
    }

    /**
     * Loads an address and verifies it belongs to the given user.
     *
     * @throws ResourceNotFoundException if the address does not exist
     * @throws ForbiddenException        if it belongs to another user
     */
    public Address getOwned(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
        if (!address.isOwnedBy(userId)) {
            throw new ForbiddenException("You do not have access to this address.");
        }
        return address;
    }
}
