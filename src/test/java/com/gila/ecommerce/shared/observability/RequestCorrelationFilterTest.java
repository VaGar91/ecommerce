package com.gila.ecommerce.shared.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

	private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void propagatesASafeRequestIdAndClearsItAfterTheRequest() throws Exception {
		var request = new MockHttpServletRequest();
		request.addHeader(RequestCorrelationFilter.HEADER_NAME, "review-request-123");
		var response = new MockHttpServletResponse();
		var requestIdDuringChain = new AtomicReference<String>();

		filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
				requestIdDuringChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

		assertEquals("review-request-123", response.getHeader(RequestCorrelationFilter.HEADER_NAME));
		assertEquals("review-request-123", requestIdDuringChain.get());
		assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
	}

	@Test
	void replacesAnUnsafeRequestIdAndRestoresPriorMdcState() throws Exception {
		MDC.put(RequestCorrelationFilter.MDC_KEY, "upstream-request");
		var request = new MockHttpServletRequest();
		request.addHeader(RequestCorrelationFilter.HEADER_NAME, "<unsafe-request-id>");
		var response = new MockHttpServletResponse();
		var requestIdDuringChain = new AtomicReference<String>();

		filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
				requestIdDuringChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

		var generatedRequestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);
		assertNotEquals("<unsafe-request-id>", generatedRequestId);
		assertTrue(generatedRequestId.matches("[0-9a-f-]{36}"));
		assertEquals(generatedRequestId, requestIdDuringChain.get());
		assertEquals("upstream-request", MDC.get(RequestCorrelationFilter.MDC_KEY));
	}

}
