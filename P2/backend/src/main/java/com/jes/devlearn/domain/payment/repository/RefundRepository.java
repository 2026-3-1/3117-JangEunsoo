package com.jes.devlearn.domain.payment.repository;

import com.jes.devlearn.domain.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByOrderIdOrderByIdDesc(Long orderId);
}
