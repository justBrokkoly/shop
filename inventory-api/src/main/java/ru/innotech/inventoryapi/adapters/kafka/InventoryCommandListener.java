package ru.innotech.inventoryapi.adapters.kafka;

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
import ru.innotech.inventoryapi.adapters.repository.ReservationRepository;
import ru.innotech.inventoryapi.core.model.Reservation;
import ru.innotech.inventoryapi.core.model.shared.MessageEnvelope;
import ru.innotech.inventoryapi.core.model.shared.StockReservationFailedEventPayload;
import ru.innotech.inventoryapi.core.model.shared.StockReservedEventPayload;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandListener {

    private final ReservationRepository reservationRepository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOutbox transactionOutbox;

    @Value("${topics.order.input}")
    private String orderTopic;
    @Value("${topics.inventory.input}")
    private String inventoryTopic;


    @Transactional
    @KafkaListener(topics = "${topics.inventory.commands}")
    public void onCommands(String raw) throws JsonProcessingException {
        JsonNode node = mapper.readTree(raw);
        String type = node.get("type").asText();
        if (!"ReserveStock".equals(type)) return;

        String orderId = node.get("payload").get("orderId").asText();
        try {
            Reservation r = reservationRepository.findByOrderId(orderId).orElseGet(Reservation::new);
            r.setOrderId(orderId);
            r.setStatus("RESERVED");
            reservationRepository.save(r);

            MessageEnvelope<StockReservedEventPayload> evt = MessageEnvelope.<StockReservedEventPayload>builder()
                    .messageId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .type("StockReservedEvent")
                    .timestamp(Instant.now())
                    .payload(new StockReservedEventPayload(orderId))
                    .build();
            transactionOutbox.schedule(getClass()).sendEvent(orderTopic, orderId, evt);
        } catch (Exception exception) {
            Reservation r = reservationRepository.findByOrderId(orderId).orElseGet(Reservation::new);
            r.setOrderId(orderId);
            r.setStatus("FAILED");
            reservationRepository.save(r);
            MessageEnvelope<StockReservationFailedEventPayload> evt =
                    MessageEnvelope.<StockReservationFailedEventPayload>builder()
                    .messageId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .type("StockReservationFailedEvent")
                    .timestamp(Instant.now())
                    .payload(new StockReservationFailedEventPayload(orderId, exception.getMessage()))
                    .build();

            transactionOutbox.schedule(getClass()).sendEvent(inventoryTopic, orderId, evt);
        }
    }

    <T> void sendEvent(String topic, String orderId, MessageEnvelope<T> event) throws JsonProcessingException {
        kafkaTemplate.send(topic, orderId, mapper.writeValueAsString(event));
    }
}
