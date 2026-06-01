package com.jes.devlearn.domain.payment.repository;

import com.jes.devlearn.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findAllByOrderIdOrderByIdDesc(Long orderId);
}
