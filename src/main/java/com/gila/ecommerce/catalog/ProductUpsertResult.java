package com.gila.ecommerce.catalog;

public record ProductUpsertResult(
		ProductSnapshot product,
		boolean created
) {
}
