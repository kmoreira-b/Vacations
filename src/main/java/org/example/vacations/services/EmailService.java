package org.example.vacations.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.grammars.hql.HqlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.example.vacations.models.Account;
import org.example.vacations.models.Employee;
import org.example.vacations.models.VacationRequest;

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

            //MUST set a valid From or SMTP rejects it
            helper.setFrom(fromAddress);

            // Override only for the initial vacation request (DL), not coverage
            if (to.equalsIgnoreCase("kmoreirab@ucenfotec.ac.cr")) {
                helper.setTo("kmoreirab@ucenfotec.ac.cr");
            } else {
                helper.setTo(to); // allow real emails sent to employee's addressess
            }


            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[EMAIL SENT] to override address: kmoreirab@ucenfotec.ac.cr (original: {})", to);
        } catch (Exception e) {
            log.warn("[EMAIL SKIPPED] {} ({})", to, e.getMessage());
            log.info("[EMAIL BODY]\n{}", html);
        }
    }

    public void sendCoverageEmails(VacationRequest req, Account account, Employee covering, String baseUrl) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startStr = fmt.format(req.getStartDate());
        String endStr   = fmt.format(req.getEndDate());
        long businessDays = countBusinessDays(req.getStartDate(), req.getEndDate());
        String link = (baseUrl == null ? "" : baseUrl) + "/request/" + req.getId();

        String fullAccountName = account.getName();
        String shortAccountName = fullAccountName.contains("-")
                ? fullAccountName.substring(fullAccountName.lastIndexOf("-") + 1).trim()
                : fullAccountName;

        // Email to requester
        String requesterHtml = """
        <p>Hello <b>%s</b>,</p>
        <p><b>%s</b> has been assigned to cover your account <b>%s</b> during your vacation from <b>%s</b> to <b>%s</b> for a total of <b>%d</b> business days.</p>
        <p>You can review the request here: <a href="%s">%s</a></p>
        """.formatted(
                req.getRequester().getName(),
                covering.getName(),
                shortAccountName,
                startStr, endStr, businessDays,
                link, link
        );

        sendHtml(
                req.getRequester().getEmail(),
                "Update - " + shortAccountName + " Covered",
                requesterHtml
        );

        // Email to covering employee
        String coveringHtml = """
        <p>Hi <b>%s</b>,</p>
        <p>You have been selected to cover for <b>%s</b> on account <b>%s</b> from <b>%s</b> to <b>%s</b> for a total of <b>%d</b> business days.</p>
        <p>You can review the request here: <a href="%s">%s</a></p>
        """.formatted(
                covering.getName(),
                req.getRequester().getName(),
                shortAccountName,
                startStr, endStr, businessDays,
                link, link
        );

        sendHtml(
                covering.getEmail(),
                "Vacation Coverage Assigned - " + shortAccountName,
                coveringHtml
        );
    }


    private long countBusinessDays(java.time.LocalDate start, java.time.LocalDate end) {
        long days = 0;
        for (var d = start; !d.isAfter(end); d = d.plusDays(1)) {
            var dow = d.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }


}


