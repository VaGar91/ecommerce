package com.gila.ecommerce.ordering.internal.application;

import java.util.List;

public record PurchaseCommand(
		List<PurchaseCommandItem> items,
		String paymentToken
) {

	public PurchaseCommand {
		items = items == null ? List.of() : List.copyOf(items);
	}

}
