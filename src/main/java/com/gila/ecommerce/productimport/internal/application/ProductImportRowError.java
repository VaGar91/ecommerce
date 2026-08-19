package com.gila.ecommerce.productimport.internal.application;

public record ProductImportRowError(
		long row,
		String field,
		String message
) {
}
