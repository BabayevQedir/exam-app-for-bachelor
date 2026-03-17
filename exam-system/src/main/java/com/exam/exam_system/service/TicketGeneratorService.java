package com.exam.exam_system.service;

import com.exam.exam_system.model.*;
import com.exam.exam_system.repository.*;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Bilet generasiya servisi
 *
 * Axış:
 * 1. İmtahana aid tələbələri tap
 * 2. Hər tələbəyə random suallar seç
 * 3. Bilet yarat (DB-yə saxla)
 * 4. QR kod generasiya et
 * 5. Word faylı yarat
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketGeneratorService {

    private final TicketRepository ticketRepository;
    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;
    private final WordExportService wordExportService;
    private final ExamRepository examRepository;

    public List<Ticket> generateAllTickets(Long examId) throws IOException, WriterException {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("İmtahan tapılmadı"));

        // Yalnız bu imtahana aid tələbələri al
        List<Student> students = studentRepository.findByExamId(examId);
        if (students.isEmpty()) {
            throw new RuntimeException("Bu imtahana aid tələbə tapılmadı. Excel-dən tələbə yüklənibmi?");
        }

        List<Question> allQuestions = questionRepository.findByExamId(examId);
        if (allQuestions.size() < exam.getQuestionsPerTicket()) {
            throw new RuntimeException(
                    String.format("Sual sayı az: %d sual var, hər biletdə %d lazımdır",
                            allQuestions.size(), exam.getQuestionsPerTicket()));
        }

        List<Ticket> generatedTickets = new ArrayList<>();
        int ticketNumber = 1;

        for (Student student : students) {
            if (ticketRepository.existsByExamIdAndStudentId(examId, student.getId())) {
                log.warn("Tələbə {} üçün bilet artıq var — atlandı", student.getFullName());
                continue;
            }

            List<Question> selectedQuestions = selectRandomQuestions(allQuestions, exam.getQuestionsPerTicket());
            int totalPoints = selectedQuestions.stream().mapToInt(Question::getPoints).sum();

            Ticket ticket = new Ticket();
            ticket.setTicketNumber(ticketNumber++);
            ticket.setExam(exam);
            ticket.setStudent(student);
            ticket.setQuestions(selectedQuestions);
            ticket.setQrToken(student.getQrToken());
            ticket.setTotalPoints(totalPoints);

            Ticket savedTicket = ticketRepository.save(ticket);
            String wordPath = wordExportService.generateTicketWord(savedTicket);
            savedTicket.setWordFilePath(wordPath);
            ticketRepository.save(savedTicket);
            generatedTickets.add(savedTicket);
            log.info("✅ Bilet {} → {}", ticketNumber - 1, student.getFullName());
        }

        log.info("✅ Cəmi {} bilet yaradıldı", generatedTickets.size());
        return generatedTickets;
    }

    private List<Question> selectRandomQuestions(List<Question> questions, int count) {
        List<Question> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}