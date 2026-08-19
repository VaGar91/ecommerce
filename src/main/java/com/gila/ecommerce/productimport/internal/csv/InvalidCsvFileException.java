package com.gila.ecommerce.productimport.internal.csv;

public class InvalidCsvFileException extends RuntimeException {

	public InvalidCsvFileException(String message) {
		super(message);
	}

	public InvalidCsvFileException(String message, Throwable cause) {
		super(message, cause);
	}

}
