package com.exam.exam_system.controller;

import com.exam.exam_system.model.*;
import com.exam.exam_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin paneli — yalnız ADMIN rolu görə bilər
 *
 * GET /api/admin/students          → Bütün tələbələr (ad + qrup + parol yox)
 * GET /api/admin/exam/{id}/results → İmtahan nəticələri + tələbə adları
 * GET /api/admin/sessions          → Bütün sessiyalar
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StudentRepository studentRepository;
    private final StudentExamSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;

    // Bütün tələbələr
    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        return ResponseEntity.ok(students.stream().map(s -> Map.of(
                "id", s.getId(),
                "fullName", s.getFullName(),
                "groupName", s.getGroupName(),
                "studentNumber", s.getStudentNumber() != null ? s.getStudentNumber() : "",
                "qrToken", s.getQrToken()
        )).toList());
    }

    // İmtahan nəticələri + tələbə adları (admin görür)
    @GetMapping("/exam/{examId}/results")
    public ResponseEntity<?> getExamResults(@PathVariable Long examId) {
        List<StudentExamSession> sessions = sessionRepository.findByExamId(examId);
        return ResponseEntity.ok(sessions.stream().map(s -> Map.of(
                "sessionId", s.getId(),
                "studentName", s.getStudent().getFullName(),
                "groupName", s.getStudent().getGroupName(),
                "status", s.getStatus(),
                "startTime", s.getStartTime().toString(),
                "submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "",
                "totalScore", s.getTotalScore() != null ? s.getTotalScore() : 0
        )).toList());
    }

    // Bilet + tələbə əlaqəsi (kağız imtahan üçün)
    @GetMapping("/exam/{examId}/tickets")
    public ResponseEntity<?> getTicketStudentMap(@PathVariable Long examId) {
        List<Ticket> tickets = ticketRepository.findByExamIdOrderByTicketNumber(examId);
        return ResponseEntity.ok(tickets.stream().map(t -> Map.of(
                "ticketNumber", t.getTicketNumber(),
                "studentName", t.getStudent() != null ? t.getStudent().getFullName() : "—",
                "groupName", t.getStudent() != null ? t.getStudent().getGroupName() : "—",
                "qrToken", t.getQrToken() != null ? t.getQrToken() : "",
                "totalPoints", t.getTotalPoints(),
                "scoredPoints", t.getScoredPoints() != null ? t.getScoredPoints() : 0
        )).toList());
    }
}