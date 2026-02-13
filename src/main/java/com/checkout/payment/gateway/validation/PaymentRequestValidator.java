package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PaymentRequestValidator {

  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("GBP", "USD", "EUR");

  /**
   * Returns an empty list means the request is valid.
   */
  public List<String> validate(PostPaymentRequest request) {
    List<String> errors = new ArrayList<>();

    validateCardNumber(request.getCardNumber(), errors);
    validateExpiryMonth(request.getExpiryMonth(), errors);
    validateExpiryYear(request.getExpiryYear(), errors);
    validateExpiryDateInFuture(request.getExpiryMonth(), request.getExpiryYear(), errors);
    validateCurrency(request.getCurrency(), errors);
    validateAmount(request.getAmount(), errors);
    validateCvv(request.getCvv(), errors);

    return errors;
  }

  private void validateCardNumber(String cardNumber, List<String> errors) {
    if (cardNumber == null || cardNumber.isBlank()) {
      errors.add("Card number is required");
      return;
    }
    if (cardNumber.length() < 14 || cardNumber.length() > 19) {
      errors.add("Card number must be between 14 and 19 characters long");
    }
    if (!cardNumber.matches("\\d+")) {
      errors.add("Card number must contain only numeric characters");
    }
  }

  private void validateExpiryMonth(int expiryMonth, List<String> errors) {
    if (expiryMonth < 1 || expiryMonth > 12) {
      errors.add("Expiry month must be between 1 and 12");
    }
  }

  private void validateExpiryYear(int expiryYear, List<String> errors) {
    if (expiryYear < 1) {
      errors.add("Expiry year is required");
    }
  }

  private void validateExpiryDateInFuture(int expiryMonth, int expiryYear, List<String> errors) {
    if (expiryMonth < 1 || expiryMonth > 12 || expiryYear < 1) {
      return;
    }
    YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
    YearMonth now = YearMonth.now();
    if (!expiry.isAfter(now)) {
      errors.add("Card expiry date must be in the future");
    }
  }

  private void validateCurrency(String currency, List<String> errors) {
    if (currency == null || currency.isBlank()) {
      errors.add("Currency is required");
      return;
    }
    if (currency.length() != 3) {
      errors.add("Currency must be 3 characters");
    }
    if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
      errors.add("Currency must be one of: " + SUPPORTED_CURRENCIES);
    }
  }

  private void validateAmount(int amount, List<String> errors) {
    if (amount <= 0) {
      errors.add("Amount must be a positive integer");
    }
  }

  private void validateCvv(String cvv, List<String> errors) {
    if (cvv == null || cvv.isBlank()) {
      errors.add("CVV is required");
      return;
    }
    if (cvv.length() < 3 || cvv.length() > 4) {
      errors.add("CVV must be 3 or 4 characters long");
    }
    if (!cvv.matches("\\d+")) {
      errors.add("CVV must contain only numeric characters");
    }
  }
}
