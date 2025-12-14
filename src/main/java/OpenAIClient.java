import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenAIClient {
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final URI baseUri;
	private final String apiKey;
	private final String model;

	public OpenAIClient(String apiKey, String model) {
		this(apiKey, model, URI.create("https://api.openai.com"));
	}

	public OpenAIClient(String apiKey, String model, URI baseUri) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("OPENAI_API_KEY is missing");
		}
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("OpenAI model is missing");
		}

		this.apiKey = apiKey;
		this.model = model;
		this.baseUri = (baseUri == null) ? URI.create("https://api.openai.com") : baseUri;
		this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		this.objectMapper = new ObjectMapper();
	}

	public String chat(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
		if (userPrompt == null || userPrompt.isBlank()) {
			throw new IllegalArgumentException("User prompt cannot be empty");
		}

		List<Map<String, String>> messages = new ArrayList<>();
		if (systemPrompt != null && !systemPrompt.isBlank()) {
			messages.add(Map.of("role", "system", "content", systemPrompt));
		}
		messages.add(Map.of("role", "user", "content", userPrompt));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("model", model);
		payload.put("temperature", 0.2);
		payload.put("messages", messages);

		String body = objectMapper.writeValueAsString(payload);

		HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/v1/chat/completions"))
			.header("Authorization", "Bearer " + apiKey)
			.header("Content-Type", "application/json")
			.timeout(REQUEST_TIMEOUT)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		int status = response.statusCode();
		String responseBody = response.body();

		if (status < 200 || status >= 300) {
			throw new IOException("OpenAI API request failed (" + status + "): " + truncate(responseBody, 800));
		}

		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode contentNode = root.at("/choices/0/message/content");
		if (contentNode.isMissingNode()) {
			throw new IOException("OpenAI API response missing message content");
		}

		return contentNode.asText();
	}

	private static String truncate(String value, int maxChars) {
		if (value == null) {
			return "";
		}
		if (value.length() <= maxChars) {
			return value;
		}
		return value.substring(0, maxChars) + "...";
	}
}

