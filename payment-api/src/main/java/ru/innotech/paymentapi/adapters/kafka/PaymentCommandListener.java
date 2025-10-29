package ru.innotech.paymentapi.adapters.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.innotech.paymentapi.adapters.repository.PaymentRepository;
import ru.innotech.paymentapi.core.model.Payment;
import ru.innotech.paymentapi.core.model.shared.MessageEnvelope;
import ru.innotech.paymentapi.core.model.shared.PaymentAuthorizedEventPayload;
import ru.innotech.paymentapi.core.model.shared.PaymentCancelledEventPayload;
import ru.innotech.paymentapi.core.model.shared.PaymentFailedEventPayload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandListener {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOutbox transactionOutbox;

    @Value("${topics.payment.replies}")
    private String paymentTopic;

    @KafkaListener(topics = "${topics.order.topic}")
    @Transactional
    public void onCommands(String raw) throws JsonProcessingException {
        log.info("Message {} was received", raw);
        JsonNode node = mapper.readTree(raw);
        String type = node.get("type").asText();
        JsonNode payload = node.get("payload");
        String orderId = payload.get("orderId").asText();
        BigDecimal amount = payload.get("amount").decimalValue();
        if ("OrderCreated".equals(type)) {
            try {
                var paymentCheck = checkPayment(amount);
                if (paymentCheck) {
                    handleSuccessPayment(orderId, amount);
                } else {
                    handleCanceledPayment(orderId);
                }

            } catch (Exception exception) {
                log.error("ex", exception);
                handleFailedPayment(orderId, exception.getMessage());
            }
        }
        if ("OrderCancelled".equals(type)) {
            Payment p = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
            p.setOrderId(orderId);
            p.setStatus("REFUND");
            paymentRepository.save(p);
        }
    }

    private void handleSuccessPayment(String orderId, BigDecimal amount) throws JsonProcessingException {
        Payment p = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        p.setOrderId(orderId);
        p.setAmount(amount);
        p.setStatus("AUTHORIZED");
        paymentRepository.save(p);

        MessageEnvelope<PaymentAuthorizedEventPayload> evt = MessageEnvelope.<PaymentAuthorizedEventPayload>builder()
                .messageId(UUID.randomUUID().toString())
                .orderId(orderId)
                .type("PaymentAuthorizedEvent")
                .timestamp(Instant.now())
                .payload(new PaymentAuthorizedEventPayload(orderId))
                .build();

        transactionOutbox.schedule(getClass()).sendEvent(orderId, evt);
    }

    private void handleCanceledPayment(String orderId) throws JsonProcessingException {
        Payment p = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        p.setOrderId(orderId);
        p.setStatus("CANCELLED");
        paymentRepository.save(p);

        MessageEnvelope<PaymentCancelledEventPayload> evt = MessageEnvelope.<PaymentCancelledEventPayload>builder()
                .messageId(UUID.randomUUID().toString())
                .orderId(orderId)
                .type("PaymentCancelledEvent")
                .timestamp(Instant.now())
                .payload(new PaymentCancelledEventPayload(orderId))
                .build();

        transactionOutbox.schedule(getClass()).sendEvent(orderId, evt);
    }

    private void handleFailedPayment(String orderId, String message) throws JsonProcessingException {
        Payment p = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        p.setOrderId(orderId);
        p.setStatus("FAILED");
        paymentRepository.save(p);

        MessageEnvelope<PaymentFailedEventPayload> evt = MessageEnvelope.<PaymentFailedEventPayload>builder()
                .messageId(UUID.randomUUID().toString())
                .orderId(orderId)
                .type("PaymentFailedEvent")
                .timestamp(Instant.now())
                .payload(new PaymentFailedEventPayload(orderId, message))
                .build();

        transactionOutbox.schedule(getClass()).sendEvent(orderId, evt);
    }

    <T> void sendEvent(String orderId, MessageEnvelope<T> event) throws JsonProcessingException {
        kafkaTemplate.send(paymentTopic, orderId, mapper.writeValueAsString(event));
        log.info("Message {} was sent", mapper.writeValueAsString(event));
    }

    private boolean checkPayment(BigDecimal amount) {
        //проверка на баланс и прочее
        return true;
    }
}
