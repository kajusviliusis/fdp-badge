package com.bookworm.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SimulatedPaymentGatewayTest {

    private static final Instant NOW = Instant.parse("2025-03-15T09:30:00Z");
    private static final BigDecimal AMOUNT = new BigDecimal("470.00");

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(Clock.fixed(NOW, ZoneOffset.UTC));

    private static PaymentRequest card(String number, String expiry) {
        return new PaymentRequest(PaymentMethod.CREDIT_CARD, new CardDetails(number, "Arjun Patel", "123", expiry), null);
    }

    private static PaymentRequest upi(String id) {
        return new PaymentRequest(PaymentMethod.UPI, null, new UpiDetails(id));
    }

    @Test
    void approvesValidCard() {
        assertThat(gateway.charge(card("4111111111111111", "12/2030"), AMOUNT).approved()).isTrue();
    }

    @Test
    void cardIsValidThroughTheEndOfItsExpiryMonth() {
        assertThat(gateway.charge(card("4111111111111111", "03/2025"), AMOUNT).approved()).isTrue();
        assertThat(gateway.charge(card("4111111111111111", "02/2025"), AMOUNT).approved()).isFalse();
    }

    @Test
    void declinesCardThatFailsLuhnCheck() {
        assertThat(gateway.charge(card("4111111111111112", "12/2030"), AMOUNT).approved()).isFalse();
    }

    @Test
    void declinesTheDesignatedTestCard() {
        assertThat(gateway.charge(card("4000000000000002", "12/2030"), AMOUNT).approved()).isFalse();
    }

    @Test
    void debitCardsAreTreatedLikeCreditCards() {
        PaymentRequest request = new PaymentRequest(PaymentMethod.DEBIT_CARD,
                new CardDetails("4111111111111111", "Arjun Patel", "123", "12/2030"), null);

        assertThat(gateway.charge(request, AMOUNT).approved()).isTrue();
    }

    @Test
    void approvesUpiAndDeclinesTheFailHandle() {
        assertThat(gateway.charge(upi("arjun@okaxis"), AMOUNT).approved()).isTrue();
        assertThat(gateway.charge(upi("arjun@fail"), AMOUNT).approved()).isFalse();
        assertThat(gateway.charge(upi("arjun@FAIL"), AMOUNT).approved()).isFalse();
    }

    @Test
    void approvesWallet() {
        assertThat(gateway.charge(new PaymentRequest(PaymentMethod.WALLET, null, null), AMOUNT).approved()).isTrue();
    }

    @Test
    void referenceHasDocumentedFormatAndIsPresentEvenWhenDeclined() {
        PaymentResult approved = gateway.charge(upi("arjun@okaxis"), AMOUNT);
        PaymentResult declined = gateway.charge(upi("arjun@fail"), AMOUNT);

        assertThat(approved.transactionReference()).matches("TXN-20250315-[A-Z2-9]{5}");
        assertThat(declined.transactionReference()).matches("TXN-20250315-[A-Z2-9]{5}");
        assertThat(approved.transactionReference()).isNotEqualTo(declined.transactionReference());
    }

    @Test
    void sensitiveDetailsAreRedactedInToString() {
        PaymentRequest request = card("4111111111111111", "12/2030");

        assertThat(request.toString()).doesNotContain("4111111111111111").doesNotContain("123");
        assertThat(upi("arjun@okaxis").toString()).doesNotContain("arjun");
    }
}
