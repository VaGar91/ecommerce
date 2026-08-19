package com.gila.ecommerce.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReservation(
		UUID productId,
		String productName,
		String sku,
		BigDecimal unitPrice,
		int quantity
) {
}
