package com.gila.ecommerce.shared.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Request-ID";
	public static final String MDC_KEY = "requestId";

	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		var requestId = requestId(request.getHeader(HEADER_NAME));
		var previousRequestId = MDC.get(MDC_KEY);
		response.setHeader(HEADER_NAME, requestId);
		MDC.put(MDC_KEY, requestId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			restoreMdc(previousRequestId);
		}
	}

	private static String requestId(String candidate) {
		return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
				? candidate
				: UUID.randomUUID().toString();
	}

	private static void restoreMdc(String previousRequestId) {
		if (previousRequestId == null) {
			MDC.remove(MDC_KEY);
		} else {
			MDC.put(MDC_KEY, previousRequestId);
		}
	}

}
