package com.shiv.resumeanalyzer.ai;

import com.shiv.resumeanalyzer.analysis.AnalysisResult;
import com.shiv.resumeanalyzer.analysis.AnalysisSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMatchAnalyzerTest {

    private final KeywordMatchAnalyzer analyzer = new KeywordMatchAnalyzer();

    @Test
    void javaDoesNotMatchInsideJavascript() {
        assertThat(analyzer.findSkills("Built UIs in JavaScript")).containsExactly("JavaScript");
    }

    @Test
    void matchesSkillsWithSymbolsAndAliases() {
        assertThat(analyzer.findSkills("C++, C#, Node.js, Postgres and K8s."))
                .contains("C++", "C#", "Node.js", "PostgreSQL", "Kubernetes");
    }

    @Test
    void scoresByShareOfJobSkillsCovered() {
        AnalysisResult result = analyzer.analyze(
                "Java developer with Spring Boot and Docker experience",
                "We need Java, Spring Boot, Kubernetes and AWS");

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.matchedSkills()).containsExactly("Java", "Spring Boot");
        assertThat(result.missingSkills()).containsExactly("Kubernetes", "AWS");
        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.source()).isEqualTo(AnalysisSource.KEYWORD_FALLBACK);
    }

    @Test
    void scoresZeroWhenJobListsNoKnownSkills() {
        assertThat(analyzer.analyze("Java", "Friendly team, great snacks").score()).isZero();
    }
}
