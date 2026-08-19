package com.gila.ecommerce.catalog.internal.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.catalog.ProductSearchQuery;
import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.Product;
import com.gila.ecommerce.catalog.internal.domain.ProductRepository;
import com.gila.ecommerce.catalog.internal.domain.ProductSearchResult;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
	public Optional<Product> findBySku(String sku) {
		return products.findBySku(sku);
	}

	@Override
	public List<Product> findAllByIdForUpdate(List<UUID> productIds) {
		return products.findAllByIdForUpdate(productIds);
	}

	@Override
	public ProductSearchResult search(ProductSearchQuery query) {
		var sort = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("sku"));
		var page = products.findAll(searchSpecification(query), PageRequest.of(query.page(), query.size(), sort));
		return new ProductSearchResult(page.getContent(), page.getTotalElements(), page.getTotalPages());
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

	private static Specification<Product> searchSpecification(ProductSearchQuery search) {
		return (root, query, builder) -> {
			var predicates = new ArrayList<Predicate>();

			if (!search.query().isEmpty()) {
				var pattern = literalContainsPattern(search.query());
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("name")), pattern, '\\'),
						builder.like(builder.lower(root.get("sku")), pattern, '\\'),
						builder.like(builder.lower(root.get("description")), pattern, '\\'),
						builder.like(builder.lower(root.get("category")), pattern, '\\')
				));
			}

			if (!search.category().isEmpty()) {
				predicates.add(builder.equal(
						builder.lower(root.get("category")),
						search.category().toLowerCase(Locale.ROOT)
				));
			}

			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String literalContainsPattern(String value) {
		var escaped = value.toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
