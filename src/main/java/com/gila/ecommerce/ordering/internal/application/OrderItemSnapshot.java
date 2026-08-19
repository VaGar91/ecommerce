package com.gila.ecommerce.ordering.internal.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSnapshot(
		UUID productId,
		String productName,
		String sku,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal
) {
}
