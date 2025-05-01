package com.cvitae.repository;

import com.cvitae.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    
    List<Resume> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Resume> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    @Query("SELECT r FROM Resume r WHERE r.status = ?1")
    List<Resume> findByStatus(String status);
    
    @Query("SELECT r FROM Resume r WHERE r.jobTitle LIKE %?1% OR r.companyName LIKE %?1%")
    List<Resume> searchByJobOrCompany(String keyword);
}
