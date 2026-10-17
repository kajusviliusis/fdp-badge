package com.bookworm.address;

public record AddressResponse(
        Long id,
        String firstName,
        String lastName,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        String email) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(address.getId(), address.getFirstName(), address.getLastName(),
                address.getStreet(), address.getCity(), address.getState(), address.getPostalCode(),
                address.getCountry(), address.getPhone(), address.getEmail());
    }
}
