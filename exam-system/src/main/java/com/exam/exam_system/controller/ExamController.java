package com.exam.exam_system.controller;

import com.exam.exam_system.model.Exam;
import com.exam.exam_system.model.Question;
import com.exam.exam_system.repository.QuestionRepository;
import com.exam.exam_system.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final QuestionRepository questionRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createExam(
            @RequestParam("examName") String examName,
            @RequestParam("subject") String subject,
            @RequestParam("ticketCount") Integer ticketCount,
            @RequestParam("questionsPerTicket") Integer questionsPerTicket,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("examType") String examType,
            @RequestParam("file") MultipartFile file) {
        try {
            Exam exam = examService.createExamWithQuestions(
                    examName, subject, ticketCount, questionsPerTicket, durationMinutes, examType, file);
            long questionCount = questionRepository.findByExamId(exam.getId()).size();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", exam.getId(),
                    "examName", exam.getExamName(),
                    "subject", exam.getSubject(),
                    "ticketCount", exam.getTicketCount(),
                    "questionsPerTicket", exam.getQuestionsPerTicket(),
                    "durationMinutes", exam.getDurationMinutes(),
                    "examType", exam.getExamType(),
                    "status", exam.getStatus(),
                    "questionCount", questionCount,
                    "message", "İmtahan uğurla yaradıldı"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Xəta: " + e.getMessage()));
        }
    }

    // Aktiv et
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateExam(@PathVariable Long id) {
        try {
            Exam exam = examService.activateExam(id);
            return ResponseEntity.ok(Map.of("message", "İmtahan aktiv edildi", "status", exam.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bitir
    @PostMapping("/{id}/finish")
    public ResponseEntity<?> finishExam(@PathVariable Long id) {
        try {
            Exam exam = examService.finishExam(id);
            return ResponseEntity.ok(Map.of("message", "İmtahan bitirildi", "status", exam.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // İmtahanı redaktə et
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateExam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Exam exam = examService.getExamById(id);
            if (body.containsKey("examName")) exam.setExamName((String) body.get("examName"));
            if (body.containsKey("subject")) exam.setSubject((String) body.get("subject"));
            if (body.containsKey("durationMinutes")) exam.setDurationMinutes((Integer) body.get("durationMinutes"));
            if (body.containsKey("questionsPerTicket")) exam.setQuestionsPerTicket((Integer) body.get("questionsPerTicket"));
            Exam saved = examService.saveExam(exam);
            return ResponseEntity.ok(Map.of("message", "İmtahan yeniləndi", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Status dəyiş (DRAFT, ACTIVE, FINISHED)
    @PostMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Exam exam = examService.changeStatus(id, body.get("status"));
            return ResponseEntity.ok(Map.of("message", "Status dəyişdirildi", "status", exam.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bütün imtahanlar
    @GetMapping("/list")
    public ResponseEntity<?> getAllExams() {
        return ResponseEntity.ok(examService.getAllExams().stream().map(e -> Map.of(
                "id", e.getId(),
                "examName", e.getExamName(),
                "subject", e.getSubject() != null ? e.getSubject() : "",
                "ticketCount", e.getTicketCount(),
                "questionsPerTicket", e.getQuestionsPerTicket(),
                "durationMinutes", e.getDurationMinutes(),
                "examType", e.getExamType(),
                "status", e.getStatus(),
                "createdAt", e.getCreatedAt().toString()
        )).toList());
    }

    // Aktiv imtahanlar
    @GetMapping("/active")
    public ResponseEntity<?> getActiveExams() {
        return ResponseEntity.ok(examService.getActiveExams().stream().map(e -> Map.of(
                "id", e.getId(),
                "examName", e.getExamName(),
                "subject", e.getSubject() != null ? e.getSubject() : "",
                "durationMinutes", e.getDurationMinutes(),
                "examType", e.getExamType()
        )).toList());
    }

    // İmtahanın sualları
    @GetMapping("/{id}/questions")
    public ResponseEntity<?> getExamQuestions(@PathVariable Long id) {
        try {
            examService.getExamById(id);
            List<Question> questions = questionRepository.findByExamId(id);
            return ResponseEntity.ok(questions.stream().map(q -> Map.of(
                    "id", q.getId(),
                    "questionText", q.getQuestionText(),
                    "points", q.getPoints()
            )).toList());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}