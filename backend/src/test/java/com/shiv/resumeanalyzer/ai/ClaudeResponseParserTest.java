package com.shiv.resumeanalyzer.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiv.resumeanalyzer.analysis.AnalysisResult;
import com.shiv.resumeanalyzer.analysis.AnalysisSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeResponseParserTest {

    private final ClaudeResponseParser parser = new ClaudeResponseParser(new ObjectMapper());

    @Test
    void parsesJsonWrappedInMarkdownFence() {
        String reply = """
                ```json
                {"score": 82, "summary": "Strong backend fit.",
                 "matchedSkills": ["Java", "Spring Boot"], "missingSkills": ["Kafka"],
                 "suggestions": [{"original": "Worked on APIs", "improved": "Built 12 REST endpoints", "reason": "Quantified"},
                                 {"original": null, "improved": "Add a Kafka project", "reason": "Job asks for it"}]}
                ```""";

        AnalysisResult result = parser.parse(reply);

        assertThat(result.score()).isEqualTo(82);
        assertThat(result.matchedSkills()).containsExactly("Java", "Spring Boot");
        assertThat(result.missingSkills()).containsExactly("Kafka");
        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.suggestions().get(1).original()).isNull();
        assertThat(result.source()).isEqualTo(AnalysisSource.AI);
    }

    @Test
    void clampsScoreToRange() {
        assertThat(parser.parse("{\"score\": 140}").score()).isEqualTo(100);
    }

    @Test
    void rejectsReplyWithoutScore() {
        assertThatThrownBy(() -> parser.parse("{\"summary\": \"no score\"}"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void rejectsNonJsonReply() {
        assertThatThrownBy(() -> parser.parse("Sorry, I can't help with that."))
                .isInstanceOf(AiUnavailableException.class);
    }
}
