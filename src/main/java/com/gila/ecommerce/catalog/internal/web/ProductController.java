package com.gila.ecommerce.catalog.internal.web;

import java.util.List;
import java.util.UUID;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.catalog.ProductPage;
import com.gila.ecommerce.catalog.ProductSearchQuery;
import com.gila.ecommerce.catalog.ProductSnapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
class ProductController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

	private final CatalogOperations catalog;

	ProductController(CatalogOperations catalog) {
		this.catalog = catalog;
	}

	@PostMapping
	ResponseEntity<ProductSnapshot> create(@Valid @RequestBody ProductRequest request) {
		var created = catalog.create(request.toDraft());
		LOGGER.info("event=product_created product_id={}", created.id());
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{productId}")
				.build(created.id());

		return ResponseEntity.created(location).body(created);
	}

	@GetMapping("/{productId}")
	ProductSnapshot get(@PathVariable UUID productId) {
		return catalog.get(productId);
	}

	@GetMapping("/categories")
	List<String> categories() {
		return catalog.categories();
	}

	@GetMapping
	ProductPage search(
			@RequestParam(defaultValue = "") @Size(max = 200) String query,
			@RequestParam(defaultValue = "") @Size(max = 100) String category,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return catalog.search(new ProductSearchQuery(query, category, page, size));
	}

	@PutMapping("/{productId}")
	ProductSnapshot update(@PathVariable UUID productId, @Valid @RequestBody ProductRequest request) {
		var updated = catalog.update(productId, request.toDraft());
		LOGGER.info("event=product_updated product_id={}", updated.id());
		return updated;
	}

	@DeleteMapping("/{productId}")
	ResponseEntity<Void> delete(@PathVariable UUID productId) {
		catalog.delete(productId);
		LOGGER.info("event=product_deleted product_id={}", productId);
		return ResponseEntity.noContent().build();
	}

}
