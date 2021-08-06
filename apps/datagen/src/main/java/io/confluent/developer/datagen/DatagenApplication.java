package io.confluent.developer.datagen;

import com.github.javafaker.Faker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootApplication
public class DatagenApplication {

  public static void main(String[] args) {
    SpringApplication.run(DatagenApplication.class, args);
  }

  @Bean
  Supplier<Message<String>> produceData() {
    return () ->
        MessageBuilder
            .withPayload(Faker.instance().backToTheFuture().quote())
            .build();
  }

  @Bean
  Consumer<Message<String>> consumeData() {
    return s ->
        //System.out.println("FACT: \u001B[3m «" + s.getPayload() + "\u001B[0m»");
        System.out.println("\u001B[3m «" + s.getPayload() + "\u001B[0m»");
  }
}


