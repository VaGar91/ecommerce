package com.gila.ecommerce.catalog.internal.web;

import java.util.List;
import java.util.UUID;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.catalog.ProductSnapshot;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
class ProductController {

	private final CatalogOperations catalog;

	ProductController(CatalogOperations catalog) {
		this.catalog = catalog;
	}

	@PostMapping
	ResponseEntity<ProductSnapshot> create(@Valid @RequestBody ProductRequest request) {
		var created = catalog.create(request.toDraft());
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{productId}")
				.build(created.id());

		return ResponseEntity.created(location).body(created);
	}

	@GetMapping("/{productId}")
	ProductSnapshot get(@PathVariable UUID productId) {
		return catalog.get(productId);
	}

	@GetMapping
	List<ProductSnapshot> findAll() {
		return catalog.findAll();
	}

	@PutMapping("/{productId}")
	ProductSnapshot update(@PathVariable UUID productId, @Valid @RequestBody ProductRequest request) {
		return catalog.update(productId, request.toDraft());
	}

	@DeleteMapping("/{productId}")
	ResponseEntity<Void> delete(@PathVariable UUID productId) {
		catalog.delete(productId);
		return ResponseEntity.noContent().build();
	}

}
