package com.jes.devlearn.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_title_snapshot", nullable = false, length = 255)
    private String courseTitleSnapshot;

    @Column(name = "price_snapshot", nullable = false)
    private Long priceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status = OrderItemStatus.ACTIVE;

    public OrderItem(Long orderId, Long courseId, String courseTitleSnapshot, Long priceSnapshot) {
        this.orderId = orderId;
        this.courseId = courseId;
        this.courseTitleSnapshot = courseTitleSnapshot;
        this.priceSnapshot = priceSnapshot;
        this.status = OrderItemStatus.ACTIVE;
    }

    public void markRefunded() {
        this.status = OrderItemStatus.REFUNDED;
    }

    public boolean isActive() {
        return this.status == OrderItemStatus.ACTIVE;
    }
}
