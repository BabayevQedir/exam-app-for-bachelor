package com.exam.exam_system.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * TƏLƏBƏ entity-si
 *
 * Müəllim Excel Sheet2-dən yükləyir:
 *  - Ad Soyad Ata adı
 *  - Qrup nömrəsi
 *  - Parol (müəllim özü təyin edir)
 *
 * Tələbə bu parol ilə imtahana daxil olur.
 * Müəllim tələbəni tanımır — yalnız QR token görür.
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ad Soyad Ata adı
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Qrup nömrəsi (məsələn: B202)
    @Column(name = "group_name", nullable = false)
    private String groupName;

    // Tələbə nömrəsi (ixtiyari)
    @Column(name = "student_number")
    private String studentNumber;

    // Müəllimin Excel-də yazdığı parol (BCrypt ilə saxlanılır)
    @Column(name = "password", nullable = false)
    private String password;

    // QR kod tokeni (kağız imtahan üçün)
    @Column(name = "qr_token", unique = true)
    private String qrToken;

    // Hansı imtahana aid olduğu — bilet generasiyası üçün
    @Column(name = "exam_id")
    private Long examId;
}