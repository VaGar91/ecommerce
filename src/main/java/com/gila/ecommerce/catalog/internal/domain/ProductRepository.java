package com.gila.ecommerce.catalog.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.ProductSearchQuery;

public interface ProductRepository {

	Product save(Product product);

	Optional<Product> findById(UUID productId);

	Optional<Product> findBySku(String sku);

	List<Product> findAllByIdForUpdate(List<UUID> productIds);

	ProductSearchResult search(ProductSearchQuery query);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, UUID productId);

	void delete(Product product);

}
