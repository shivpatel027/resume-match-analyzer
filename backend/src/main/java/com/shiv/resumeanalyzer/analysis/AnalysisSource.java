package com.shiv.resumeanalyzer.analysis;

public enum AnalysisSource {
    /** Produced by the LLM. */
    AI,
    /** Produced by local keyword matching because the LLM was unavailable. */
    KEYWORD_FALLBACK
}
