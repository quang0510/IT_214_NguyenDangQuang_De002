package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendBookingCreatedEmail(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Email người nhận không được để trống");
        }
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Chưa cấu hình MAIL_FROM hoặc MAIL_USERNAME");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.trim());
        message.setSubject("Đặt vé xem phim thành công");
        message.setText("Xin chào,\n\nYêu cầu đặt vé xem phim của bạn đã được hệ thống tiếp nhận thành công.\n\nTrân trọng.");

        mailSender.send(message);
    }
}
