package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false)
    private Integer ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ticket_questions",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "qr_code_image", columnDefinition = "TEXT")
    private String qrCodeImage;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "scored_points")
    private Integer scoredPoints;

    @Column(name = "word_file_path")
    private String wordFilePath;
}
