package com.bookworm.user;

public record UserResponse(Long id, String name, String email, int giftPoints) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getGiftPoints());
    }
}
