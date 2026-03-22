package com.veo.backend.entity;

import com.veo.backend.enums.ReturnRequestStatus;
import com.veo.backend.enums.ReturnRequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnRequestStatus status;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidenceImageUrl;

    private BigDecimal requestedAmount;

    private BigDecimal approvedAmount;

    private String refundBankName;

    private String refundBankAccountNumber;

    private String refundBankAccountName;

    @Column(columnDefinition = "TEXT")
    private String staffNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnRequestItem> items = new ArrayList<>();
}
