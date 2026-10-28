package com.shiv.resumeanalyzer.analysis;

import java.util.List;

/**
 * The outcome of comparing one resume with one job description.
 *
 * @param score         0-100 fit score
 * @param summary       two or three sentences explaining the score
 * @param matchedSkills skills the job asks for that the resume shows
 * @param missingSkills skills the job asks for that the resume doesn't show
 * @param suggestions   concrete bullet rewrites or additions
 * @param source        whether the LLM or the keyword fallback produced this
 */
public record AnalysisResult(
        int score,
        String summary,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<BulletSuggestion> suggestions,
        AnalysisSource source) {

    public AnalysisResult {
        score = Math.max(0, Math.min(100, score));
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
        missingSkills = missingSkills == null ? List.of() : List.copyOf(missingSkills);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    /**
     * @param original the resume bullet to change, or null when the suggestion is a new bullet
     * @param improved the suggested wording
     * @param reason   why this helps for this particular job
     */
    public record BulletSuggestion(String original, String improved, String reason) {
    }
}
