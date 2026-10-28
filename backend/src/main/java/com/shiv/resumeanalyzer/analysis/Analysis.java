package com.shiv.resumeanalyzer.analysis;

import com.shiv.resumeanalyzer.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** One saved resume-vs-job comparison in a user's history. */
@Entity
@Table(name = "analyses", indexes = @Index(name = "idx_analyses_user_created", columnList = "user_id, created_at"))
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisSource source;

    @Column(name = "job_description", nullable = false, length = 20000)
    private String jobDescription;

    /** The full AnalysisResult as JSON, so the history view can show it without re-running the AI. */
    @Column(name = "result_json", nullable = false, length = 20000)
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Analysis() {
    }

    public Analysis(User user, String jobTitle, String resumeFileName, int score, AnalysisSource source,
                    String jobDescription, String resultJson) {
        this.user = user;
        this.jobTitle = jobTitle;
        this.resumeFileName = resumeFileName;
        this.score = score;
        this.source = source;
        this.jobDescription = jobDescription;
        this.resultJson = resultJson;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getJobTitle() { return jobTitle; }
    public String getResumeFileName() { return resumeFileName; }
    public int getScore() { return score; }
    public AnalysisSource getSource() { return source; }
    public String getJobDescription() { return jobDescription; }
    public String getResultJson() { return resultJson; }
    public Instant getCreatedAt() { return createdAt; }
}
