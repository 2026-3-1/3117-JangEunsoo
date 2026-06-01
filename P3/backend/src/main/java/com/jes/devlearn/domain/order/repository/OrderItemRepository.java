package com.jes.devlearn.domain.order.repository;

import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderId(Long orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<Long> orderIds);

    List<OrderItem> findAllByCourseIdAndStatus(Long courseId, OrderItemStatus status);
}
