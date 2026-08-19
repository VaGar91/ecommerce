package com.gila.ecommerce.ordering.internal.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.gila.ecommerce.catalog.StockReservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private PurchaseOrder order;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Column(nullable = false, length = 64)
	private String sku;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total", nullable = false, precision = 18, scale = 2)
	private BigDecimal lineTotal;

	protected PurchaseOrderItem() {
	}

	private PurchaseOrderItem(PurchaseOrder order, int lineNumber, StockReservation reservation) {
		this.id = UUID.randomUUID();
		this.order = Objects.requireNonNull(order, "order must not be null");
		this.lineNumber = lineNumber;
		this.productId = reservation.productId();
		this.productName = reservation.productName();
		this.sku = reservation.sku();
		this.unitPrice = reservation.unitPrice();
		this.quantity = reservation.quantity();
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	static PurchaseOrderItem create(PurchaseOrder order, int lineNumber, StockReservation reservation) {
		return new PurchaseOrderItem(order, lineNumber, reservation);
	}

	public UUID productId() {
		return productId;
	}

	public String productName() {
		return productName;
	}

	public String sku() {
		return sku;
	}

	public BigDecimal unitPrice() {
		return unitPrice;
	}

	public int quantity() {
		return quantity;
	}

	public BigDecimal lineTotal() {
		return lineTotal;
	}

}
