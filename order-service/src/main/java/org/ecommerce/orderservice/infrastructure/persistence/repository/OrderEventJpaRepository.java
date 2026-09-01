package org.ecommerce.orderservice.infrastructure.persistence.repository;

import org.ecommerce.orderservice.domain.model.OrderEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderEventJpaRepository extends JpaRepository<OrderEvent, Long> {

    boolean existsByEventId(String eventId);

    @EntityGraph(attributePaths = "order")
    @Query("select e from OrderEvent e where e.publishedToKafka = false order by e.createdAt asc")
    List<OrderEvent> findUnpublishedForOutbox(Pageable pageable);
}


