package com.exam.exam_system.controller;

import com.exam.exam_system.model.*;
import com.exam.exam_system.repository.*;
import com.exam.exam_system.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StudentRepository studentRepository;
    private final StudentExamSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ExamService examService;

    // Bütün tələbələr
    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("fullName", s.getFullName());
            m.put("groupName", s.getGroupName());
            m.put("examId", s.getExamId());
            m.put("qrToken", s.getQrToken());
            return m;
        }).toList());
    }

    // İmtahan nəticələri + tələbə adları
    @GetMapping("/exam/{examId}/results")
    public ResponseEntity<?> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(sessionRepository.findByExamId(examId).stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("sessionId", s.getId());
            m.put("studentName", s.getStudent().getFullName());
            m.put("groupName", s.getStudent().getGroupName());
            m.put("studentId", s.getStudent().getId());
            m.put("status", s.getStatus());
            m.put("startTime", s.getStartTime().toString());
            m.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "");
            m.put("totalScore", s.getTotalScore());
            return m;
        }).toList());
    }

    // Bilet + tələbə əlaqəsi
    @GetMapping("/exam/{examId}/tickets")
    public ResponseEntity<?> getTicketStudentMap(@PathVariable Long examId) {
        return ResponseEntity.ok(ticketRepository.findByExamIdOrderByTicketNumber(examId).stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("ticketNumber", t.getTicketNumber());
            m.put("studentId", t.getStudent() != null ? t.getStudent().getId() : null);
            m.put("studentName", t.getStudent() != null ? t.getStudent().getFullName() : "—");
            m.put("groupName", t.getStudent() != null ? t.getStudent().getGroupName() : "—");
            m.put("totalPoints", t.getTotalPoints());
            m.put("scoredPoints", t.getScoredPoints());
            return m;
        }).toList());
    }

    // İmtahanı sil
    @DeleteMapping("/exam/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        try {
            examRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "İmtahan silindi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bütün istifadəçilər
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("fullName", u.getFullName() != null ? u.getFullName() : "");
            m.put("role", u.getRole());
            m.put("enabled", u.isEnabled());
            return m;
        }).toList());
    }
}