package com.gila.ecommerce.productimport.internal.web;

import com.gila.ecommerce.productimport.internal.csv.InvalidCsvFileException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProductImportExceptionHandler {

	@ExceptionHandler(InvalidCsvFileException.class)
	ProblemDetail handleInvalidFile(InvalidCsvFileException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid CSV file");
		return problem;
	}

}
