package com.gila.ecommerce.productimport.internal.application;

import java.io.InputStream;
import java.util.List;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.productimport.internal.csv.ProductCsvParser;

import org.springframework.stereotype.Service;

@Service
public class ProductImportService {

	private final ProductCsvParser parser;
	private final CatalogOperations catalog;

	public ProductImportService(ProductCsvParser parser, CatalogOperations catalog) {
		this.parser = parser;
		this.catalog = catalog;
	}

	public ProductImportReport importCsv(InputStream input) {
		var parsed = parser.parse(input);
		if (!parsed.errors().isEmpty()) {
			throw new ProductImportValidationException(
					new ProductImportReport(parsed.totalRows(), 0, 0, parsed.errors())
			);
		}

		var results = catalog.upsertAll(parsed.products());
		var created = (int) results.stream().filter(result -> result.created()).count();
		var updated = results.size() - created;

		return new ProductImportReport(parsed.totalRows(), created, updated, List.of());
	}

}
