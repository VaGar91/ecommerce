package com.gila.ecommerce.catalog;

public record ProductSearchQuery(
		String query,
		String category,
		int page,
		int size
) {

	public ProductSearchQuery {
		query = normalize(query);
		category = normalize(category);

		if (query.length() > 200) {
			throw new IllegalArgumentException("query must not exceed 200 characters");
		}
		if (category.length() > 100) {
			throw new IllegalArgumentException("category must not exceed 100 characters");
		}
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("size must be between 1 and 100");
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

}
