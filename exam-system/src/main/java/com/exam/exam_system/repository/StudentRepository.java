package com.exam.exam_system.repository;

import com.exam.exam_system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByGroupName(String groupName);

    Optional<Student> findByQrToken(String qrToken);
    // İmtahana aid tələbələr
    List<Student> findByExamId(Long examId);
}
