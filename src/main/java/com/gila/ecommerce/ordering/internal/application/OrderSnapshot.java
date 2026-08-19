package com.gila.ecommerce.ordering.internal.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.gila.ecommerce.ordering.internal.domain.OrderStatus;

public record OrderSnapshot(
		UUID id,
		OrderStatus status,
		List<OrderItemSnapshot> items,
		BigDecimal total,
		String currency,
		String paymentReference,
		Instant createdAt
) {

	public OrderSnapshot {
		items = List.copyOf(items);
	}

}
