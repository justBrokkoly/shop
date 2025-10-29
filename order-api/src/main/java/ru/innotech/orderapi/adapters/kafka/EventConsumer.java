package ru.innotech.orderapi.adapters.kafka;

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
import ru.innotech.orderapi.adapters.repository.OrderRepository;
import ru.innotech.orderapi.core.model.Order;
import ru.innotech.orderapi.core.model.shared.MessageEnvelope;
import ru.innotech.orderapi.core.model.shared.OrderEvent;
import ru.innotech.orderapi.core.model.shared.ReserveStockCommandPayload;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOutbox transactionOutbox;

    @Value("${topics.order.topic}")
    private String orderTopic;

    @Transactional
    @KafkaListener(topics = "${topics.payment.topic}")
    public void consumePaymentEvent(String raw) throws IOException {
        log.info("Message {} was received", raw);
        JsonNode node = mapper.readTree(raw);
        String type = node.get("type").asText();
        JsonNode payload = node.get("payload");
        String orderId = payload.get("orderId").asText();
        if ("PaymentAuthorizedEvent".equals(type)) {
            Order order = orderRepository.findByOrderId(orderId);
            order.setStatus("PaymentProcessed");

            MessageEnvelope<ReserveStockCommandPayload> evt = MessageEnvelope.<ReserveStockCommandPayload>builder()
                    .messageId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .type("ReserveStock")
                    .timestamp(Instant.now())
                    .payload(new ReserveStockCommandPayload(orderId, order.getItems()))
                    .build();
            transactionOutbox.schedule(getClass()).sendEvent(orderId, evt);
        }
        if ("PaymentCancelledEvent".equals(type)) {
            Order order = orderRepository.findByOrderId(orderId);
            order.setStatus("PaymentCancelled");
        }
        if ("PaymentFailedEvent".equals(type)) {
            Order order = orderRepository.findByOrderId(orderId);
            order.setStatus("PaymentFailed");
        }
    }


    @Transactional
    @KafkaListener(topics = "${topics.inventory.topic}")
    public void consumeInventoryEvent(String raw) throws IOException {
        log.info("Message {} was received", raw);
        JsonNode node = mapper.readTree(raw);
        String type = node.get("type").asText();
        JsonNode payload = node.get("payload");
        String orderId = payload.get("orderId").asText();
        if ("StockReservedEvent".equals(type)) {
            Order order = orderRepository.findByOrderId(orderId);
            order.setStatus("OrderCompleted");
        }
        if ("StockReservationFailedEvent".equals(type)) {
            Order order = orderRepository.findByOrderId(orderId);
            order.setStatus("StockReservationFailed");
            MessageEnvelope<OrderEvent> evt = MessageEnvelope.<OrderEvent>builder()
                    .messageId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .type("OrderCancelled")
                    .timestamp(Instant.now())
                    .payload(new OrderEvent(orderId, "OrderCancelled"))
                    .build();
            transactionOutbox.schedule(getClass()).sendEvent(orderId, evt);
        }
    }

    <T> void sendEvent(String orderId,MessageEnvelope<T> event ) throws IOException {
        kafkaTemplate.send(orderTopic, orderId, mapper.writeValueAsString(event));
        log.info("Message {} was sent", mapper.writeValueAsString(event));
    }
}
