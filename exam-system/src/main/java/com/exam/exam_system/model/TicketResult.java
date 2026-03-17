package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "scored_points")
    private Integer scoredPoints;

    @Column(name = "percentage")
    private Double percentage;

    @Column(name = "grade")
    private String grade;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt = LocalDateTime.now();

    public void calculateGrade() {
        if (maxPoints != null && maxPoints > 0 && scoredPoints != null) {
            this.percentage = (scoredPoints * 100.0) / maxPoints;
            if (percentage >= 91) this.grade = "A";
            else if (percentage >= 81) this.grade = "B";
            else if (percentage >= 71) this.grade = "C";
            else if (percentage >= 61) this.grade = "D";
            else this.grade = "F";
        }
    }
}