package com.veo.backend.repository;

import com.veo.backend.entity.VirtualTryOnSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VirtualTryOnSessionRepository extends JpaRepository<VirtualTryOnSession, Long> {
    List<VirtualTryOnSession> findByUserId(Long userId);
}