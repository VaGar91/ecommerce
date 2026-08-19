package com.gila.ecommerce.payment.internal;

import java.util.UUID;

import com.gila.ecommerce.payment.PaymentGateway;
import com.gila.ecommerce.payment.PaymentRequest;
import com.gila.ecommerce.payment.PaymentResult;

import org.springframework.stereotype.Component;

@Component
class FakePaymentGateway implements PaymentGateway {

	private static final String DECLINED_TOKEN = "tok_declined";

	@Override
	public PaymentResult charge(PaymentRequest request) {
		if (DECLINED_TOKEN.equalsIgnoreCase(request.paymentToken().trim())) {
			return PaymentResult.declined("The fake payment provider declined the payment");
		}

		return PaymentResult.approved("fake-" + UUID.randomUUID());
	}

}
