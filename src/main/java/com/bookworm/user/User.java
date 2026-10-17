package com.bookworm.user;

import java.util.Locale;

import com.bookworm.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A registered user: a customer, or an administrator who may also manage the catalog's categories and publishers.
 * Only the password <em>hash</em> is stored.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(name = "gift_points", nullable = false)
    private int giftPoints;

    /** Prevents the same gift points being spent twice by concurrent requests. */
    @Version
    private long version;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this(name, email, passwordHash, Role.CUSTOMER);
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** Email addresses are compared case-insensitively, so they are always stored trimmed and in lower case. */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
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

    /**
     * Takes points back, e.g. when the order that earned them is cancelled. The balance never goes below zero:
     * points the user has already spent are not clawed back.
     */
    public void revokeGiftPoints(int points) {
        giftPoints = Math.max(0, giftPoints - points);
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

    public Role getRole() {
        return role;
    }

    public int getGiftPoints() {
        return giftPoints;
    }
}
