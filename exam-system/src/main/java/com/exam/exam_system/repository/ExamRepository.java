package com.exam.exam_system.repository;

import com.exam.exam_system.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByStatus(Exam.ExamStatus status);
}

