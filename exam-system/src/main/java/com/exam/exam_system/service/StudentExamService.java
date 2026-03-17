package com.exam.exam_system.service;

import com.exam.exam_system.model.*;
import com.exam.exam_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentExamService {

    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final StudentExamSessionRepository sessionRepository;
    private final StudentAnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;

    // =============================================
    // YALNIZ PAROL İLƏ İMTAHANA DAXİL OL
    // =============================================
    public StudentExamSession startExam(Long examId, String password) {

        // İmtahanı tap
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("İmtahan tapılmadı"));

        if (exam.getStatus() != Exam.ExamStatus.ACTIVE) {
            throw new IllegalStateException("Bu imtahan aktiv deyil");
        }

        // Bütün tələbələr arasında parolu uyğun olanı tap
        List<Student> allStudents = studentRepository.findAll();
        Student student = allStudents.stream()
                .filter(s -> passwordEncoder.matches(password, s.getPassword()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parol yanlışdır"));

        // Artıq sessiya varsa yoxla
        sessionRepository.findByStudentIdAndExamId(student.getId(), examId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == StudentExamSession.SessionStatus.IN_PROGRESS) {
                        throw new IllegalStateException("Siz artıq bu imtahanda iştirak edirsiniz");
                    }
                    throw new IllegalStateException("Siz bu imtahanda artıq iştirak etmisiniz");
                });

        // Sessiya yarat
        StudentExamSession session = StudentExamSession.builder()
                .student(student)
                .exam(exam)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusMinutes(exam.getDurationMinutes()))
                .status(StudentExamSession.SessionStatus.IN_PROGRESS)
                .build();

        StudentExamSession saved = sessionRepository.save(session);
        log.info("✅ Tələbə {} imtahana daxil oldu", student.getFullName());
        return saved;
    }

    // =============================================
    // RANDOM SUALLAR AL
    // =============================================
    public List<Question> getRandomQuestions(Long sessionId) {
        StudentExamSession session = getSession(sessionId);
        checkSessionActive(session);

        List<Question> allQuestions = questionRepository.findByExamId(session.getExam().getId());
        Collections.shuffle(allQuestions);
        int count = Math.min(session.getExam().getQuestionsPerTicket(), allQuestions.size());
        return allQuestions.subList(0, count);
    }

    // =============================================
    // MƏTN CAVAB SAXLA
    // =============================================
    public StudentAnswer saveTextAnswer(Long sessionId, Long questionId, String answerText) {
        StudentExamSession session = getSession(sessionId);
        checkSessionActive(session);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Sual tapılmadı"));

        StudentAnswer answer = answerRepository
                .findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(StudentAnswer.builder()
                        .session(session)
                        .question(question)
                        .build());

        answer.setAnswerText(answerText);
        answer.setAnsweredAt(LocalDateTime.now());
        return answerRepository.save(answer);
    }

    // =============================================
    // FAYL CAVAB SAXLA
    // =============================================
    public StudentAnswer saveFileAnswer(Long sessionId, Long questionId,
                                        MultipartFile file) throws IOException {
        StudentExamSession session = getSession(sessionId);
        checkSessionActive(session);

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Yalnız .zip formatı qəbul edilir!");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Sual tapılmadı"));

        String uploadDir = "./student-answers/" + sessionId + "/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String savedFileName = questionId + "_" + UUID.randomUUID() + ".zip";
        Path filePath = uploadPath.resolve(savedFileName);
        Files.write(filePath, file.getBytes());

        StudentAnswer answer = answerRepository
                .findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(StudentAnswer.builder()
                        .session(session)
                        .question(question)
                        .build());

        answer.setAttachedFilePath(filePath.toString());
        answer.setOriginalFileName(filename);
        answer.setAnsweredAt(LocalDateTime.now());
        return answerRepository.save(answer);
    }

    // =============================================
    // SUBMIT ET
    // =============================================
    public StudentExamSession submitExam(Long sessionId) {
        StudentExamSession session = getSession(sessionId);
        if (session.getStatus() != StudentExamSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Bu sessiya artıq bitib");
        }
        session.setStatus(StudentExamSession.SessionStatus.SUBMITTED);
        session.setSubmittedAt(LocalDateTime.now());
        log.info("✅ Tələbə {} imtahanı submit etdi", session.getStudent().getFullName());
        return sessionRepository.save(session);
    }

    // =============================================
    // Köməkçi metodlar
    // =============================================
    private StudentExamSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sessiya tapılmadı"));
    }

    private void checkSessionActive(StudentExamSession session) {
        if (session.getStatus() != StudentExamSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Bu sessiya artıq aktiv deyil");
        }
        if (LocalDateTime.now().isAfter(session.getEndTime())) {
            session.setStatus(StudentExamSession.SessionStatus.TIMEOUT);
            session.setSubmittedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new IllegalStateException("İmtahan vaxtı bitib");
        }
    }
}