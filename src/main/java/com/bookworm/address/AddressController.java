package com.bookworm.address;

import java.util.List;

import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> list(@CurrentUserId Long userId) {
        return addressService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@CurrentUserId Long userId, @Valid @RequestBody AddressRequest request) {
        return addressService.create(userId, request);
    }

    @GetMapping("/{addressId}")
    public AddressResponse get(@CurrentUserId Long userId, @PathVariable Long addressId) {
        return addressService.get(userId, addressId);
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(@CurrentUserId Long userId, @PathVariable Long addressId,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(userId, addressId, request);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long addressId) {
        addressService.delete(userId, addressId);
    }
}
