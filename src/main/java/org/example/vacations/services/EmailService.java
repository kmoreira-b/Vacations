package org.example.vacations.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.grammars.hql.HqlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Use your authenticated SMTP user as the From
    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ✅ MUST set a valid From or SMTP rejects it
            helper.setFrom(fromAddress);

            // 🔒 Force all mail to one recipient (your override)
            helper.setTo("kmoreirab@ucenfotec.ac.cr");

            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[EMAIL SENT] to override address: kmoreirab@ucenfotec.ac.cr (original: {})", to);
        } catch (Exception e) {
            log.warn("[EMAIL SKIPPED] {} ({})", to, e.getMessage());
            log.info("[EMAIL BODY]\n{}", html);
        }
    }
}


