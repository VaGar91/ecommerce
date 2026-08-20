package com.gila.ecommerce;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductImportControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM products");
	}

	@Test
	void createsThenUpdatesProductsBySku() throws Exception {
		var initial = csvFile("""
				name,sku,description,category,price,stock,weight_kg
				USB Cable,CAB-001,"Braided, durable cable",Electronics,12.50,30,0.125
				Desk Lamp,LAM-002,Adjustable LED lamp,Home & Office,39.90,15,1.200
				""");

		mockMvc.perform(multipart("/api/product-imports").file(initial))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRows").value(2))
				.andExpect(jsonPath("$.created").value(2))
				.andExpect(jsonPath("$.updated").value(0))
				.andExpect(jsonPath("$.rejected").value(0))
				.andExpect(jsonPath("$.errors.length()").value(0));

		var updated = csvFile("""
				name,sku,description,category,price,stock,weight_kg
				Premium USB Cable,cab-001,"Braided, durable cable",Electronics,15.00,25,0.125
				Desk Lamp,LAM-002,Adjustable LED lamp,Home & Office,39.90,12,1.200
				""");

		mockMvc.perform(multipart("/api/product-imports").file(updated))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.created").value(0))
				.andExpect(jsonPath("$.updated").value(2))
				.andExpect(jsonPath("$.rejected").value(0));

		assertDatabaseValue("SELECT count(*) FROM products", Long.class, 2L);
		assertDatabaseValue("SELECT name FROM products WHERE sku = 'CAB-001'", String.class, "Premium USB Cable");
	}

	@Test
	void importsValidRowsAndReportsInvalidRows() throws Exception {
		var file = csvFile("""
				name,sku,description,category,price,stock,weight_kg
				USB Cable,CAB-001,Braided cable,Electronics,12.50,30,0.125
				Wireless Mouse,MOU-002,Ergonomic mouse,Electronics,free,20,0.100
				<script>alert('xss')</script>,SCR-003,Unsafe product,Electronics,19.99,10,0.100
				""");

		try (var logs = TestLogCapture.forLogger(
				"com.gila.ecommerce.productimport.internal.application.ProductImportService"
		)) {
			mockMvc.perform(multipart("/api/product-imports").file(file))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalRows").value(3))
					.andExpect(jsonPath("$.created").value(1))
					.andExpect(jsonPath("$.updated").value(0))
					.andExpect(jsonPath("$.rejected").value(2))
					.andExpect(jsonPath("$.errors[0].field").value("price"))
					.andExpect(jsonPath("$.errors[1].field").value("name"))
					.andExpect(jsonPath("$.errors[1].message").value("must not contain HTML markup"));

			assertTrue(logs.containsMessage(
					"event=product_import_completed outcome=partial total_rows=3 created=1 updated=0 rejected=2 error_count=2"
			));
		}

		assertDatabaseValue("SELECT count(*) FROM products", Long.class, 1L);
		assertDatabaseValue("SELECT name FROM products", String.class, "USB Cable");
	}

	@Test
	void rejectsAnInvalidHeaderContract() throws Exception {
		var file = csvFile("name,sku,price\nKeyboard,KEY-001,49.90\n");

		try (var logs = TestLogCapture.forLogger(
				"com.gila.ecommerce.productimport.internal.web.ProductImportExceptionHandler"
		)) {
			mockMvc.perform(multipart("/api/product-imports").file(file))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.title").value("Invalid CSV file"));

			assertTrue(logs.containsMessage("event=product_import_rejected reason=invalid_csv"));
		}
	}

	private static MockMultipartFile csvFile(String contents) {
		return new MockMultipartFile("file", "products.csv", "text/csv", contents.getBytes(UTF_8));
	}

	private <T> void assertDatabaseValue(String sql, Class<T> type, T expected) {
		var actual = jdbcTemplate.queryForObject(sql, type);
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}

}
