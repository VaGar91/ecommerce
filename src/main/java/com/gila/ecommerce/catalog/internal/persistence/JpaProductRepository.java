package com.gila.ecommerce.catalog.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.internal.domain.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

	boolean existsBySku(String sku);

	Optional<Product> findBySku(String sku);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT product FROM Product product WHERE product.id IN :productIds ORDER BY product.id")
	List<Product> findAllByIdForUpdate(@Param("productIds") List<UUID> productIds);

	boolean existsBySkuAndIdNot(String sku, UUID productId);

}
