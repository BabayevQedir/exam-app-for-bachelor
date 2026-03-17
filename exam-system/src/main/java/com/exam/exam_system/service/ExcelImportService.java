package com.exam.exam_system.service;

import com.exam.exam_system.model.Exam;
import com.exam.exam_system.model.Question;
import com.exam.exam_system.model.Student;
import com.exam.exam_system.repository.QuestionRepository;
import com.exam.exam_system.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Excel faylından sualları və tələbələri oxuyan servis.
 *
 * Sheet1 — Suallar:
 * ┌───────────────────────┬──────┐
 * │ Sual mətni            │ Bal  │
 * ├───────────────────────┼──────┤
 * │ Java nədir?           │  10  │
 * └───────────────────────┴──────┘
 *
 * Sheet2 — Tələbələr:
 * ┌───────┬──────────────────────┬────────┐
 * │ Qrup  │ Ad Soyad Ata adı     │ Parol  │
 * ├───────┼──────────────────────┼────────┤
 * │ B202  │ Əli Həsən Məmməd     │ 12345  │
 * └───────┴──────────────────────┴────────┘
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    // Sheet1 — Suallar
    public List<Question> importQuestionsFromExcel(MultipartFile file, Exam exam) throws IOException {
        validateFile(file);
        List<Question> questions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowNumber = 0;
            for (Row row : sheet) {
                if (rowNumber == 0) { rowNumber++; continue; }
                if (isRowEmpty(row)) { rowNumber++; continue; }
                try {
                    String questionText = getCellStringValue(row.getCell(0));
                    Integer points = getCellIntValue(row.getCell(1));
                    if (questionText == null || questionText.trim().isEmpty()) { rowNumber++; continue; }
                    if (points == null || points <= 0) { rowNumber++; continue; }
                    Question question = new Question();
                    question.setQuestionText(questionText.trim());
                    question.setPoints(points);
                    question.setExam(exam);
                    question.setOriginalRowNumber(rowNumber + 1);
                    questions.add(question);
                } catch (Exception e) {
                    log.error("Sual sırası {} xəta: {}", rowNumber + 1, e.getMessage());
                }
                rowNumber++;
            }
        }

        if (questions.isEmpty()) throw new IllegalArgumentException("Sheet1-də sual tapılmadı!");
        List<Question> saved = questionRepository.saveAll(questions);
        log.info("✅ {} sual saxlandı", saved.size());
        return saved;
    }

    // Sheet2 — Tələbələr (examId ilə saxlanılır)
    public List<Student> importStudentsFromExcel(MultipartFile file, Long examId) throws IOException {
        validateFile(file);
        List<Student> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() < 2) {
                throw new IllegalArgumentException("Excel faylında Sheet2 (tələbələr) tapılmadı!");
            }
            Sheet sheet = workbook.getSheetAt(1);
            int rowNumber = 0;
            for (Row row : sheet) {
                if (rowNumber == 0) { rowNumber++; continue; }
                if (isRowEmpty(row)) { rowNumber++; continue; }
                try {
                    String groupName = getCellStringValue(row.getCell(0));
                    String fullName = getCellStringValue(row.getCell(1));
                    String password = getCellStringValue(row.getCell(2));

                    if (groupName == null || groupName.trim().isEmpty()) { rowNumber++; continue; }
                    if (fullName == null || fullName.trim().isEmpty()) { rowNumber++; continue; }
                    if (password == null || password.trim().isEmpty()) { rowNumber++; continue; }

                    Student student = Student.builder()
                            .fullName(fullName.trim())
                            .groupName(groupName.trim())
                            .password(passwordEncoder.encode(password.trim()))
                            .qrToken(UUID.randomUUID().toString())
                            .examId(examId)  // ← imtahana bağlı
                            .build();
                    students.add(student);
                    log.info("✅ Tələbə: {} / {}", fullName.trim(), groupName.trim());
                } catch (Exception e) {
                    log.error("Tələbə sırası {} xəta: {}", rowNumber + 1, e.getMessage());
                }
                rowNumber++;
            }
        }

        if (students.isEmpty()) throw new IllegalArgumentException("Sheet2-də tələbə tapılmadı!");
        List<Student> saved = studentRepository.saveAll(students);
        log.info("✅ {} tələbə saxlandı", saved.size());
        return saved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Fayl boşdur!");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx"))
            throw new IllegalArgumentException("Yalnız .xlsx formatı qəbul edilir!");
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> null;
        };
    }

    private Integer getCellIntValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING  -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }
}