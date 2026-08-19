package com.gila.ecommerce.catalog;

import java.util.UUID;

public interface CatalogOperations {

	ProductSnapshot create(ProductDraft product);

	ProductSnapshot update(UUID productId, ProductDraft product);

	ProductSnapshot get(UUID productId);

	ProductPage search(ProductSearchQuery query);

	void delete(UUID productId);

}
