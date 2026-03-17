package com.exam.exam_system.controller;

import com.exam.exam_system.model.Student;
import com.exam.exam_system.model.Ticket;
import com.exam.exam_system.repository.StudentRepository;
import com.exam.exam_system.repository.TicketRepository;
import com.exam.exam_system.service.TicketGeneratorService;
import com.exam.exam_system.service.WordExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketGeneratorService ticketGeneratorService;
    private final TicketRepository ticketRepository;
    private final StudentRepository studentRepository;
    private final WordExportService wordExportService;

    // Bütün biletləri yarat
    @PostMapping("/api/tickets/{examId}/generate")
    public ResponseEntity<?> generateAllTickets(@PathVariable Long examId) {
        try {
            List<Ticket> tickets = ticketGeneratorService.generateAllTickets(examId);
            return ResponseEntity.ok(Map.of(
                    "message", tickets.size() + " bilet uğurla yaradıldı",
                    "count", tickets.size(),
                    "tickets", tickets.stream().map(t -> Map.of(
                            "id", t.getId(),
                            "ticketNumber", t.getTicketNumber(),
                            "totalPoints", t.getTotalPoints()
                    )).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bütün biletləri TƏK Word faylında yüklə (çap üçün)
    @GetMapping("/api/tickets/{examId}/download-all")
    public ResponseEntity<Resource> downloadAllTickets(@PathVariable Long examId) {
        try {
            List<Ticket> tickets = ticketRepository.findByExamIdOrderByTicketNumber(examId);
            if (tickets.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String filePath = wordExportService.generateAllTicketsWord(examId, tickets);
            File file = new File(filePath);
            if (!file.exists()) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"exam_" + examId + "_biletler.docx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Biletlərin siyahısı
    @GetMapping("/api/tickets/{examId}/list")
    public ResponseEntity<?> getTicketList(@PathVariable Long examId) {
        List<Ticket> tickets = ticketRepository.findByExamIdOrderByTicketNumber(examId);
        return ResponseEntity.ok(tickets.stream().map(t -> Map.of(
                "id", t.getId(),
                "ticketNumber", t.getTicketNumber(),
                "totalPoints", t.getTotalPoints(),
                "scoredPoints", t.getScoredPoints() != null ? t.getScoredPoints() : 0
        )).toList());
    }

    // Tək bilet yüklə
    @GetMapping("/api/tickets/download/{ticketId}")
    public ResponseEntity<Resource> downloadTicket(@PathVariable Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty() || ticketOpt.get().getWordFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(ticketOpt.get().getWordFilePath());
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bilet_" + ticketOpt.get().getTicketNumber() + ".docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    // Müəllim bal yazır (anonim — tələbə adı görsənmir)
    @PostMapping("/api/tickets/{ticketId}/score")
    public ResponseEntity<?> setScore(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Integer> body) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Bilet tapılmadı"));
            ticket.setScoredPoints(body.get("score"));
            ticketRepository.save(ticket);
            return ResponseEntity.ok(Map.of(
                    "message", "Bal yazıldı",
                    "ticketNumber", ticket.getTicketNumber(),
                    "score", body.get("score"),
                    "maxPoints", ticket.getTotalPoints()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // QR oxuma — nəzarətçi
    @GetMapping("/api/qr/scan")
    public ResponseEntity<?> scanQrCode(@RequestParam("token") String token) {
        Optional<Student> studentOpt = studentRepository.findByQrToken(token);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "QR kod tanınmadı"));
        }
        Student student = studentOpt.get();
        Optional<Ticket> ticketOpt = ticketRepository.findByQrToken(token);

        if (ticketOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "studentName", student.getFullName(),
                    "groupName", student.getGroupName(),
                    "message", "Bilet hələ yaradılmayıb"
            ));
        }

        Ticket ticket = ticketOpt.get();
        return ResponseEntity.ok(Map.of(
                "studentName", student.getFullName(),
                "groupName", student.getGroupName(),
                "ticketNumber", ticket.getTicketNumber(),
                "totalPoints", ticket.getTotalPoints(),
                "durationMinutes", ticket.getExam().getDurationMinutes(),
                "message", "✅ Bu bileti həmin tələbəyə verin"
        ));
    }
}