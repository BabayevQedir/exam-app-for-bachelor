package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * TƏLƏBƏ İMTAHAN SESSİYASI
 *
 * Tələbə imtahana daxil olanda yaranır.
 * Vaxt sayğacı burada izlənilir.
 * Submit edəndə və ya vaxt bitəndə SUBMITTED olur.
 */
@Entity
@Table(name = "student_exam_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hansı tələbə
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Hansı imtahan
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // Başlama vaxtı
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // Bitmə vaxtı (startTime + durationMinutes)
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Submit edilmə vaxtı
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Sessiya statusu
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    // Müəllimin verdiyi yekun bal (sonradan)
    @Column(name = "total_score")
    private Integer totalScore;

    public enum SessionStatus {
        IN_PROGRESS,  // İmtahan gedir
        SUBMITTED,    // Tələbə özü göndərdi
        TIMEOUT       // Vaxt bitdi, avtomatik göndərildi
    }
}