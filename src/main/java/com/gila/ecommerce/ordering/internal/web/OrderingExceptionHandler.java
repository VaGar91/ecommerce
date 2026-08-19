package com.gila.ecommerce.ordering.internal.web;

import com.gila.ecommerce.ordering.internal.domain.DuplicateOrderItemException;
import com.gila.ecommerce.ordering.internal.domain.OrderNotFoundException;
import com.gila.ecommerce.ordering.internal.domain.PaymentDeclinedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class OrderingExceptionHandler {

	@ExceptionHandler(OrderNotFoundException.class)
	ProblemDetail handleNotFound(OrderNotFoundException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Order not found");
		return problem;
	}

	@ExceptionHandler(DuplicateOrderItemException.class)
	ProblemDetail handleDuplicateItem(DuplicateOrderItemException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(PaymentDeclinedException.class)
	ProblemDetail handlePaymentDeclined(PaymentDeclinedException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, exception.getMessage());
		problem.setTitle("Payment declined");
		return problem;
	}

}
