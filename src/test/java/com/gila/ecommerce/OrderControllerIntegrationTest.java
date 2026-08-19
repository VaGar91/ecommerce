package com.gila.ecommerce;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM purchase_order_items");
		jdbcTemplate.update("DELETE FROM purchase_orders");
		jdbcTemplate.update("DELETE FROM products");
	}

	@Test
	void purchasesProductsAndReturnsAnAuditableOrder() throws Exception {
		var keyboardId = createProduct("Mechanical Keyboard", "KEY-001", "49.90", 25);
		var cableId = createProduct("USB Cable", "CAB-002", "20.00", 10);

		var result = mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(order("tok_approved", item(keyboardId, 2), item(cableId, 1))))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.total").value(119.80))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.paymentReference", startsWith("fake-")))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].productName").value("Mechanical Keyboard"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(49.90))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].lineTotal").value(99.80))
				.andReturn();

		var location = result.getResponse().getHeader("Location");
		assertNotNull(location);
		mockMvc.perform(get(URI.create(location).getPath()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.items[1].sku").value("CAB-002"));

		assertEquals(1L, count("purchase_orders"));
		assertEquals(2L, count("purchase_order_items"));
		assertEquals(23, stock(keyboardId));
		assertEquals(9, stock(cableId));
	}

	@Test
	void rollsBackStockAndOrderWhenPaymentIsDeclined() throws Exception {
		var productId = createProduct("Mechanical Keyboard", "KEY-001", "49.90", 25);

		mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(order("tok_declined", item(productId, 2))))
				.andExpect(status().isPaymentRequired())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Payment declined"));

		assertEquals(0L, count("purchase_orders"));
		assertEquals(25, stock(productId));
	}

	@Test
	void rollsBackEveryLineWhenOneProductHasInsufficientStock() throws Exception {
		var availableId = createProduct("Mechanical Keyboard", "KEY-001", "49.90", 5);
		var unavailableId = createProduct("USB Cable", "CAB-002", "20.00", 1);

		mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(order("tok_approved", item(availableId, 2), item(unavailableId, 2))))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Insufficient stock"));

		assertEquals(0L, count("purchase_orders"));
		assertEquals(5, stock(availableId));
		assertEquals(1, stock(unavailableId));
	}

	@Test
	void rejectsDuplicateProductsBeforeChangingStock() throws Exception {
		var productId = createProduct("Mechanical Keyboard", "KEY-001", "49.90", 25);

		mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(order("tok_approved", item(productId, 1), item(productId, 2))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		assertEquals(0L, count("purchase_orders"));
		assertEquals(25, stock(productId));
	}

	private UUID createProduct(String name, String sku, String price, int stock) throws Exception {
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "%s",
							  "sku": "%s",
							  "description": "Product for checkout testing",
							  "category": "Accessories",
							  "price": %s,
							  "stock": %d,
							  "weightKg": 0.850
							}
							""".formatted(name, sku, price, stock)))
				.andExpect(status().isCreated());

		return jdbcTemplate.queryForObject("SELECT id FROM products WHERE sku = ?", UUID.class, sku);
	}

	private static String item(UUID productId, int quantity) {
		return """
				{"productId":"%s","quantity":%d}
				""".formatted(productId, quantity).trim();
	}

	private static String order(String paymentToken, String... items) {
		return """
				{
				  "items": [%s],
				  "paymentToken": "%s"
				}
				""".formatted(String.join(",", items), paymentToken);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}

	private int stock(UUID productId) {
		return jdbcTemplate.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, productId);
	}

}
