package com.gila.ecommerce.catalog.internal.domain;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

	public InsufficientStockException(UUID productId, int requested, int available) {
		super("Product %s has %d units available but %d were requested".formatted(productId, available, requested));
	}

}
