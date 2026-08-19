package com.gila.ecommerce.payment;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentRequest(
		UUID orderId,
		BigDecimal amount,
		String currency,
		String paymentToken
) {

	public PaymentRequest {
		Objects.requireNonNull(orderId, "orderId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		if (amount.signum() < 0) {
			throw new IllegalArgumentException("amount must not be negative");
		}
		if (currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("currency must not be blank");
		}
		if (paymentToken == null || paymentToken.isBlank()) {
			throw new IllegalArgumentException("paymentToken must not be blank");
		}
	}

}
