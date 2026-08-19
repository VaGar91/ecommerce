package com.gila.ecommerce.catalog;

import java.util.List;
import java.util.UUID;

public interface CatalogOperations {

	ProductSnapshot create(ProductDraft product);

	ProductSnapshot update(UUID productId, ProductDraft product);

	ProductSnapshot get(UUID productId);

	ProductPage search(ProductSearchQuery query);

	List<ProductUpsertResult> upsertAll(List<ProductDraft> products);

	void delete(UUID productId);

}
