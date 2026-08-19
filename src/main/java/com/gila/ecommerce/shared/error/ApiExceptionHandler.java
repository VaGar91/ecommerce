package com.gila.ecommerce.shared.error;

import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidArguments(MethodArgumentNotValidException exception) {
		var errors = new LinkedHashMap<String, String>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
		problem.setTitle("Invalid request");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableMessage() {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is missing or malformed");
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleInvalidParameters() {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid");
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ProblemDetail handleFileTooLarge() {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "The uploaded file exceeds the 5 MB limit");
		problem.setTitle("File too large");
		return problem;
	}

}
