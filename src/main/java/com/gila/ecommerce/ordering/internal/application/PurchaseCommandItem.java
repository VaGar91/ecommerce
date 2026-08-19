package com.gila.ecommerce.ordering.internal.application;

import java.util.UUID;

public record PurchaseCommandItem(UUID productId, int quantity) {
}
