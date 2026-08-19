package com.gila.ecommerce.catalog.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.Product;
import com.gila.ecommerce.catalog.internal.domain.ProductRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class ProductPersistenceAdapter implements ProductRepository {

	private final JpaProductRepository products;

	ProductPersistenceAdapter(JpaProductRepository products) {
		this.products = products;
	}

	@Override
	public Product save(Product product) {
		try {
			return products.saveAndFlush(product);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateSkuException(product.sku());
		}
	}

	@Override
	public Optional<Product> findById(UUID productId) {
		return products.findById(productId);
	}

	@Override
	public List<Product> findAll() {
		return products.findAllByOrderByNameAscSkuAsc();
	}

	@Override
	public boolean existsBySku(String sku) {
		return products.existsBySku(sku);
	}

	@Override
	public boolean existsBySkuAndIdNot(String sku, UUID productId) {
		return products.existsBySkuAndIdNot(sku, productId);
	}

	@Override
	public void delete(Product product) {
		products.delete(product);
	}

}
