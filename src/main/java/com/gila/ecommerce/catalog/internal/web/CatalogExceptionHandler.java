package com.gila.ecommerce.catalog.internal.web;

import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.InsufficientStockException;
import com.gila.ecommerce.catalog.internal.domain.ProductNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CatalogExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(CatalogExceptionHandler.class);

	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail handleNotFound(ProductNotFoundException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Product not found");
		return problem;
	}

	@ExceptionHandler(DuplicateSkuException.class)
	ProblemDetail handleDuplicateSku(DuplicateSkuException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("SKU already exists");
		return problem;
	}

	@ExceptionHandler(InsufficientStockException.class)
	ProblemDetail handleInsufficientStock(InsufficientStockException exception) {
		LOGGER.warn(
				"event=order_stock_rejected product_id={} requested={} available={}",
				exception.productId(),
				exception.requested(),
				exception.available()
		);
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Insufficient stock");
		problem.setProperty("productId", exception.productId());
		problem.setProperty("productName", exception.productName());
		problem.setProperty("sku", exception.sku());
		problem.setProperty("requested", exception.requested());
		problem.setProperty("available", exception.available());
		return problem;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleInvalidProduct(IllegalArgumentException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid request");
		return problem;
	}

}
