package com.learning.spring.testing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/*
 * ============================================================================
 *  TOPIC 09 : A CLASS DESIGNED FOR UNIT TESTING
 * ============================================================================
 *
 *  Notice what makes this easy to test WITHOUT Spring:
 *    - all collaborators come through the CONSTRUCTOR (no field injection, no 'new' inside)
 *    - collaborators are INTERFACES -> tests pass Mockito mocks
 *  See CheckoutServiceTest: a plain JUnit 5 + Mockito test that runs in milliseconds.
 */
@Service
public class CheckoutService {

    public interface CouponRepository {
        Optional<Integer> discountPercent(String couponCode);
    }

    public interface PaymentGateway {
        String charge(String customerId, BigDecimal amount);
    }

    public interface Notifier {
        void orderConfirmed(String customerId, String paymentRef, BigDecimal amount);
    }

    private final CouponRepository coupons;
    private final PaymentGateway payments;
    private final Notifier notifier;

    public CheckoutService(CouponRepository coupons, PaymentGateway payments, Notifier notifier) {
        this.coupons = coupons;
        this.payments = payments;
        this.notifier = notifier;
    }

    public BigDecimal priceAfterCoupon(BigDecimal amount, String couponCode) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        int percent = couponCode == null ? 0 : coupons.discountPercent(couponCode).orElse(0);
        BigDecimal discount = amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return amount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    public String checkout(String customerId, BigDecimal amount, String couponCode) {
        BigDecimal finalAmount = priceAfterCoupon(amount, couponCode);
        String ref = payments.charge(customerId, finalAmount);       // may throw -> no notification
        notifier.orderConfirmed(customerId, ref, finalAmount);
        return ref;
    }
}
