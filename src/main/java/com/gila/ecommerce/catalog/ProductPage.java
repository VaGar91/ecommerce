package com.gila.ecommerce.catalog;

import java.util.List;

public record ProductPage(
		List<ProductSnapshot> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public ProductPage {
		content = List.copyOf(content);
	}

}
