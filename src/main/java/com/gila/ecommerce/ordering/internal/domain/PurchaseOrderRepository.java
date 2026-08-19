package com.gila.ecommerce.ordering.internal.domain;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository {

	PurchaseOrder save(PurchaseOrder order);

	Optional<PurchaseOrder> findById(UUID orderId);

}
