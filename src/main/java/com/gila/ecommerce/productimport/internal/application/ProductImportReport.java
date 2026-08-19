package com.gila.ecommerce.productimport.internal.application;

import java.util.List;

public record ProductImportReport(
		int totalRows,
		int created,
		int updated,
		List<ProductImportRowError> errors
) {

	public ProductImportReport {
		errors = List.copyOf(errors);
	}

}
