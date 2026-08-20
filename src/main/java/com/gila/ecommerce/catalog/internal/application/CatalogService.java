package com.gila.ecommerce.catalog.internal.application;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.catalog.ProductDraft;
import com.gila.ecommerce.catalog.ProductPage;
import com.gila.ecommerce.catalog.ProductSearchQuery;
import com.gila.ecommerce.catalog.ProductSnapshot;
import com.gila.ecommerce.catalog.ProductUpsertResult;
import com.gila.ecommerce.catalog.StockRequest;
import com.gila.ecommerce.catalog.StockReservation;
import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.Product;
import com.gila.ecommerce.catalog.internal.domain.ProductNotFoundException;
import com.gila.ecommerce.catalog.internal.domain.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService implements CatalogOperations {

	private final ProductRepository products;

	public CatalogService(ProductRepository products) {
		this.products = products;
	}

	@Override
	@Transactional
	public ProductSnapshot create(ProductDraft draft) {
		var sku = Product.normalizeSku(draft.sku());
		if (products.existsBySku(sku)) {
			throw new DuplicateSkuException(sku);
		}

		return snapshot(products.save(Product.create(draft)));
	}

	@Override
	@Transactional
	public ProductSnapshot update(UUID productId, ProductDraft draft) {
		var product = findProduct(productId);
		var sku = Product.normalizeSku(draft.sku());

		if (products.existsBySkuAndIdNot(sku, productId)) {
			throw new DuplicateSkuException(sku);
		}

		product.update(draft);
		return snapshot(products.save(product));
	}

	@Override
	@Transactional(readOnly = true)
	public ProductSnapshot get(UUID productId) {
		return snapshot(findProduct(productId));
	}

	@Override
	@Transactional(readOnly = true)
	public ProductPage search(ProductSearchQuery query) {
		var result = products.search(query);
		var content = result.content().stream().map(CatalogService::snapshot).toList();

		return new ProductPage(content, query.page(), query.size(), result.totalElements(), result.totalPages());
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> categories() {
		return products.findCategories();
	}

	@Override
	@Transactional
	public List<ProductUpsertResult> upsertAll(List<ProductDraft> drafts) {
		return drafts.stream().map(this::upsert).toList();
	}

	@Override
	@Transactional
	public List<StockReservation> reserveStock(List<StockRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			throw new IllegalArgumentException("at least one stock request is required");
		}

		var productIds = requests.stream().map(StockRequest::productId).toList();
		if (new HashSet<>(productIds).size() != productIds.size()) {
			throw new IllegalArgumentException("each product may only appear once");
		}

		var sortedIds = productIds.stream().sorted(Comparator.naturalOrder()).toList();
		var lockedProducts = products.findAllByIdForUpdate(sortedIds).stream()
				.collect(Collectors.toMap(Product::id, Function.identity()));

		return requests.stream().map(request -> reserve(request, lockedProducts)).toList();
	}

	@Override
	@Transactional
	public void delete(UUID productId) {
		products.delete(findProduct(productId));
	}

	private Product findProduct(UUID productId) {
		return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
	}

	private ProductUpsertResult upsert(ProductDraft draft) {
		var sku = Product.normalizeSku(draft.sku());
		var existing = products.findBySku(sku);

		if (existing.isPresent()) {
			var product = existing.orElseThrow();
			product.update(draft);
			return new ProductUpsertResult(snapshot(products.save(product)), false);
		}

		return new ProductUpsertResult(snapshot(products.save(Product.create(draft))), true);
	}

	private static StockReservation reserve(StockRequest request, Map<UUID, Product> products) {
		var product = products.get(request.productId());
		if (product == null) {
			throw new ProductNotFoundException(request.productId());
		}

		product.reserveStock(request.quantity());
		return new StockReservation(
				product.id(),
				product.name(),
				product.sku(),
				product.price(),
				request.quantity()
		);
	}

	private static ProductSnapshot snapshot(Product product) {
		return new ProductSnapshot(
				product.id(),
				product.name(),
				product.sku(),
				product.description(),
				product.category(),
				product.price(),
				product.stock(),
				product.weightKg()
		);
	}

}
