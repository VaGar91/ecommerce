package com.gila.ecommerce.ordering.internal.persistence;

import java.util.Optional;
import java.util.UUID;

import com.gila.ecommerce.ordering.internal.domain.PurchaseOrder;
import com.gila.ecommerce.ordering.internal.domain.PurchaseOrderRepository;

import org.springframework.stereotype.Repository;

@Repository
class PurchaseOrderPersistenceAdapter implements PurchaseOrderRepository {

	private final JpaPurchaseOrderRepository orders;

	PurchaseOrderPersistenceAdapter(JpaPurchaseOrderRepository orders) {
		this.orders = orders;
	}

	@Override
	public PurchaseOrder save(PurchaseOrder order) {
		return orders.saveAndFlush(order);
	}

	@Override
	public Optional<PurchaseOrder> findById(UUID orderId) {
		return orders.findById(orderId);
	}

}
