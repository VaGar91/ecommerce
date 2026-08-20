package com.gila.ecommerce.catalog.internal.domain;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

	private final UUID productId;
	private final String productName;
	private final String sku;
	private final int requested;
	private final int available;

	public InsufficientStockException(
			UUID productId,
			String productName,
			String sku,
			int requested,
			int available
	) {
		super("Product \"%s\" (%s) has %d %s available but %d were requested".formatted(
				productName,
				sku,
				available,
				available == 1 ? "unit" : "units",
				requested
		));
		this.productId = productId;
		this.productName = productName;
		this.sku = sku;
		this.requested = requested;
		this.available = available;
	}

	public UUID productId() {
		return productId;
	}

	public String productName() {
		return productName;
	}

	public String sku() {
		return sku;
	}

	public int requested() {
		return requested;
	}

	public int available() {
		return available;
	}

}
