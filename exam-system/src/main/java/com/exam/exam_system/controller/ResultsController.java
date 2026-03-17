package com.exam.exam_system.controller;

import com.exam.exam_system.model.*;
import com.exam.exam_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.File;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultsController {

    private final StudentExamSessionRepository sessionRepository;
    private final StudentAnswerRepository answerRepository;

    @GetMapping("/exam/{examId}/sessions")
    public ResponseEntity<?> getSessions(@PathVariable Long examId) {
        List<StudentExamSession> sessions = sessionRepository.findByExamId(examId);
        return ResponseEntity.ok(sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("status", s.getStatus());
            map.put("startTime", s.getStartTime().toString());
            map.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "");
            map.put("totalScore", s.getTotalScore());
            return map;
        }).toList());
    }

    @GetMapping("/session/{sessionId}/answers")
    public ResponseEntity<?> getAnswers(@PathVariable Long sessionId) {
        List<StudentAnswer> answers = answerRepository.findBySessionId(sessionId);
        return ResponseEntity.ok(answers.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("questionText", a.getQuestion().getQuestionText());
            map.put("points", a.getQuestion().getPoints());
            map.put("answerText", a.getAnswerText() != null ? a.getAnswerText() : "");
            map.put("fileName", a.getOriginalFileName() != null ? a.getOriginalFileName() : "");
            map.put("hasFile", a.getAttachedFilePath() != null && !a.getAttachedFilePath().isEmpty());
            map.put("answeredAt", a.getAnsweredAt() != null ? a.getAnsweredAt().toString() : "");
            return map;
        }).toList());
    }

    @PostMapping("/session/{sessionId}/score")
    public ResponseEntity<?> setScore(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Integer> body) {
        try {
            StudentExamSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sessiya tapılmadı"));
            session.setTotalScore(body.get("score"));
            sessionRepository.save(session);
            return ResponseEntity.ok(Map.of("message", "Bal yazıldı", "score", body.get("score")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/answer/{answerId}/download")
    public ResponseEntity<Resource> downloadAnswer(@PathVariable Long answerId) {
        StudentAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Cavab tapılmadı"));

        if (answer.getAttachedFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(answer.getAttachedFilePath());
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + answer.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}