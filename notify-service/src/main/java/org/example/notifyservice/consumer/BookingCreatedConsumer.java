package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BookingCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingCreatedConsumer.class);

    private final EmailService emailService;

    public BookingCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.booking-created-topic}")
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("Bỏ qua sự kiện booking có email rỗng");
            return;
        }
        log.info("Nhận sự kiện tạo booking thành công cho email: {}", email);
        emailService.sendBookingCreatedEmail(email);
        log.info("Đã gửi email xác nhận tạo đơn hàng tới: {}", email);
    }
}
