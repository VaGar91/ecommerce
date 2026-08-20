package com.gila.ecommerce;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

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
class ProductControllerIntegrationTest {

	private static final String PRODUCT = """
			{
			  "name": "Mechanical Keyboard",
			  "sku": "key-001",
			  "description": "Tactile mechanical keyboard",
			  "category": "Accessories",
			  "price": 49.90,
			  "stock": 25,
			  "weightKg": 0.850
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM products");
	}

	@Test
	void performsTheCompleteProductLifecycle() throws Exception {
		var creation = mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(PRODUCT))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.sku").value("KEY-001"))
				.andReturn();

		var location = creation.getResponse().getHeader("Location");
		assertNotNull(location);
		var productPath = URI.create(location).getPath();

		mockMvc.perform(get(productPath))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
				.andExpect(jsonPath("$.stock").value(25));

		mockMvc.perform(put(productPath)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Ergonomic Keyboard",
							  "sku": "KEY-002",
							  "description": "Split mechanical keyboard",
							  "category": "Accessories",
							  "price": 79.99,
							  "stock": 12,
							  "weightKg": 1.100
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ergonomic Keyboard"))
				.andExpect(jsonPath("$.sku").value("KEY-002"));

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].sku").value("KEY-002"))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(delete(productPath))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(get(productPath))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Product not found"));
	}

	@Test
	void rejectsDuplicateSkus() throws Exception {
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(PRODUCT))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(PRODUCT.replace("key-001", "KEY-001")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("SKU already exists"));
	}

	@Test
	void reportsFieldValidationErrors() throws Exception {
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": " ",
							  "sku": "",
							  "description": "",
							  "category": "",
							  "price": -1,
							  "stock": -1,
							  "weightKg": 0
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.price").exists())
				.andExpect(jsonPath("$.errors.stock").exists())
				.andExpect(jsonPath("$.errors.weightKg").exists());
	}

	@Test
	void searchesFiltersAndPaginatesProducts() throws Exception {
		createProduct("Alpha Keyboard", "KEY-100", "Mechanical keyboard", "Accessories");
		createProduct("Beta Monitor", "MON-200", "4K display", "Displays");
		createProduct("Gamma Keyboard", "KEY-300", "Compact mechanical keyboard", "Accessories");
		createProduct("100% Cotton Cable", "CAB-400", "Braided charging cable", "Accessories");

		mockMvc.perform(get("/api/products")
					.param("query", "mechanical")
					.param("category", "accessories")
					.param("page", "0")
					.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Alpha Keyboard"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/products")
					.param("query", "mechanical")
					.param("category", "ACCESSORIES")
					.param("page", "1")
					.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Gamma Keyboard"));

		mockMvc.perform(get("/api/products")
					.param("query", "MON-200"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Beta Monitor"));

		mockMvc.perform(get("/api/products").param("query", "%"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].name").value("100% Cotton Cable"));
	}

	@Test
	void listsCurrentCategoriesWithoutCaseDuplicates() throws Exception {
		createProduct("Keyboard", "KEY-100", "Mechanical keyboard", "Accessories");
		createProduct("Mouse", "MOU-200", "Wireless mouse", "accessories");
		createProduct("Monitor", "MON-300", "4K display", "Displays");

		mockMvc.perform(get("/api/products/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0]").value("Accessories"))
				.andExpect(jsonPath("$[1]").value("Displays"));
	}

	@Test
	void rejectsInvalidPagination() throws Exception {
		mockMvc.perform(get("/api/products").param("page", "-1").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid request"));
	}

	private void createProduct(String name, String sku, String description, String category) throws Exception {
		var body = """
				{
				  "name": "%s",
				  "sku": "%s",
				  "description": "%s",
				  "category": "%s",
				  "price": 49.90,
				  "stock": 25,
				  "weightKg": 0.850
				}
				""".formatted(name, sku, description, category);

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isCreated());
	}

}
