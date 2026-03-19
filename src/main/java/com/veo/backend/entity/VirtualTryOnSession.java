package com.veo.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_try_on_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VirtualTryOnSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String inputImageUrl;

    private String faceShape;

    private String suggestedFrameSize;

    private String suggestedBrands;

    private String model3dUrl;

    private LocalDateTime createdAt;

    private String note;
}