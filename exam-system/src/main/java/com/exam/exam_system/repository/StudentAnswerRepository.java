package com.exam.exam_system.repository;

import com.exam.exam_system.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findBySessionId(Long sessionId);
    Optional<StudentAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}