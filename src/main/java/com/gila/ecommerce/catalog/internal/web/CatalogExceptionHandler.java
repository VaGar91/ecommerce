package com.gila.ecommerce.catalog.internal.web;

import com.gila.ecommerce.catalog.internal.domain.DuplicateSkuException;
import com.gila.ecommerce.catalog.internal.domain.ProductNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CatalogExceptionHandler {

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

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleInvalidProduct(IllegalArgumentException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid request");
		return problem;
	}

}
