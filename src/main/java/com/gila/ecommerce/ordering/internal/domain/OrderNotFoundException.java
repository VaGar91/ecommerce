package com.gila.ecommerce.ordering.internal.domain;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(UUID orderId) {
		super("Order %s was not found".formatted(orderId));
	}

}
