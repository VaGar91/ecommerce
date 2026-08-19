package com.gila.ecommerce.productimport.internal.csv;

import java.util.List;

import com.gila.ecommerce.catalog.ProductDraft;
import com.gila.ecommerce.productimport.internal.application.ProductImportRowError;

public record ProductCsvParseResult(
		int totalRows,
		List<ProductDraft> products,
		List<ProductImportRowError> errors
) {

	public ProductCsvParseResult {
		products = List.copyOf(products);
		errors = List.copyOf(errors);
	}

}
