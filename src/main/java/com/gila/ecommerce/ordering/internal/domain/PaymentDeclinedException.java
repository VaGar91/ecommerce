package com.gila.ecommerce.ordering.internal.domain;

public class PaymentDeclinedException extends RuntimeException {

	public PaymentDeclinedException(String reason) {
		super(reason == null || reason.isBlank() ? "The payment was declined" : reason);
	}

}
