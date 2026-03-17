package com.exam.exam_system.repository;
import com.exam.exam_system.model.TicketResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketResultRepository extends JpaRepository<TicketResult, Long> {
    List<TicketResult> findByExamIdOrderByScoredPointsDesc(Long examId);

    @Query("SELECT AVG(r.percentage) FROM TicketResult r WHERE r.exam.id = :examId")
    Double findAveragePercentageByExamId(Long examId);
}