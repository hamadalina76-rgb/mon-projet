package com.speedline.order.repository;

import com.speedline.order.domain.OrderInternalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInternalNoteRepository extends JpaRepository<OrderInternalNote, Long> {

    List<OrderInternalNote> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    long countByOrderId(Long orderId);
}
