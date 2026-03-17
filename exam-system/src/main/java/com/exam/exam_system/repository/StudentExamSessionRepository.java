package com.exam.exam_system.repository;

import com.exam.exam_system.model.StudentExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentExamSessionRepository extends JpaRepository<StudentExamSession, Long> {
    // Tələbənin müəyyən imtahan üçün sessiyası
    Optional<StudentExamSession> findByStudentIdAndExamId(Long studentId, Long examId);
    // İmtahanın bütün sessiyaları
    List<StudentExamSession> findByExamId(Long examId);
    // Tələbənin bütün sessiyaları
    List<StudentExamSession> findByStudentId(Long studentId);
}