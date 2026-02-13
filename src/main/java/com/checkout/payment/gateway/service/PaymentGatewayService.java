package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;
  private final PaymentRequestValidator validator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient,
      PaymentRequestValidator validator) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
    this.validator = validator;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.info("Processing payment request");

    // Step 1: Validate the request
    List<String> validationErrors = validator.validate(paymentRequest);
    if (!validationErrors.isEmpty()) {
      LOG.warn("Payment request rejected due to validation errors: {}", validationErrors);
      PostPaymentResponse rejectedResponse = buildResponse(paymentRequest, PaymentStatus.REJECTED);
      return rejectedResponse;
    }

    // Step 2: Call the acquiring bank
    BankPaymentRequest bankRequest = new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        String.format("%02d/%d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv()
    );

    Optional<BankPaymentResponse> bankResponse = bankClient.processPayment(bankRequest);

    if (bankResponse.isEmpty()) {
      LOG.error("Bank was unavailable for payment processing");
      PostPaymentResponse rejectedResponse = buildResponse(paymentRequest, PaymentStatus.REJECTED);
      return rejectedResponse;
    }

    // Step 3: Map bank response to payment status
    PaymentStatus status = bankResponse.get().isAuthorized()
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    PostPaymentResponse response = buildResponse(paymentRequest, status);

    // Step 4: Store the payment
    paymentsRepository.add(response);
    LOG.info("Payment processed with ID {} and status {}", response.getId(), status);

    return response;
  }

  private PostPaymentResponse buildResponse(PostPaymentRequest request, PaymentStatus status) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());

    if (request.getCardNumber() != null && request.getCardNumber().length() >= 4) {
      String lastFour = request.getCardNumber()
          .substring(request.getCardNumber().length() - 4);
      response.setCardNumberLastFour(Integer.parseInt(lastFour));
    }

    return response;
  }
}
