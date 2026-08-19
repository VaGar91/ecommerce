package com.gila.ecommerce.catalog;

import java.util.List;
import java.util.UUID;

public interface CatalogOperations {

	ProductSnapshot create(ProductDraft product);

	ProductSnapshot update(UUID productId, ProductDraft product);

	ProductSnapshot get(UUID productId);

	List<ProductSnapshot> findAll();

	void delete(UUID productId);

}
