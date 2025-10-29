package ru.innotech.paymentapi.core;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {


    private final TransactionOutbox outbox;

    @Scheduled(fixedRateString = "${outbox.repeatEvery}")
    void poll() {
        try {
            do {
                log.info("Flushing");
            } while (outbox.flush());
        } catch (Exception e) {
            log.error("Error flushing transaction outbox. Pausing", e);
        }
    }


}

