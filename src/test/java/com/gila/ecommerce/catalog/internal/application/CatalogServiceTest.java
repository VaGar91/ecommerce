package com.gila.ecommerce.catalog.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.ProductDraft;
import com.gila.ecommerce.catalog.ProductSearchQuery;
import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.Product;
import com.gila.ecommerce.catalog.internal.domain.ProductNotFoundException;
import com.gila.ecommerce.catalog.internal.domain.ProductRepository;
import com.gila.ecommerce.catalog.internal.domain.ProductSearchResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {

	private CatalogService catalog;

	@BeforeEach
	void setUp() {
		catalog = new CatalogService(new InMemoryProductRepository());
	}

	@Test
	void createsAndNormalizesAProduct() {
		var created = catalog.create(product("  Mechanical Keyboard  ", " key-001 "));

		assertEquals("Mechanical Keyboard", created.name());
		assertEquals("KEY-001", created.sku());
		assertEquals(new BigDecimal("49.90"), created.price());
		assertEquals(new BigDecimal("0.850"), created.weightKg());
	}

	@Test
	void rejectsDuplicateSkusRegardlessOfCase() {
		catalog.create(product("Keyboard", "KEY-001"));

		assertThrows(DuplicateSkuException.class, () -> catalog.create(product("Another keyboard", "key-001")));
	}

	@Test
	void updatesAndListsProducts() {
		var created = catalog.create(product("Keyboard", "KEY-001"));
		var updatedDraft = new ProductDraft(
				"Ergonomic Keyboard",
				"KEY-002",
				"Split mechanical keyboard",
				"Accessories",
				new BigDecimal("79.99"),
				12,
				new BigDecimal("1.100")
		);

		var updated = catalog.update(created.id(), updatedDraft);

		assertEquals("Ergonomic Keyboard", updated.name());
		assertEquals("KEY-002", updated.sku());
		assertEquals(List.of(updated), catalog.search(new ProductSearchQuery("", "", 0, 20)).content());
	}

	@Test
	void deletesAProductAndRejectsSubsequentReads() {
		var created = catalog.create(product("Keyboard", "KEY-001"));

		catalog.delete(created.id());

		assertThrows(ProductNotFoundException.class, () -> catalog.get(created.id()));
	}

	private static ProductDraft product(String name, String sku) {
		return new ProductDraft(
				name,
				sku,
				"Tactile mechanical keyboard",
				"Accessories",
				new BigDecimal("49.9"),
				25,
				new BigDecimal("0.85")
		);
	}

	private static final class InMemoryProductRepository implements ProductRepository {

		private final Map<UUID, Product> products = new HashMap<>();

		@Override
		public Product save(Product product) {
			products.put(product.id(), product);
			return product;
		}

		@Override
		public Optional<Product> findById(UUID productId) {
			return Optional.ofNullable(products.get(productId));
		}

		@Override
		public ProductSearchResult search(ProductSearchQuery query) {
			var matches = new ArrayList<>(products.values());
			matches.sort(Comparator.comparing(Product::name).thenComparing(Product::sku));

			var fromIndex = Math.min(query.page() * query.size(), matches.size());
			var toIndex = Math.min(fromIndex + query.size(), matches.size());
			var totalPages = (int) Math.ceil((double) matches.size() / query.size());

			return new ProductSearchResult(matches.subList(fromIndex, toIndex), matches.size(), totalPages);
		}

		@Override
		public boolean existsBySku(String sku) {
			return products.values().stream().anyMatch(product -> product.sku().equals(sku));
		}

		@Override
		public boolean existsBySkuAndIdNot(String sku, UUID productId) {
			return products.values().stream()
					.anyMatch(product -> !product.id().equals(productId) && product.sku().equals(sku));
		}

		@Override
		public void delete(Product product) {
			products.remove(product.id());
		}
	}

}
