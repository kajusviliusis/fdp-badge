package com.bookworm.user;

import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getProfile(@CurrentUserId Long userId) {
        return userService.getProfile(userId);
    }

    @PatchMapping
    public UserResponse updateProfile(@CurrentUserId Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(userId, request);
    }

    @GetMapping("/gift-points")
    public GiftPointsResponse getGiftPoints(@CurrentUserId Long userId) {
        return userService.getGiftPoints(userId);
    }
}
