package com.shiv.resumeanalyzer.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiv.resumeanalyzer.ai.AiUnavailableException;
import com.shiv.resumeanalyzer.ai.ClaudeMatchAnalyzer;
import com.shiv.resumeanalyzer.ai.KeywordMatchAnalyzer;
import com.shiv.resumeanalyzer.parsing.ResumeParser;
import com.shiv.resumeanalyzer.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final ClaudeMatchAnalyzer ai = mock(ClaudeMatchAnalyzer.class);
    private final AnalysisService service = new AnalysisService(
            mock(ResumeParser.class), ai, new KeywordMatchAnalyzer(),
            mock(AnalysisRepository.class), mock(UserRepository.class), new ObjectMapper());

    @Test
    void usesAiResultWhenAvailable() {
        AnalysisResult aiResult = new AnalysisResult(77, "ok", List.of("Java"), List.of(), List.of(), AnalysisSource.AI);
        when(ai.analyze(anyString(), anyString(), anyString())).thenReturn(aiResult);

        assertThat(service.score("Java dev", "Need Java")).isSameAs(aiResult);
    }

    @Test
    void fallsBackToKeywordsWhenAiFails() {
        when(ai.analyze(anyString(), anyString(), anyString()))
                .thenThrow(new AiUnavailableException("timeout"));

        AnalysisResult result = service.score("Java and Docker", "Need Java and AWS");

        assertThat(result.source()).isEqualTo(AnalysisSource.KEYWORD_FALLBACK);
        assertThat(result.score()).isEqualTo(50);
    }

    @Test
    void cacheKeyIgnoresWhitespaceDifferences() {
        assertThat(AnalysisService.cacheKey("Java   dev\n", "Need  Java"))
                .isEqualTo(AnalysisService.cacheKey("Java dev", "Need Java"))
                .isNotEqualTo(AnalysisService.cacheKey("Java dev", "Need Python"));
    }

    @Test
    void jobTitleIsFirstNonEmptyLine() {
        assertThat(AnalysisService.jobTitleFrom("\n\n  Backend Engineer II \nAbout us...")).isEqualTo("Backend Engineer II");
    }
}
