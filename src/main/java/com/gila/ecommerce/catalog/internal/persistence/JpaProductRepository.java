package com.gila.ecommerce.catalog.internal.persistence;

import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.internal.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface JpaProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

	boolean existsBySku(String sku);

	Optional<Product> findBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, UUID productId);

}
