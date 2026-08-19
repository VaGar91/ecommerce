package com.gila.ecommerce.productimport.internal.application;

public class ProductImportValidationException extends RuntimeException {

	private final ProductImportReport report;

	public ProductImportValidationException(ProductImportReport report) {
		super("The CSV contains invalid product rows");
		this.report = report;
	}

	public ProductImportReport report() {
		return report;
	}

}
