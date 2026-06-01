package com.jes.devlearn.domain.order.repository;

import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    long countByOrderNoStartingWith(String prefix);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status)")
    Page<Order> findAllForAdmin(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'PARTIAL_REFUNDED', 'REFUNDED')")
    long sumGrossRevenue();

    @Query("SELECT COALESCE(SUM(o.refundedAmount), 0) FROM Order o")
    long sumRefundedAmount();

    long countByStatus(OrderStatus status);
}
