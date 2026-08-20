package com.gila.ecommerce.ordering.internal.domain;

import java.util.Objects;
import java.util.UUID;

public class PaymentDeclinedException extends RuntimeException {

	private final UUID orderId;

	public PaymentDeclinedException(UUID orderId, String reason) {
		super(reason == null || reason.isBlank() ? "The payment was declined" : reason);
		this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
	}

	public UUID orderId() {
		return orderId;
	}

}
