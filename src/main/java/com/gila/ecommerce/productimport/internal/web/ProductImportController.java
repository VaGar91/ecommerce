package com.gila.ecommerce.productimport.internal.web;

import java.io.IOException;

import com.gila.ecommerce.productimport.internal.application.ProductImportReport;
import com.gila.ecommerce.productimport.internal.application.ProductImportService;
import com.gila.ecommerce.productimport.internal.csv.InvalidCsvFileException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/product-imports")
class ProductImportController {

	private final ProductImportService imports;

	ProductImportController(ProductImportService imports) {
		this.imports = imports;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ProductImportReport importProducts(@RequestPart("file") MultipartFile file) {
		if (file.isEmpty()) {
			throw new InvalidCsvFileException("The uploaded CSV file is empty");
		}

		try {
			return imports.importCsv(file.getInputStream());
		} catch (IOException exception) {
			throw new InvalidCsvFileException("The uploaded CSV file could not be read", exception);
		}
	}

}
