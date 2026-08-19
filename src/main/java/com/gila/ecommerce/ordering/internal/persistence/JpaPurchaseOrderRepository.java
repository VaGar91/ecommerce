package com.gila.ecommerce.ordering.internal.persistence;

import java.util.UUID;

import com.gila.ecommerce.ordering.internal.domain.PurchaseOrder;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
}
