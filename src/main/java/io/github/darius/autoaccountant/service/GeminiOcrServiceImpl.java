package io.github.darius.autoaccountant.service;

import io.github.darius.autoaccountant.domain.FiscalConstants;
import io.github.darius.autoaccountant.dto.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Profile("!local")
public class GeminiOcrServiceImpl implements InvoiceOcrService {

    private static final Logger log = LoggerFactory.getLogger(GeminiOcrServiceImpl.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;
    private final String apiKey;
    private final String model;

    public GeminiOcrServiceImpl(
            ObjectMapper objectMapper,
            @Value("classpath:prompt.md") Resource promptResource,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model,
            @Value("${app.gemini.timeout.connect-seconds}") int connectTimeoutSeconds,
            @Value("${app.gemini.timeout.read-seconds}") int readTimeoutSeconds) throws Exception {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.promptTemplate = new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public OcrResult readInvoice(MultipartFile file, String sector) {
        try {
            String base64pdf = Base64.getEncoder().encodeToString(file.getBytes());
            String finalPrompt = buildPrompt(sector);
            Map<String, Object> requestBody = createRequestBody(finalPrompt, base64pdf, file.getContentType());

            String jsonResponse = restClient.post()
                    .uri(BASE_URL + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(jsonResponse);

        } catch (Exception ex) {
            log.error("Fallo al llamar a Gemini (modelo={}, archivo={})", model, file.getOriginalFilename(), ex);
            throw new RuntimeException("Error processing document with AI model", ex);
        }
    }

    private String buildPrompt(String sector) {
        boolean isVehicleIntensive = "TRANSPORT".equalsIgnoreCase(sector);
        return promptTemplate
                .replace("{{TAX_YEAR}}", String.valueOf(FiscalConstants.CURRENT_FISCAL_YEAR))
                .replace("{{ACTIVIDAD}}", sector)
                + "\n- Actividad de afectacion exclusiva de vehiculo, segun codigo IAE: " + isVehicleIntensive;
    }

    private Map<String, Object> createRequestBody(String prompt, String base64Data, String mimeType) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType != null ? mimeType : "application/pdf",
                                        "data", base64Data
                                ))
                        ))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );
    }

    private OcrResult parseResponse(String fullJsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(fullJsonResponse);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = root.path("promptFeedback").path("blockReason").asString("desconocido");
            log.error("Gemini no devolvió candidatos. Motivo: {}. Respuesta: {}", blockReason, fullJsonResponse);
            throw new IllegalStateException("La IA no devolvió ningún resultado (motivo: " + blockReason + ")");
        }

        JsonNode candidate = candidates.get(0);
        String finishReason = candidate.path("finishReason").asString("");
        if ("MAX_TOKENS".equals(finishReason)) {
            log.error("Respuesta truncada por límite de tokens. Sube maxOutputTokens o baja thinking_budget.");
            throw new IllegalStateException("La respuesta de la IA se truncó antes de completarse");
        }

        String rawText = candidate.path("content").path("parts").get(0).path("text").asString();

        String cleanJson = rawText.replaceAll("```json", "").replaceAll("```", "").trim();

        return objectMapper.readValue(cleanJson, OcrResult.class);
    }
}