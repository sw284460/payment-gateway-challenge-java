package com.checkout.payment.gateway.controller.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock private PaymentsRepository paymentsRepository;
  @Mock private BankClient bankClient;
  @Mock private PaymentRequestValidator validator;

  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, bankClient, validator);
  }

  @Test
  void getPaymentById_found() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse expected = new PostPaymentResponse();
    expected.setId(id);
    expected.setStatus(PaymentStatus.AUTHORIZED);
    when(paymentsRepository.get(id)).thenReturn(Optional.of(expected));

    PostPaymentResponse result = service.getPaymentById(id);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getPaymentById_notFound_throws() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPaymentById(id))
        .isInstanceOf(EventProcessingException.class);
  }

  @Test
  void testValidationFailureRejectsWithoutCallingBank() {
    PostPaymentRequest req = makeRequest();
    when(validator.validate(req)).thenReturn(List.of("Card number is required"));

    PostPaymentResponse resp = service.processPayment(req);

    assertThat(resp.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    verify(bankClient, never()).processPayment(any());
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void testAuthorizedPayment() {
    PostPaymentRequest req = makeRequest();
    when(validator.validate(req)).thenReturn(Collections.emptyList());

    BankPaymentResponse bankResp = new BankPaymentResponse();
    bankResp.setAuthorized(true);
    bankResp.setAuthorizationCode("abc-123");
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(Optional.of(bankResp));

    PostPaymentResponse result = service.processPayment(req);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(result.getId()).isNotNull();
    assertThat(result.getCardNumberLastFour()).isEqualTo(8877);
    assertThat(result.getCurrency()).isEqualTo("GBP");
    assertThat(result.getAmount()).isEqualTo(100);
    verify(paymentsRepository).add(result);
  }

  @Test
  void testDeclinedPayment() {
    PostPaymentRequest req = makeRequest();
    when(validator.validate(req)).thenReturn(Collections.emptyList());

    BankPaymentResponse bankResp = new BankPaymentResponse();
    bankResp.setAuthorized(false);
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(Optional.of(bankResp));

    PostPaymentResponse result = service.processPayment(req);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    verify(paymentsRepository).add(result);
  }

  @Test
  void shouldRejectWhenBankUnavailable() {
    PostPaymentRequest req = makeRequest();
    when(validator.validate(req)).thenReturn(Collections.emptyList());
    when(bankClient.processPayment(any())).thenReturn(Optional.empty());

    PostPaymentResponse result = service.processPayment(req);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    verify(paymentsRepository, never()).add(any());
  }

  private PostPaymentRequest makeRequest() {
    PostPaymentRequest req = new PostPaymentRequest();
    req.setCardNumber("2222405343248877");
    req.setExpiryMonth(4);
    req.setExpiryYear(2027);
    req.setCurrency("GBP");
    req.setAmount(100);
    req.setCvv("123");
    return req;
  }
}
