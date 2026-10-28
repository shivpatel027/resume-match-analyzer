package com.shiv.resumeanalyzer.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiv.resumeanalyzer.ai.ClaudeMatchAnalyzer;
import com.shiv.resumeanalyzer.ai.KeywordMatchAnalyzer;
import com.shiv.resumeanalyzer.analysis.AnalysisDtos.AnalysisResponse;
import com.shiv.resumeanalyzer.analysis.AnalysisDtos.AnalysisSummary;
import com.shiv.resumeanalyzer.parsing.ResumeParser;
import com.shiv.resumeanalyzer.user.User;
import com.shiv.resumeanalyzer.user.UserRepository;
import com.shiv.resumeanalyzer.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    static final int MIN_JOB_DESCRIPTION_CHARS = 50;
    static final int MAX_JOB_DESCRIPTION_CHARS = 15000;

    private final ResumeParser resumeParser;
    private final ClaudeMatchAnalyzer aiAnalyzer;
    private final KeywordMatchAnalyzer keywordAnalyzer;
    private final AnalysisRepository analyses;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public AnalysisService(ResumeParser resumeParser, ClaudeMatchAnalyzer aiAnalyzer,
                           KeywordMatchAnalyzer keywordAnalyzer, AnalysisRepository analyses,
                           UserRepository users, ObjectMapper objectMapper) {
        this.resumeParser = resumeParser;
        this.aiAnalyzer = aiAnalyzer;
        this.keywordAnalyzer = keywordAnalyzer;
        this.analyses = analyses;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisResponse analyze(String email, MultipartFile resume, String jobDescription) {
        String jd = jobDescription == null ? "" : jobDescription.trim();
        if (jd.length() < MIN_JOB_DESCRIPTION_CHARS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Paste the full job description (at least " + MIN_JOB_DESCRIPTION_CHARS + " characters).");
        }
        if (jd.length() > MAX_JOB_DESCRIPTION_CHARS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The job description is too long. Keep it under " + MAX_JOB_DESCRIPTION_CHARS + " characters.");
        }
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your session is no longer valid. Log in again."));

        String resumeText = resumeParser.extractText(resume);
        AnalysisResult result = score(resumeText, jd);

        Analysis saved = analyses.save(new Analysis(user, jobTitleFrom(jd), resume.getOriginalFilename(),
                result.score(), result.source(), jd, toJson(result)));
        return toResponse(saved, result);
    }

    /** Tries the LLM first; on any failure falls back to keyword matching so the user still gets a result. */
    AnalysisResult score(String resumeText, String jobDescription) {
        try {
            return aiAnalyzer.analyze(cacheKey(resumeText, jobDescription), resumeText, jobDescription);
        } catch (RuntimeException e) {
            log.warn("AI analysis unavailable, using keyword fallback: {}", e.getMessage());
            return keywordAnalyzer.analyze(resumeText, jobDescription);
        }
    }

    @Transactional(readOnly = true)
    public List<AnalysisSummary> history(String email) {
        return analyses.findTop50ByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(AnalysisSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(String email, Long id) {
        Analysis analysis = findOwned(email, id);
        return toResponse(analysis, fromJson(analysis.getResultJson()));
    }

    @Transactional
    public void delete(String email, Long id) {
        analyses.delete(findOwned(email, id));
    }

    private Analysis findOwned(String email, Long id) {
        // Scoped by owner, so one user can never read another user's analysis by guessing ids.
        return analyses.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That analysis doesn't exist."));
    }

    private static AnalysisResponse toResponse(Analysis a, AnalysisResult result) {
        return new AnalysisResponse(a.getId(), a.getJobTitle(), a.getResumeFileName(), a.getJobDescription(),
                a.getCreatedAt(), result);
    }

    /** Uses the first non-empty line of the posting (usually the role title) as a label. */
    static String jobTitleFrom(String jd) {
        String firstLine = jd.lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("Untitled job");
        return firstLine.length() > 100 ? firstLine.substring(0, 97) + "..." : firstLine;
    }

    static String cacheKey(String resumeText, String jobDescription) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(normalize(resumeText).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(normalize(jobDescription).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private String toJson(AnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't serialize analysis result", e);
        }
    }

    private AnalysisResult fromJson(String json) {
        try {
            return objectMapper.readValue(json, AnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored analysis result is corrupt", e);
        }
    }
}
