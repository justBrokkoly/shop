package ru.innotech.orderapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties("outbox")
@Data
class TransactionOutboxProperties {
  private Duration repeatEvery;
  private boolean useJackson;
  private Duration attemptFrequency;
  private int blockAfterAttempts;
}