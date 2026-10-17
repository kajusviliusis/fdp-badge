package com.bookworm.payment;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/**
 * Stand-in for a payment provider; contacts nothing and is fully deterministic apart from the reference id.
 * <p>
 * Approves every payment except:
 * <ul>
 *   <li>cards that fail the Luhn check, have expired, or end in {@value #DECLINED_CARD_SUFFIX}
 *       (a test card that is always declined);</li>
 *   <li>UPI ids whose handle is {@value #DECLINED_UPI_HANDLE}.</li>
 * </ul>
 * Wallet payments are always approved. Payment details are never logged.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    static final String DECLINED_CARD_SUFFIX = "0002";
    static final String DECLINED_UPI_HANDLE = "fail";

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/uuuu");
    private static final DateTimeFormatter DATE_IN_REFERENCE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_SUFFIX_LENGTH = 5;

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SimulatedPaymentGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PaymentResult charge(PaymentRequest request, BigDecimal amount) {
        return new PaymentResult(isApproved(request), newReference());
    }

    private boolean isApproved(PaymentRequest request) {
        return switch (request.method()) {
            case CREDIT_CARD, DEBIT_CARD -> cardIsAcceptable(request.cardDetails());
            case UPI -> !handleOf(request.upiDetails().upiId()).equalsIgnoreCase(DECLINED_UPI_HANDLE);
            case WALLET -> true;
        };
    }

    private boolean cardIsAcceptable(CardDetails card) {
        return passesLuhnCheck(card.cardNumber())
                && !card.cardNumber().endsWith(DECLINED_CARD_SUFFIX)
                && !hasExpired(card.expiryDate());
    }

    /** A card is valid through the end of its expiry month. */
    private boolean hasExpired(String expiryDate) {
        return YearMonth.parse(expiryDate, EXPIRY_FORMAT).isBefore(YearMonth.now(clock));
    }

    private static String handleOf(String upiId) {
        return upiId.substring(upiId.indexOf('@') + 1);
    }

    private static boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** Format {@code TXN-yyyyMMdd-XXXXX}, e.g. {@code TXN-20240721-XK9Q2}. */
    private String newReference() {
        StringBuilder suffix = new StringBuilder(REFERENCE_SUFFIX_LENGTH);
        for (int i = 0; i < REFERENCE_SUFFIX_LENGTH; i++) {
            suffix.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return "TXN-" + clock.instant().atOffset(ZoneOffset.UTC).format(DATE_IN_REFERENCE) + "-" + suffix;
    }
}
