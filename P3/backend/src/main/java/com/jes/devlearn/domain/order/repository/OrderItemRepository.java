package com.jes.devlearn.domain.order.repository;

import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderId(Long orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<Long> orderIds);

    List<OrderItem> findAllByCourseIdAndStatus(Long courseId, OrderItemStatus status);

    /**
     * 강사 본인 강의의 실매출 합계.
     * 결제 완료(PAID/부분환불) 주문에 속한 ACTIVE(미환불) order_item 의 price_snapshot 합.
     */
    @Query("""
            SELECT COALESCE(SUM(oi.priceSnapshot), 0)
            FROM OrderItem oi
            JOIN Order o ON o.id = oi.orderId
            WHERE oi.courseId IN :courseIds
              AND oi.status = com.jes.devlearn.domain.order.entity.OrderItemStatus.ACTIVE
              AND o.status IN (com.jes.devlearn.domain.order.entity.OrderStatus.PAID,
                               com.jes.devlearn.domain.order.entity.OrderStatus.PARTIAL_REFUNDED)
            """)
    long sumActiveRevenueByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
