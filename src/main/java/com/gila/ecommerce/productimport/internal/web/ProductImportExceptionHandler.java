package com.gila.ecommerce.productimport.internal.web;

import com.gila.ecommerce.productimport.internal.csv.InvalidCsvFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProductImportExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductImportExceptionHandler.class);

	@ExceptionHandler(InvalidCsvFileException.class)
	ProblemDetail handleInvalidFile(InvalidCsvFileException exception) {
		LOGGER.warn("event=product_import_rejected reason=invalid_csv");
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid CSV file");
		return problem;
	}

}
