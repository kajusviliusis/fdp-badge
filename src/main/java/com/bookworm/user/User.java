package com.bookworm.user;

import com.bookworm.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Registered customer. Registration, authentication and profile management are added with the auth increment.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "gift_points", nullable = false)
    private int giftPoints;

    /** Prevents the same gift points being spent twice by concurrent requests. */
    @Version
    private long version;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * @throws BusinessRuleException if the user has fewer than {@code points} gift points
     */
    public void deductGiftPoints(int points) {
        if (points > giftPoints) {
            throw new BusinessRuleException(
                    "Cannot redeem %d gift points; only %d are available.".formatted(points, giftPoints));
        }
        giftPoints -= points;
    }

    public void addGiftPoints(int points) {
        giftPoints += points;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getGiftPoints() {
        return giftPoints;
    }
}
