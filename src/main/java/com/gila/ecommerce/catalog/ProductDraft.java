package com.gila.ecommerce.catalog;

import java.math.BigDecimal;

public record ProductDraft(
		String name,
		String sku,
		String description,
		String category,
		BigDecimal price,
		int stock,
		BigDecimal weightKg
) {
}
