package com.checkout.payment.gateway.controller.gateway.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentRequestValidator();
  }

  @Test
  void validRequestReturnsNoErrors() {
    PostPaymentRequest request = createValidRequest();
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  // Card number validation
  @Test
  void nullCardNumberReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber(null);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Card number is required"));
  }

  @Test
  void cardNumberTooShortReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber("1234567890123"); // 13 chars
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("14 and 19"));
  }

  @Test
  void cardNumberTooLongReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber("12345678901234567890"); // 20 chars
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("14 and 19"));
  }

  @Test
  void cardNumberWith14CharsIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber("12345678901237"); // 14 chars, ends in odd
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void cardNumberWith19CharsIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber("1234567890123456789"); // 19 chars
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void nonNumericCardNumberReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCardNumber("2222ABCD43248877");
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("numeric"));
  }

  // Expiry month validation
  @Test
  void expiryMonthZeroReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setExpiryMonth(0);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Expiry month"));
  }

  @Test
  void expiryMonth13ReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setExpiryMonth(13);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Expiry month"));
  }

  // Expiry date in future validation
  @Test
  void pastExpiryDateReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setExpiryMonth(1);
    request.setExpiryYear(2020);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("future"));
  }

  // Currency validation
  @Test
  void nullCurrencyReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency(null);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Currency is required"));
  }

  @Test
  void unsupportedCurrencyReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency("JPY");
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Currency must be one of"));
  }

  @Test
  void gbpCurrencyIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency("GBP");
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void usdCurrencyIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency("USD");
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void eurCurrencyIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCurrency("EUR");
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  // Amount validation
  @Test
  void zeroAmountReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setAmount(0);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Amount"));
  }

  @Test
  void negativeAmountReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setAmount(-1);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("Amount"));
  }

  // CVV validation
  @Test
  void nullCvvReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv(null);
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("CVV is required"));
  }

  @Test
  void cvvTooShortReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv("12");
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("CVV must be 3 or 4"));
  }

  @Test
  void cvvTooLongReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv("12345");
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("CVV must be 3 or 4"));
  }

  @Test
  void cvvWith3DigitsIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv("123");
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void cvvWith4DigitsIsValid() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv("1234");
    List<String> errors = validator.validate(request);
    assertThat(errors).isEmpty();
  }

  @Test
  void nonNumericCvvReturnsError() {
    PostPaymentRequest request = createValidRequest();
    request.setCvv("12A");
    List<String> errors = validator.validate(request);
    assertThat(errors).anyMatch(e -> e.contains("numeric"));
  }

  private PostPaymentRequest createValidRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }
}
