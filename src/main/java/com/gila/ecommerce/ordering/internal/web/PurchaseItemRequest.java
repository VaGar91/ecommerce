package com.gila.ecommerce.ordering.internal.web;

import java.util.UUID;

import com.gila.ecommerce.ordering.internal.application.PurchaseCommandItem;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record PurchaseItemRequest(
		@NotNull(message = "is required") UUID productId,
		@NotNull(message = "is required")
		@Min(value = 1, message = "must be at least {value}")
		@Max(value = 1_000, message = "must not exceed {value}")
		Integer quantity
) {

	PurchaseCommandItem toCommandItem() {
		return new PurchaseCommandItem(productId, quantity);
	}

}
