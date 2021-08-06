package io.confluent.developer.datagen;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import static java.util.Arrays.asList;

@SpringBootApplication
public class WordcountApplication {

  public static void main(String[] args) {
    SpringApplication.run(WordcountApplication.class, args);
  }

  @Bean
  public Function<KStream<String, String>, KStream<String, Long>> processWords() {
    return inputStream -> {
      final Serde<String> stringSerde = Serdes.String();
      final KStream<String, Long> countsStream = inputStream
          .flatMapValues(value -> asList(value.toLowerCase().split("\\W+")))
          //.filter((key, value) -> value.length() > 3)
          .map((key, value) -> new KeyValue<>(value, value))
          .groupByKey(Grouped.with(stringSerde, stringSerde))
          .count(Materialized.as("word-count-state-store"))
          .toStream();
      countsStream.to("counts", Produced.with(stringSerde, Serdes.Long()));
      return countsStream;
    };
  }
}

@RestController
@RequiredArgsConstructor
class IQRestController {

  private final InteractiveQueryService iqService;

  @GetMapping("/iq/count/{word}")
  public Long getCount(@PathVariable final String word) {
    final ReadOnlyKeyValueStore<String, Long> store =
        iqService.getQueryableStore("word-count-state-store", QueryableStoreTypes.keyValueStore());
    return store.get(word);
  }
}


