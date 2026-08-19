package com.gila.ecommerce.ordering.internal.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

import com.gila.ecommerce.catalog.CatalogOperations;
import com.gila.ecommerce.catalog.StockRequest;
import com.gila.ecommerce.ordering.internal.domain.DuplicateOrderItemException;
import com.gila.ecommerce.ordering.internal.domain.OrderNotFoundException;
import com.gila.ecommerce.ordering.internal.domain.PaymentDeclinedException;
import com.gila.ecommerce.ordering.internal.domain.PurchaseOrder;
import com.gila.ecommerce.ordering.internal.domain.PurchaseOrderItem;
import com.gila.ecommerce.ordering.internal.domain.PurchaseOrderRepository;
import com.gila.ecommerce.payment.PaymentGateway;
import com.gila.ecommerce.payment.PaymentRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderingService {

	private final CatalogOperations catalog;
	private final PaymentGateway payments;
	private final PurchaseOrderRepository orders;

	public OrderingService(
			CatalogOperations catalog,
			PaymentGateway payments,
			PurchaseOrderRepository orders
	) {
		this.catalog = catalog;
		this.payments = payments;
		this.orders = orders;
	}

	@Transactional
	public OrderSnapshot purchase(PurchaseCommand command) {
		validate(command);
		var stockRequests = command.items().stream()
				.map(item -> new StockRequest(item.productId(), item.quantity()))
				.toList();
		var reservations = catalog.reserveStock(stockRequests);
		var order = PurchaseOrder.create(reservations, Instant.now());

		var payment = payments.charge(new PaymentRequest(
				order.id(),
				order.total(),
				order.currency(),
				command.paymentToken()
		));
		if (!payment.approved()) {
			throw new PaymentDeclinedException(payment.failureReason());
		}

		order.markPaid(payment.paymentReference());
		return snapshot(orders.save(order));
	}

	@Transactional(readOnly = true)
	public OrderSnapshot get(UUID orderId) {
		return snapshot(orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId)));
	}

	private static void validate(PurchaseCommand command) {
		if (command == null || command.items().isEmpty()) {
			throw new IllegalArgumentException("an order must contain at least one item");
		}

		var productIds = command.items().stream().map(PurchaseCommandItem::productId).toList();
		if (new HashSet<>(productIds).size() != productIds.size()) {
			throw new DuplicateOrderItemException();
		}
	}

	private static OrderSnapshot snapshot(PurchaseOrder order) {
		return new OrderSnapshot(
				order.id(),
				order.status(),
				order.items().stream().map(OrderingService::snapshot).toList(),
				order.total(),
				order.currency(),
				order.paymentReference(),
				order.createdAt()
		);
	}

	private static OrderItemSnapshot snapshot(PurchaseOrderItem item) {
		return new OrderItemSnapshot(
				item.productId(),
				item.productName(),
				item.sku(),
				item.unitPrice(),
				item.quantity(),
				item.lineTotal()
		);
	}

}
