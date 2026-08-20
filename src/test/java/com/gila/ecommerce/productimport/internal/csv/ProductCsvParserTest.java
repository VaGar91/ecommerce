package com.gila.ecommerce.productimport.internal.csv;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductCsvParserTest {

	private static ValidatorFactory validatorFactory;

	private ProductCsvParser parser;

	@BeforeAll
	static void createValidatorFactory() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
	}

	@AfterAll
	static void closeValidatorFactory() {
		validatorFactory.close();
	}

	@BeforeEach
	void setUp() {
		parser = new ProductCsvParser(validatorFactory.getValidator());
	}

	@Test
	void parsesUtf8BomQuotedValuesAndReorderedHeaders() {
		var csv = """
				﻿sku,name,description,category,price,stock,weight_kg
				CAB-001,USB Cable,"Braided, durable cable",Electronics,12.50,30,0.125
				""";

		var result = parser.parse(input(csv));

		assertEquals(1, result.totalRows());
		assertTrue(result.errors().isEmpty());
		assertEquals("USB Cable", result.products().getFirst().name());
		assertEquals("Braided, durable cable", result.products().getFirst().description());
	}

	@Test
	void reportsAllInvalidRowsAndIgnoresBlankLines() {
		var csv = """
				name,sku,description,category,price,stock,weight_kg
				Keyboard,KEY-001,Mechanical keyboard,Electronics,49.90,20,0.800
				Duplicate,key-001,Duplicate SKU,Electronics,59.90,10,0.900
				Mouse,MOU-001,Wireless mouse,Electronics,free,15,0.120
				,DESK-001,Standing desk,Home & Office,399.00,-1,0
				<script>alert('xss')</script>,SCR-001,Unsafe product,Electronics,19.99,10,0.100

				""";

		var result = parser.parse(input(csv));

		assertEquals(5, result.totalRows());
		assertEquals(1, result.products().size());
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("sku") && error.message().contains("duplicates")));
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("price") && error.message().contains("decimal")));
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("name") && error.message().equals("is required")));
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("stock") && error.message().equals("must not be negative")));
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("weight_kg") && error.message().equals("must be greater than zero")));
		assertTrue(result.errors().stream()
				.anyMatch(error -> error.field().equals("name") && error.message().equals("must not contain HTML markup")));
	}

	@Test
	void rejectsMissingHeaders() {
		var csv = "name,sku,price\nKeyboard,KEY-001,49.90\n";

		assertThrows(InvalidCsvFileException.class, () -> parser.parse(input(csv)));
	}

	private static ByteArrayInputStream input(String csv) {
		return new ByteArrayInputStream(csv.getBytes(UTF_8));
	}

}
