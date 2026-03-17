package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * İMTAHAN entity-si
 *
 * examType → müəllim özü seçir:
 *   COMPUTER → tələbə sistemdə parol ilə girir, suallar ekranda
 *   PAPER    → nəzarətçi QR oxudur, Word bilet verilir
 */
@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_name", nullable = false)
    private String examName;

    @Column(name = "subject")
    private String subject;

    // Neçə bilet (kağız imtahan üçün) və ya neçə tələbə (kompüter üçün)
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;

    // Hər biletdə / tələbəyə neçə sual
    @Column(name = "questions_per_ticket", nullable = false)
    private Integer questionsPerTicket;

    // İmtahan müddəti (dəqiqə)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // İmtahan növü — müəllim seçir
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    // İmtahan statusu
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    public enum ExamType {
        COMPUTER,  // Tələbə sistemdə verir
        PAPER      // Kağız bilet + QR kod
    }

    public enum ExamStatus {
        DRAFT,      // Suallar yüklənib, hələ aktiv deyil
        ACTIVE,     // Tələbələr imtahan verə bilər
        FINISHED    // İmtahan bitib
    }
}