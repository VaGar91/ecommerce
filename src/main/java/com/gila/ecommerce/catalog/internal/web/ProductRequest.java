package com.gila.ecommerce.catalog.internal.web;

import java.math.BigDecimal;

import com.gila.ecommerce.catalog.ProductDraft;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record ProductRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 64) String sku,
		@NotBlank @Size(max = 2_000) String description,
		@NotBlank @Size(max = 100) String category,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@NotNull @PositiveOrZero Integer stock,
		@NotNull @DecimalMin(value = "0.000", inclusive = false) @Digits(integer = 7, fraction = 3) BigDecimal weightKg
) {

	ProductDraft toDraft() {
		return new ProductDraft(name, sku, description, category, price, stock, weightKg);
	}

}
