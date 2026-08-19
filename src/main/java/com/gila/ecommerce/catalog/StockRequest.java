package com.gila.ecommerce.catalog;

import java.util.Objects;
import java.util.UUID;

public record StockRequest(UUID productId, int quantity) {

	public StockRequest {
		Objects.requireNonNull(productId, "productId must not be null");
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be greater than zero");
		}
	}

}
