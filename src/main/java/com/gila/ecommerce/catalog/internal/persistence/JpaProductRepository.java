package com.gila.ecommerce.catalog.internal.persistence;

import java.util.List;
import java.util.UUID;

import com.gila.ecommerce.catalog.internal.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaProductRepository extends JpaRepository<Product, UUID> {

	List<Product> findAllByOrderByNameAscSkuAsc();

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, UUID productId);

}
