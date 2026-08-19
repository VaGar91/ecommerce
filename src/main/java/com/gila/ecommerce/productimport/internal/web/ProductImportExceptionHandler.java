package com.gila.ecommerce.productimport.internal.web;

import com.gila.ecommerce.productimport.internal.application.ProductImportReport;
import com.gila.ecommerce.productimport.internal.application.ProductImportValidationException;
import com.gila.ecommerce.productimport.internal.csv.InvalidCsvFileException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProductImportExceptionHandler {

	@ExceptionHandler(ProductImportValidationException.class)
	ResponseEntity<ProductImportReport> handleValidation(ProductImportValidationException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(exception.report());
	}

	@ExceptionHandler(InvalidCsvFileException.class)
	ProblemDetail handleInvalidFile(InvalidCsvFileException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid CSV file");
		return problem;
	}

}
