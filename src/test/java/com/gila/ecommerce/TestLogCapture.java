package com.gila.ecommerce;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

final class TestLogCapture implements AutoCloseable {

	private final Logger logger;
	private final ListAppender<ILoggingEvent> appender;

	private TestLogCapture(String loggerName) {
		this.logger = (Logger) LoggerFactory.getLogger(loggerName);
		this.appender = new ListAppender<>();
		this.appender.start();
		this.logger.addAppender(appender);
	}

	static TestLogCapture forLogger(String loggerName) {
		return new TestLogCapture(loggerName);
	}

	boolean containsMessage(String expected) {
		return appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(expected));
	}

	boolean containsRequestId(String requestId) {
		return appender.list.stream()
				.anyMatch(event -> requestId.equals(event.getMDCPropertyMap().get("requestId")));
	}

	@Override
	public void close() {
		logger.detachAppender(appender);
		appender.stop();
	}

}
