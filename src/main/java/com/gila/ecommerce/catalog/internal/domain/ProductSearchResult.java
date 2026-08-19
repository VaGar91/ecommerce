package com.gila.ecommerce.catalog.internal.domain;

import java.util.List;

public record ProductSearchResult(
		List<Product> content,
		long totalElements,
		int totalPages
) {

	public ProductSearchResult {
		content = List.copyOf(content);
	}

}
