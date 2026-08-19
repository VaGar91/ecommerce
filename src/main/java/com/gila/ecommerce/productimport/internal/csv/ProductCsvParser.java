package com.gila.ecommerce.productimport.internal.csv;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gila.ecommerce.catalog.ProductDraft;
import com.gila.ecommerce.productimport.internal.application.ProductImportRowError;

import jakarta.validation.Validator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

@Component
public class ProductCsvParser {

	private static final int MAX_ROWS = 10_000;
	private static final List<String> REQUIRED_HEADERS = List.of(
			"name", "sku", "description", "category", "price", "stock", "weight_kg"
	);
	private static final Set<String> REQUIRED_HEADER_SET = Set.copyOf(REQUIRED_HEADERS);
	private static final Map<String, String> CSV_FIELD_NAMES = Map.of("weightKg", "weight_kg");
	private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreEmptyLines(true)
			.setIgnoreSurroundingSpaces(true)
			.setTrim(true)
			.setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
			.get();

	private final Validator validator;

	public ProductCsvParser(Validator validator) {
		this.validator = validator;
	}

	public ProductCsvParseResult parse(InputStream input) {
		if (input == null) {
			throw new InvalidCsvFileException("A CSV file is required");
		}

		try (var reader = utf8Reader(input); var parser = FORMAT.parse(reader)) {
			validateHeaders(parser.getHeaderNames());
			return parseRecords(parser);
		} catch (InvalidCsvFileException exception) {
			throw exception;
		} catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
			throw new InvalidCsvFileException("The uploaded file is not a valid CSV document", exception);
		}
	}

	private ProductCsvParseResult parseRecords(CSVParser parser) {
		var products = new ArrayList<ProductDraft>();
		var errors = new ArrayList<ProductImportRowError>();
		var firstRowBySku = new HashMap<String, Long>();
		var totalRows = 0;

		for (var record : parser) {
			totalRows++;
			if (totalRows > MAX_ROWS) {
				throw new InvalidCsvFileException("The CSV must not contain more than " + MAX_ROWS + " product rows");
			}

			var row = parser.getCurrentLineNumber();
			if (!record.isConsistent()) {
				errors.add(new ProductImportRowError(row, "row", "must contain exactly 7 columns"));
				continue;
			}

			var rowErrors = new ArrayList<ProductImportRowError>();
			var unparsableFields = new HashSet<String>();
			var price = decimal(record, "price", row, rowErrors, unparsableFields);
			var stock = integer(record, "stock", row, rowErrors, unparsableFields);
			var weight = decimal(record, "weight_kg", row, rowErrors, unparsableFields);

			var draft = new ProductDraft(
					record.get("name"),
					record.get("sku"),
					record.get("description"),
					record.get("category"),
					price,
					stock == null ? 0 : stock,
					weight
			);

			validator.validate(draft).stream()
					.sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
					.filter(violation -> !unparsableFields.contains(csvField(violation.getPropertyPath().toString())))
					.map(violation -> new ProductImportRowError(
							row,
							csvField(violation.getPropertyPath().toString()),
							violation.getMessage()
					))
					.forEach(rowErrors::add);

			if (rowErrors.isEmpty()) {
				var normalizedSku = draft.sku().trim().toUpperCase(Locale.ROOT);
				var firstRow = firstRowBySku.putIfAbsent(normalizedSku, row);
				if (firstRow == null) {
					products.add(draft);
				} else {
					rowErrors.add(new ProductImportRowError(
							row,
							"sku",
							"duplicates SKU from row " + firstRow
					));
				}
			}

			errors.addAll(rowErrors);
		}

		if (totalRows == 0) {
			throw new InvalidCsvFileException("The CSV does not contain any product rows");
		}

		return new ProductCsvParseResult(totalRows, products, errors);
	}

	private static BigDecimal decimal(
			CSVRecord record,
			String field,
			long row,
			List<ProductImportRowError> errors,
			Set<String> unparsableFields
	) {
		var value = record.get(field);
		if (value == null || value.isBlank()) {
			errors.add(new ProductImportRowError(row, field, "is required"));
			unparsableFields.add(field);
			return null;
		}

		try {
			return new BigDecimal(value);
		} catch (NumberFormatException exception) {
			errors.add(new ProductImportRowError(row, field, "must be a decimal number"));
			unparsableFields.add(field);
			return null;
		}
	}

	private static Integer integer(
			CSVRecord record,
			String field,
			long row,
			List<ProductImportRowError> errors,
			Set<String> unparsableFields
	) {
		var value = record.get(field);
		if (value == null || value.isBlank()) {
			errors.add(new ProductImportRowError(row, field, "is required"));
			unparsableFields.add(field);
			return null;
		}

		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException exception) {
			errors.add(new ProductImportRowError(row, field, "must be an integer"));
			unparsableFields.add(field);
			return null;
		}
	}

	private static String csvField(String property) {
		return CSV_FIELD_NAMES.getOrDefault(property, property);
	}

	private static void validateHeaders(List<String> headers) {
		if (headers.size() != REQUIRED_HEADERS.size() || !Set.copyOf(headers).equals(REQUIRED_HEADER_SET)) {
			throw new InvalidCsvFileException(
					"CSV headers must contain exactly: " + String.join(", ", REQUIRED_HEADERS)
			);
		}
	}

	private static BufferedReader utf8Reader(InputStream input) throws IOException {
		var reader = new BufferedReader(new InputStreamReader(input, UTF_8));
		reader.mark(1);
		if (reader.read() != '\uFEFF') {
			reader.reset();
		}
		return reader;
	}

}
