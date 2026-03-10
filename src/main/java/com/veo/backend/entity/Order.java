package com.veo.backend.entity;

import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;      // PENDING_PAYMENT, SHIPPING...

    @Enumerated(EnumType.STRING)
    private OrderType orderType;   // NORMAL, PRE_ORDER, PRESCRIPTION

    private BigDecimal totalAmount;

    private BigDecimal shippingFee;

    private BigDecimal discountAmount;

    private String shippingAddress;

    private String phoneNumber;

    private String receiverName;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}
