package com.shiv.resumeanalyzer.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiv.resumeanalyzer.analysis.AnalysisResult;
import com.shiv.resumeanalyzer.analysis.AnalysisResult.BulletSuggestion;
import com.shiv.resumeanalyzer.analysis.AnalysisSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Turns the model's text reply into an {@link AnalysisResult}, rejecting anything malformed. */
@Component
public class ClaudeResponseParser {

    private final ObjectMapper objectMapper;

    public ClaudeResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AnalysisResult parse(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            throw new AiUnavailableException("Model returned an empty response");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(extractJsonObject(modelText));
        } catch (JsonProcessingException e) {
            throw new AiUnavailableException("Model response was not valid JSON", e);
        }

        JsonNode score = root.path("score");
        if (!score.isNumber()) {
            throw new AiUnavailableException("Model response had no numeric score");
        }

        List<BulletSuggestion> suggestions = new ArrayList<>();
        for (JsonNode s : root.path("suggestions")) {
            String improved = text(s, "improved");
            if (improved != null) {
                suggestions.add(new BulletSuggestion(text(s, "original"), improved, text(s, "reason")));
            }
        }

        return new AnalysisResult(
                (int) Math.round(score.asDouble()),
                text(root, "summary"),
                strings(root.path("matchedSkills")),
                strings(root.path("missingSkills")),
                suggestions,
                AnalysisSource.AI);
    }

    /** Models sometimes wrap JSON in ```json fences or add a sentence; keep only the outer object. */
    static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiUnavailableException("Model response contained no JSON object");
        }
        return text.substring(start, end + 1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        for (JsonNode item : array) {
            if (item.isTextual() && !item.asText().isBlank()) {
                out.add(item.asText().trim());
            }
        }
        return out;
    }
}
