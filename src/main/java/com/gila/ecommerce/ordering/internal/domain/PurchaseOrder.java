package com.gila.ecommerce.ordering.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.gila.ecommerce.catalog.StockReservation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal total;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "payment_reference", length = 128)
	private String paymentReference;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("lineNumber ASC")
	private List<PurchaseOrderItem> items = new ArrayList<>();

	@Version
	private long version;

	protected PurchaseOrder() {
	}

	private PurchaseOrder(UUID id, List<StockReservation> reservations, Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.status = OrderStatus.PENDING;
		this.currency = "USD";
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.items = createItems(reservations);
		this.total = items.stream()
				.map(PurchaseOrderItem::lineTotal)
				.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
	}

	public static PurchaseOrder create(List<StockReservation> reservations, Instant createdAt) {
		if (reservations == null || reservations.isEmpty()) {
			throw new IllegalArgumentException("an order must contain at least one item");
		}
		return new PurchaseOrder(UUID.randomUUID(), reservations, createdAt);
	}

	public void markPaid(String reference) {
		if (status != OrderStatus.PENDING) {
			throw new IllegalStateException("only pending orders can be paid");
		}
		if (reference == null || reference.isBlank()) {
			throw new IllegalArgumentException("payment reference must not be blank");
		}
		this.paymentReference = reference.trim();
		this.status = OrderStatus.PAID;
	}

	private List<PurchaseOrderItem> createItems(List<StockReservation> reservations) {
		var createdItems = new ArrayList<PurchaseOrderItem>(reservations.size());
		for (var lineNumber = 0; lineNumber < reservations.size(); lineNumber++) {
			createdItems.add(PurchaseOrderItem.create(this, lineNumber, reservations.get(lineNumber)));
		}
		return createdItems;
	}

	public UUID id() {
		return id;
	}

	public OrderStatus status() {
		return status;
	}

	public BigDecimal total() {
		return total;
	}

	public String currency() {
		return currency;
	}

	public String paymentReference() {
		return paymentReference;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public List<PurchaseOrderItem> items() {
		return Collections.unmodifiableList(items);
	}

}
