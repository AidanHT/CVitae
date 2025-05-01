package com.cvitae.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "master_resume", columnDefinition = "TEXT")
    private String masterResume;

    @Column(name = "job_posting", columnDefinition = "TEXT")
    private String jobPosting;

    @Column(name = "tailored_resume", columnDefinition = "TEXT")
    private String tailoredResume;

    @Column(name = "latex_code", columnDefinition = "TEXT")
    private String latexCode;

    @Column(name = "target_length")
    private Integer targetLength;

    @Column(name = "status")
    private String status; // GENERATING, COMPLETED, ERROR

    @Column(name = "ats_score")
    private Double atsScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
