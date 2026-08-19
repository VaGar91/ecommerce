package com.gila.ecommerce.catalog.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

	Product save(Product product);

	Optional<Product> findById(UUID productId);

	List<Product> findAll();

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, UUID productId);

	void delete(Product product);

}
