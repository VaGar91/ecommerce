package com.gila.ecommerce.payment;

public record PaymentResult(
		boolean approved,
		String paymentReference,
		String failureReason
) {

	public static PaymentResult approved(String paymentReference) {
		return new PaymentResult(true, paymentReference, null);
	}

	public static PaymentResult declined(String failureReason) {
		return new PaymentResult(false, null, failureReason);
	}

}
