package com.gila.ecommerce.payment;

public interface PaymentGateway {

	PaymentResult charge(PaymentRequest request);

}
