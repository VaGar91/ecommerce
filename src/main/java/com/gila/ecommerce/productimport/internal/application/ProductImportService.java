package com.gila.ecommerce.productimport.internal.application;

import java.io.InputStream;

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
		var results = catalog.upsertAll(parsed.products());
		var created = (int) results.stream().filter(result -> result.created()).count();
		var updated = results.size() - created;
		var rejected = parsed.totalRows() - results.size();

		return new ProductImportReport(parsed.totalRows(), created, updated, rejected, parsed.errors());
	}

}
