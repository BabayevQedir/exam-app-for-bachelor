package com.exam.exam_system.repository;

import com.exam.exam_system.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByExamIdOrderByTicketNumber(Long examId);
    Optional<Ticket> findByQrToken(String qrToken);
    boolean existsByExamId(Long examId);
    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
}