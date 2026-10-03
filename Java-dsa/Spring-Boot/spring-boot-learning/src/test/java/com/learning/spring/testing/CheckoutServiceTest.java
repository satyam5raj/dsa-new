package com.learning.spring.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

/*
 * ============================================================================
 *  TOPIC 09 : PURE UNIT TESTS - JUnit 5 + Mockito (no Spring at all)
 * ============================================================================
 *
 *  THE TESTING PYRAMID
 *      few   E2E / @SpringBootTest   (whole app; slow; catches wiring problems)
 *      some  SLICE tests             (@WebMvcTest, @DataJpaTest: one layer, real framework)
 *      MANY  UNIT tests (this file)  (one class, mocked collaborators; milliseconds)
 *
 *  MOCKITO VOCABULARY
 *    @Mock          a fake implementation that returns defaults (null/0/empty) until stubbed
 *    @InjectMocks   create the class under test and inject the @Mocks through its constructor
 *    given(...).willReturn(...)  stubbing (BDD style; same as when(...).thenReturn(...))
 *    then(mock).should().method(...)   verification (same as verify(mock).method(...))
 *    ArgumentCaptor capture what was passed to a mock, then assert on it
 *    Spy            a REAL object with some methods stubbed (use rarely)
 *
 *  JUNIT 5 FEATURES USED: @Nested, @DisplayName, @ParameterizedTest + @CsvSource,
 *  assertThatThrownBy (AssertJ). Structure every test as Arrange / Act / Assert
 *  (aka Given / When / Then).
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock CheckoutService.CouponRepository coupons;
    @Mock CheckoutService.PaymentGateway payments;
    @Mock CheckoutService.Notifier notifier;
    @InjectMocks CheckoutService service;

    @Captor ArgumentCaptor<BigDecimal> amountCaptor;

    @Nested
    @DisplayName("priceAfterCoupon")
    class Pricing {

        @ParameterizedTest(name = "{0} with {1}% off = {2}")
        @CsvSource({
                "1000.00, 10, 900.00",
                "999.99,  25, 749.99",
                "50.00,    0, 50.00"})
        void appliesPercentageDiscount(BigDecimal amount, int percent, BigDecimal expected) {
            given(coupons.discountPercent("CODE")).willReturn(Optional.of(percent));        // Arrange
            BigDecimal result = service.priceAfterCoupon(amount, "CODE");                   // Act
            assertThat(result).isEqualByComparingTo(expected);                              // Assert
        }

        @Test
        void unknownCoupon_noDiscount() {
            given(coupons.discountPercent("NOPE")).willReturn(Optional.empty());
            assertThat(service.priceAfterCoupon(new BigDecimal("100"), "NOPE")).isEqualByComparingTo("100");
        }

        @Test
        void noCoupon_repositoryNeverCalled() {
            service.priceAfterCoupon(new BigDecimal("100"), null);
            verifyNoInteractions(coupons);
        }

        @Test
        void nonPositiveAmount_rejected() {
            assertThatThrownBy(() -> service.priceAfterCoupon(BigDecimal.ZERO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("amount must be positive");
        }
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @BeforeEach
        void coupon() {
            given(coupons.discountPercent("WELCOME10")).willReturn(Optional.of(10));
        }

        @Test
        void chargesDiscountedAmount_andNotifies() {
            given(payments.charge(eq("cust-1"), any())).willReturn("PAY-123");

            String ref = service.checkout("cust-1", new BigDecimal("2000"), "WELCOME10");

            assertThat(ref).isEqualTo("PAY-123");
            then(payments).should().charge(eq("cust-1"), amountCaptor.capture());     // capture the argument
            assertThat(amountCaptor.getValue()).isEqualByComparingTo("1800.00");
            then(notifier).should().orderConfirmed("cust-1", "PAY-123", new BigDecimal("1800.00"));
        }

        @Test
        void paymentFails_noConfirmationSent() {
            given(payments.charge(anyString(), any())).willThrow(new IllegalStateException("card declined"));

            assertThatThrownBy(() -> service.checkout("cust-1", new BigDecimal("2000"), "WELCOME10"))
                    .hasMessage("card declined");
            then(notifier).should(never()).orderConfirmed(anyString(), anyString(), any());
        }
    }
}
