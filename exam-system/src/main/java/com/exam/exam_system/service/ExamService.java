package com.exam.exam_system.service;

import com.exam.exam_system.model.Exam;
import com.exam.exam_system.model.Question;
import com.exam.exam_system.model.Student;
import com.exam.exam_system.model.Ticket;
import com.exam.exam_system.repository.ExamRepository;
import com.exam.exam_system.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExcelImportService excelImportService;
    private final TicketGeneratorService ticketGeneratorService;

    public ExamService(ExamRepository examRepository,
                       QuestionRepository questionRepository,
                       ExcelImportService excelImportService,
                       @Lazy TicketGeneratorService ticketGeneratorService) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.excelImportService = excelImportService;
        this.ticketGeneratorService = ticketGeneratorService;
    }

    public Exam createExamWithQuestions(String examName, String subject,
                                        Integer ticketCount, Integer questionsPerTicket,
                                        Integer durationMinutes, String examType,
                                        MultipartFile excelFile) throws IOException {

        if (ticketCount <= 0) throw new IllegalArgumentException("Bilet sayı 0-dan böyük olmalıdır");
        if (questionsPerTicket <= 0) throw new IllegalArgumentException("Sual sayı 0-dan böyük olmalıdır");
        if (durationMinutes <= 0) throw new IllegalArgumentException("Müddət 0-dan böyük olmalıdır");

        Exam.ExamType type;
        try { type = Exam.ExamType.valueOf(examType.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Növ COMPUTER və ya PAPER olmalıdır"); }

        Exam exam = Exam.builder()
                .examName(examName).subject(subject)
                .ticketCount(ticketCount).questionsPerTicket(questionsPerTicket)
                .durationMinutes(durationMinutes).examType(type)
                .status(Exam.ExamStatus.DRAFT)
                .build();

        Exam savedExam = examRepository.save(exam);

        // Sheet1 — suallar
        List<Question> questions = excelImportService.importQuestionsFromExcel(excelFile, savedExam);
        log.info("✅ {} sual yükləndi", questions.size());

        // Sheet2 — tələbələr
        List<Student> students = excelImportService.importStudentsFromExcel(excelFile, savedExam.getId());
        log.info("✅ {} tələbə yükləndi", students.size());

        // PAPER imtahan üçün biletlər avtomatik yaranır
        if (type == Exam.ExamType.PAPER) {
            try {
                List<Ticket> tickets = ticketGeneratorService.generateAllTickets(savedExam.getId());
                log.info("✅ {} bilet avtomatik yaradıldı", tickets.size());
                // Statusu ACTIVE et
                savedExam.setStatus(Exam.ExamStatus.ACTIVE);
                examRepository.save(savedExam);
            } catch (Exception e) {
                log.error("Bilet yaratmada xəta: {}", e.getMessage());
            }
        }

        return savedExam;
    }

    public Exam activateExam(Long examId) {
        Exam exam = getExamById(examId);
        exam.setStatus(Exam.ExamStatus.ACTIVE);
        return examRepository.save(exam);
    }

    public Exam finishExam(Long examId) {
        Exam exam = getExamById(examId);
        exam.setStatus(Exam.ExamStatus.FINISHED);
        return examRepository.save(exam);
    }

    public Exam changeStatus(Long examId, String status) {
        Exam exam = getExamById(examId);
        exam.setStatus(Exam.ExamStatus.valueOf(status.toUpperCase()));
        return examRepository.save(exam);
    }

    public Exam saveExam(Exam exam) { return examRepository.save(exam); }

    public List<Exam> getAllExams() { return examRepository.findAll(); }

    public List<Exam> getActiveExams() { return examRepository.findByStatus(Exam.ExamStatus.ACTIVE); }

    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İmtahan tapılmadı: " + id));
    }
}