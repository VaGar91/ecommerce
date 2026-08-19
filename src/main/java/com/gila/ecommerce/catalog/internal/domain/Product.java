package com.gila.ecommerce.catalog.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.gila.ecommerce.catalog.ProductDraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 64, unique = true)
	private String sku;

	@Column(nullable = false, length = 2_000)
	private String description;

	@Column(nullable = false, length = 100)
	private String category;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int stock;

	@Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
	private BigDecimal weightKg;

	@Version
	private long version;

	protected Product() {
	}

	private Product(UUID id, ProductDraft draft) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		apply(draft);
	}

	public static Product create(ProductDraft draft) {
		return new Product(UUID.randomUUID(), draft);
	}

	public void update(ProductDraft draft) {
		apply(draft);
	}

	public void reserveStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be greater than zero");
		}
		if (stock < quantity) {
			throw new InsufficientStockException(id, quantity, stock);
		}
		stock -= quantity;
	}

	private void apply(ProductDraft draft) {
		Objects.requireNonNull(draft, "product must not be null");
		this.name = requiredText(draft.name(), "name", 200);
		this.sku = normalizeSku(draft.sku());
		this.description = requiredText(draft.description(), "description", 2_000);
		this.category = requiredText(draft.category(), "category", 100);
		this.price = decimal(draft.price(), "price", 12, 2, false);
		this.stock = nonNegativeStock(draft.stock());
		this.weightKg = decimal(draft.weightKg(), "weightKg", 10, 3, true);
	}

	public static String normalizeSku(String sku) {
		return requiredText(sku, "sku", 64).toUpperCase(Locale.ROOT);
	}

	private static String requiredText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}

		var normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private static BigDecimal decimal(
			BigDecimal value,
			String field,
			int precision,
			int scale,
			boolean strictlyPositive
	) {
		Objects.requireNonNull(value, field + " must not be null");

		final BigDecimal normalized;
		try {
			normalized = value.setScale(scale, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(field + " must have at most " + scale + " decimal places", exception);
		}

		if (normalized.precision() > precision) {
			throw new IllegalArgumentException(field + " exceeds the supported precision");
		}
		if (strictlyPositive ? normalized.signum() <= 0 : normalized.signum() < 0) {
			throw new IllegalArgumentException(field + (strictlyPositive ? " must be greater than zero" : " must not be negative"));
		}
		return normalized;
	}

	private static int nonNegativeStock(int stock) {
		if (stock < 0) {
			throw new IllegalArgumentException("stock must not be negative");
		}
		return stock;
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String sku() {
		return sku;
	}

	public String description() {
		return description;
	}

	public String category() {
		return category;
	}

	public BigDecimal price() {
		return price;
	}

	public int stock() {
		return stock;
	}

	public BigDecimal weightKg() {
		return weightKg;
	}

}
