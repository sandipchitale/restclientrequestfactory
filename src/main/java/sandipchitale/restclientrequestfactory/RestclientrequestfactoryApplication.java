package sandipchitale.restclientrequestfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;

@SpringBootApplication
public class RestclientrequestfactoryApplication {
	public static void main(String[] args) {
		SpringApplication.run(RestclientrequestfactoryApplication.class, args);
	}

	@RestController
	public static class ProxyController {

		private final RestClient.Builder restClientBuilder;
		private final String X_CONNECTION_TIMEOUT_MILLIS = "X-CONNECTION-TIMEOUT-MILLIS";
		private final String X_READ_TIMEOUT_MILLIS = "X-READ-TIMEOUT-MILLIS";

		public ProxyController(RestClient.Builder restClientBuilder) {
			this.restClientBuilder = restClientBuilder;
		}

		record Todo(long id, long userId, String title, boolean completed){};

		@ExceptionHandler({SocketTimeoutException.class})
		ResponseEntity<?> handleTimeoutException(Exception exception) {
			return ResponseEntity
					.status(HttpStatus.GATEWAY_TIMEOUT.value())
					.body(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase() + " : " + exception.getMessage());
		}

		@GetMapping("/")
		public Todo proxy(
				@RequestHeader(value = X_CONNECTION_TIMEOUT_MILLIS, required = false) String connectionTimeout,
				@RequestHeader(value = X_READ_TIMEOUT_MILLIS, required = false) String readTimeout) {

			if (connectionTimeout == null && readTimeout == null) {
				return restClientBuilder
						.build()
						.get()
						.uri("https://jsonplaceholder.typicode.com/todos/1")
						.retrieve()
						.body(Todo.class);
			}

			ClientHttpRequestFactorySettings clientHttpRequestFactorySettings =
					ClientHttpRequestFactorySettings.DEFAULTS;

			if (connectionTimeout != null) {
				Duration connectionTimeoutDuration = Duration.ofMillis(Long.parseLong(connectionTimeout));
				clientHttpRequestFactorySettings = clientHttpRequestFactorySettings.withConnectTimeout(connectionTimeoutDuration);
			}

			if (readTimeout != null) {
				Duration readTimeoutDuration = Duration.ofMillis(Long.parseLong(readTimeout));
				clientHttpRequestFactorySettings = clientHttpRequestFactorySettings.withReadTimeout(readTimeoutDuration);
			}

			return restClientBuilder
					.requestFactory(ClientHttpRequestFactories.get(clientHttpRequestFactorySettings))
					.build()
					.get()
						.uri("https://jsonplaceholder.typicode.com/todos/1")
					.retrieve()
					.body(Todo.class);
		}
	}

}
