package com.shiv.resumeanalyzer.ai;

import com.shiv.resumeanalyzer.analysis.AnalysisResult;
import com.shiv.resumeanalyzer.analysis.AnalysisResult.BulletSuggestion;
import com.shiv.resumeanalyzer.analysis.AnalysisSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fallback used when the LLM is unavailable: finds known skills in both texts with whole-word
 * matching and scores the resume by the share of the job's skills it covers.
 */
@Component
public class KeywordMatchAnalyzer {

    /** Display name -> spellings to look for. Order is the display order. */
    private static final Map<String, List<String>> SKILLS = new LinkedHashMap<>();

    static {
        add("Java", "java");
        add("Spring Boot", "spring boot", "springboot");
        add("Spring Security", "spring security");
        add("Hibernate/JPA", "hibernate", "jpa");
        add("Python", "python");
        add("JavaScript", "javascript");
        add("TypeScript", "typescript");
        add("React", "react", "react.js", "reactjs");
        add("Angular", "angular");
        add("Node.js", "node.js", "nodejs");
        add("C++", "c++");
        add("C#", "c#");
        add("Go", "golang");
        add("SQL", "sql");
        add("PostgreSQL", "postgresql", "postgres");
        add("MySQL", "mysql");
        add("MongoDB", "mongodb");
        add("Redis", "redis");
        add("Kafka", "kafka");
        add("REST APIs", "rest api", "rest apis", "restful");
        add("GraphQL", "graphql");
        add("Microservices", "microservices", "microservice");
        add("Docker", "docker");
        add("Kubernetes", "kubernetes", "k8s");
        add("AWS", "aws", "amazon web services");
        add("Azure", "azure");
        add("GCP", "gcp", "google cloud");
        add("CI/CD", "ci/cd", "continuous integration");
        add("GitHub Actions", "github actions");
        add("Jenkins", "jenkins");
        add("Git", "git");
        add("Linux", "linux");
        add("JUnit", "junit");
        add("Unit testing", "unit testing", "unit tests");
        add("HTML", "html");
        add("CSS", "css");
        add("PHP", "php");
        add("Machine learning", "machine learning");
        add("Agile", "agile", "scrum");
    }

    private static final Map<String, List<Pattern>> PATTERNS = new LinkedHashMap<>();

    static {
        SKILLS.forEach((name, spellings) -> PATTERNS.put(name, spellings.stream()
                // Whole-word match that also works for "c++", "c#" and "node.js": the alias must not
                // touch other letters/digits/symbols, so "java" doesn't match inside "javascript".
                .map(s -> Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(s) + "(?![a-z0-9+#])"))
                .toList()));
    }

    private static void add(String name, String... spellings) {
        SKILLS.put(name, List.of(spellings));
    }

    public List<String> findSkills(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        PATTERNS.forEach((name, patterns) -> {
            if (patterns.stream().anyMatch(p -> p.matcher(lower).find())) {
                found.add(name);
            }
        });
        return found;
    }

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        List<String> resumeSkills = findSkills(resumeText);
        List<String> jobSkills = findSkills(jobDescription);

        List<String> matched = jobSkills.stream().filter(resumeSkills::contains).toList();
        List<String> missing = jobSkills.stream().filter(s -> !resumeSkills.contains(s)).toList();
        int score = jobSkills.isEmpty() ? 0 : Math.round(100f * matched.size() / jobSkills.size());

        String summary = jobSkills.isEmpty()
                ? "No recognizable technical skills were found in the job description, so a keyword score isn't meaningful."
                : "Keyword check: your resume mentions %d of the %d skills this job lists.".formatted(matched.size(), jobSkills.size());

        List<BulletSuggestion> suggestions = missing.stream().limit(3)
                .map(skill -> new BulletSuggestion(null,
                        "If you've used " + skill + ", add a bullet showing what you built with it and the result.",
                        "The job asks for " + skill + " and your resume doesn't mention it."))
                .toList();

        return new AnalysisResult(score, summary, matched, missing, suggestions, AnalysisSource.KEYWORD_FALLBACK);
    }
}


//postgresql://neondb_owner:npg_eMSAmao6hT0B@ep-wispy-lake-b457lld1-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require