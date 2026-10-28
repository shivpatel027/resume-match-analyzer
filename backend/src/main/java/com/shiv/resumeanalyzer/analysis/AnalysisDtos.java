package com.shiv.resumeanalyzer.analysis;

import java.time.Instant;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    /** A row in the history list. */
    public record AnalysisSummary(Long id, String jobTitle, String resumeFileName, int score,
                                  AnalysisSource source, Instant createdAt) {
        static AnalysisSummary from(Analysis a) {
            return new AnalysisSummary(a.getId(), a.getJobTitle(), a.getResumeFileName(), a.getScore(),
                    a.getSource(), a.getCreatedAt());
        }
    }

    /** A full saved analysis. */
    public record AnalysisResponse(Long id, String jobTitle, String resumeFileName, String jobDescription,
                                   Instant createdAt, AnalysisResult result) {
    }
}
