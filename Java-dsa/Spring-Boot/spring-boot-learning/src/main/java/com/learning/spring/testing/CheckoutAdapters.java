package com.learning.spring.testing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
 * Simple "real" implementations so the app can start (CheckoutService needs
 * beans for its three interfaces). In production these would call a DB, a
 * payment provider and an email service. Unit tests replace them with mocks.
 */
@Configuration
public class CheckoutAdapters {

    @Bean
    CheckoutService.CouponRepository couponRepository() {
        Map<String, Integer> coupons = Map.of("WELCOME10", 10, "FESTIVE25", 25);
        return code -> Optional.ofNullable(coupons.get(code));
    }

    @Bean
    CheckoutService.PaymentGateway paymentGateway() {
        return (customer, amount) -> "PAY-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Bean
    CheckoutService.Notifier notifier() {
        return (customer, ref, amount) -> System.out.println("Order confirmed for " + customer + " (" + ref + ", " + amount + ")");
    }
}
