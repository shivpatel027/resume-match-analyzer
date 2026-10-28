package com.shiv.resumeanalyzer.analysis;

import com.shiv.resumeanalyzer.analysis.AnalysisDtos.AnalysisResponse;
import com.shiv.resumeanalyzer.analysis.AnalysisDtos.AnalysisSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(Authentication auth,
                                   @RequestPart("resume") MultipartFile resume,
                                   @RequestParam("jobDescription") String jobDescription) {
        return analysisService.analyze(auth.getName(), resume, jobDescription);
    }

    @GetMapping
    public List<AnalysisSummary> history(Authentication auth) {
        return analysisService.history(auth.getName());
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(Authentication auth, @PathVariable Long id) {
        return analysisService.get(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        analysisService.delete(auth.getName(), id);
    }
}
