package com.gila.ecommerce.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductDraft(
		@NotBlank(message = "is required")
		@Size(max = 200, message = "must not exceed {max} characters")
		@Pattern(regexp = "^[^<>]*$", message = "must not contain HTML markup")
		String name,
		@NotBlank(message = "is required")
		@Size(max = 64, message = "must not exceed {max} characters")
		@Pattern(regexp = "^[^<>]*$", message = "must not contain HTML markup")
		String sku,
		@NotBlank(message = "is required")
		@Size(max = 2_000, message = "must not exceed {max} characters")
		@Pattern(regexp = "^[^<>]*$", message = "must not contain HTML markup")
		String description,
		@NotBlank(message = "is required")
		@Size(max = 100, message = "must not exceed {max} characters")
		@Pattern(regexp = "^[^<>]*$", message = "must not contain HTML markup")
		String category,
		@NotNull(message = "is required")
		@DecimalMin(value = "0.00", message = "must not be negative")
		@Digits(integer = 10, fraction = 2, message = "must have at most 10 integer and 2 decimal digits")
		BigDecimal price,
		@PositiveOrZero(message = "must not be negative") int stock,
		@NotNull(message = "is required")
		@DecimalMin(value = "0.000", inclusive = false, message = "must be greater than zero")
		@Digits(integer = 7, fraction = 3, message = "must have at most 7 integer and 3 decimal digits")
		BigDecimal weightKg
) {
}
