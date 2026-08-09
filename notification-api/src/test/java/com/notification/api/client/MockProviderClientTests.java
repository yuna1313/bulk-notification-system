package com.notification.api.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.notification.api.domain.FailureReason;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class MockProviderClientTests {

	@Test
	void socketTimeoutIsClassifiedAsTimeout() {
		ResourceAccessException exception = new ResourceAccessException(
				"read timed out", new SocketTimeoutException("Read timed out"));

		assertThat(MockProviderClient.toNetworkFailureReason(exception))
				.isEqualTo(FailureReason.TIMEOUT);
	}

	@Test
	void ephemeralPortExhaustionIsClassifiedAsConnectFail() {
		ResourceAccessException exception = new ResourceAccessException(
				"I/O error on POST request", new BindException("Address already in use: getsockopt"));

		assertThat(MockProviderClient.toNetworkFailureReason(exception))
				.isEqualTo(FailureReason.CONNECT_FAIL);
	}

	@Test
	void connectionRefusedIsClassifiedAsConnectFail() {
		ResourceAccessException exception = new ResourceAccessException(
				"I/O error on POST request", new ConnectException("Connection refused"));

		assertThat(MockProviderClient.toNetworkFailureReason(exception))
				.isEqualTo(FailureReason.CONNECT_FAIL);
	}

	@Test
	void causeIsFoundThroughNestedExceptions() {
		ResourceAccessException exception = new ResourceAccessException(
				"I/O error on POST request",
				new IOException("wrapped", new BindException("Address already in use: getsockopt")));

		assertThat(MockProviderClient.toNetworkFailureReason(exception))
				.isEqualTo(FailureReason.CONNECT_FAIL);
	}

	@Test
	void unrecognizedCauseIsClassifiedAsUnknown() {
		ResourceAccessException exception = new ResourceAccessException(
				"I/O error on POST request", new IOException("연결이 끊겼습니다."));

		assertThat(MockProviderClient.toNetworkFailureReason(exception))
				.isEqualTo(FailureReason.UNKNOWN);
	}
}
