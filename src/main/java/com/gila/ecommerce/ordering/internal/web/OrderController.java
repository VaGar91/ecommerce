package com.gila.ecommerce.ordering.internal.web;

import java.net.URI;
import java.util.UUID;

import com.gila.ecommerce.ordering.internal.application.OrderSnapshot;
import com.gila.ecommerce.ordering.internal.application.OrderingService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

	private final OrderingService ordering;

	OrderController(OrderingService ordering) {
		this.ordering = ordering;
	}

	@PostMapping
	ResponseEntity<OrderSnapshot> purchase(@Valid @RequestBody PurchaseRequest request) {
		var order = ordering.purchase(request.toCommand());
		LOGGER.info(
				"event=order_paid order_id={} item_count={} total={} currency={}",
				order.id(),
				order.items().size(),
				order.total(),
				order.currency()
		);
		return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
	}

	@GetMapping("/{orderId}")
	OrderSnapshot get(@PathVariable UUID orderId) {
		return ordering.get(orderId);
	}

}
