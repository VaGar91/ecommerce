package com.gila.ecommerce.ordering.internal.web;

import java.util.List;

import com.gila.ecommerce.ordering.internal.application.PurchaseCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

record PurchaseRequest(
		@NotEmpty(message = "must contain at least one item")
		@Size(max = 100, message = "must not contain more than {max} items")
		List<@Valid PurchaseItemRequest> items,
		@NotBlank(message = "is required")
		@Size(max = 100, message = "must not exceed {max} characters")
		String paymentToken
) {

	PurchaseCommand toCommand() {
		return new PurchaseCommand(items.stream().map(PurchaseItemRequest::toCommandItem).toList(), paymentToken);
	}

}
