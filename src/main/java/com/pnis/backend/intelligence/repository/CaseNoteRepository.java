package com.pnis.backend.intelligence.repository;

import com.pnis.backend.intelligence.model.CaseNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
    Page<CaseNote> findByIntelligenceCaseIdOrderByCreatedAtDesc(Long caseId, Pageable pageable);
}
