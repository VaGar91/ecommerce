package com.gila.ecommerce.ordering.internal.domain;

public class DuplicateOrderItemException extends RuntimeException {

	public DuplicateOrderItemException() {
		super("Each product may only appear once in an order");
	}

}
