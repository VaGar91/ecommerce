package com.gila.ecommerce.productimport.internal.application;

import java.io.InputStream;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.productimport.internal.csv.ProductCsvParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductImportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductImportService.class);

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

		var report = new ProductImportReport(parsed.totalRows(), created, updated, rejected, parsed.errors());
		if (rejected == 0) {
			LOGGER.info(
					"event=product_import_completed outcome=accepted total_rows={} created={} updated={} rejected=0",
					report.totalRows(),
					report.created(),
					report.updated()
			);
		} else {
			LOGGER.warn(
					"event=product_import_completed outcome=partial total_rows={} created={} updated={} rejected={} error_count={}",
					report.totalRows(),
					report.created(),
					report.updated(),
					report.rejected(),
					report.errors().size()
			);
		}

		return report;
	}

}
