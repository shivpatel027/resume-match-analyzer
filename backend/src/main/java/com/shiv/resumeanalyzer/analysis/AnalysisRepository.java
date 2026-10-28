package com.shiv.resumeanalyzer.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    List<Analysis> findTop50ByUserEmailOrderByCreatedAtDesc(String email);

    Optional<Analysis> findByIdAndUserEmail(Long id, String email);
}
