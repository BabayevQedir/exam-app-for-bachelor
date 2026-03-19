package com.exam.exam_system.controller;

import com.exam.exam_system.model.*;
import com.exam.exam_system.service.StudentExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentExamService studentExamService;

    // Yalnız parol ilə imtahana daxil ol
    @PostMapping("/exam/{examId}/start")
    public ResponseEntity<?> startExam(
            @PathVariable Long examId,
            @RequestBody StartExamRequest request) {
        try {
            StudentExamSession session = studentExamService.startExam(examId, request.password());

            // Tələbənin adını və qrupunu da qaytarırıq — ekranda göstərmək üçün
            Student student = session.getStudent();

            return ResponseEntity.ok(Map.of(
                    "sessionId", session.getId(),
                    "startTime", session.getStartTime().toString(),
                    "endTime", session.getEndTime().toString(),
                    "durationMinutes", session.getExam().getDurationMinutes(),
                    "studentName", student.getFullName(),
                    "groupName", student.getGroupName(),
                    "message", "İmtahana uğurla daxil oldunuz"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Random sualları al
    @GetMapping("/session/{sessionId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable Long sessionId) {
        try {
            List<Question> questions = studentExamService.getRandomQuestions(sessionId);
            return ResponseEntity.ok(questions.stream().map(q -> Map.of(
                    "id", q.getId(),
                    "questionText", q.getQuestionText(),
                    "points", q.getPoints()
            )).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Mətn cavab saxla
    @PostMapping("/session/{sessionId}/answer/text")
    public ResponseEntity<?> saveTextAnswer(
            @PathVariable Long sessionId,
            @RequestBody TextAnswerRequest request) {
        try {
            StudentAnswer answer = studentExamService.saveTextAnswer(
                    sessionId, request.questionId(), request.answerText());
            return ResponseEntity.ok(Map.of(
                    "message", "Cavab saxlandı",
                    "answerId", answer.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Fayl cavab yüklə
    @PostMapping("/session/{sessionId}/answer/file")
    public ResponseEntity<?> saveFileAnswer(
            @PathVariable Long sessionId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("file") MultipartFile file) {
        try {
            StudentAnswer answer = studentExamService.saveFileAnswer(sessionId, questionId, file);
            return ResponseEntity.ok(Map.of(
                    "message", "Fayl yükləndi",
                    "fileName", answer.getOriginalFileName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Submit et
    @PostMapping("/session/{sessionId}/submit")
    public ResponseEntity<?> submitExam(@PathVariable Long sessionId) {
        try {
            StudentExamSession session = studentExamService.submitExam(sessionId);
            return ResponseEntity.ok(Map.of(
                    "message", "İmtahan uğurla təhvil verildi",
                    "submittedAt", session.getSubmittedAt().toString(),
                    "status", session.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    public record StartExamRequest(String password) {}
    public record TextAnswerRequest(Long questionId, String answerText) {}
}