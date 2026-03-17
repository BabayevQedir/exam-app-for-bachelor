package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * TƏLƏBƏ CAVABI
 *
 * Tələbənin hər suala verdiyi cavab saxlanılır.
 * 2 tip cavab var:
 *   1. Yazılı cavab (answerText) — tələbə ekranda yazır
 *   2. Fayl (attachedFilePath) — zip formatında yükləyir
 */
@Entity
@Table(name = "student_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hansı sessiya
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudentExamSession session;

    // Hansı suala cavab
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // Yazılı cavab mətni (tələbə ekranda yazırsa)
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    // Yüklənmiş zip faylının yolu
    @Column(name = "attached_file_path")
    private String attachedFilePath;

    // Faylın orijinal adı
    @Column(name = "original_file_name")
    private String originalFileName;

    // Cavab vaxtı
    @Column(name = "answered_at")
    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();

    // Müəllimin bu suala verdiyi bal (sonradan)
    @Column(name = "score")
    private Integer score;
}