package com.gila.ecommerce.catalog.internal.domain;

public class DuplicateSkuException extends RuntimeException {

	public DuplicateSkuException(String sku) {
		super("A product with SKU '%s' already exists".formatted(sku));
	}

}
