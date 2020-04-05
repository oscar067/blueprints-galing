package ocp.bluprints.executor;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import ocp.bluprints.echo.Event;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

public class GatlingKafkaConsumer {

    private static Persistence persistence;

    static {
        try {
            persistence = new Persistence();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Consumer<String, Event> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", "http://0.0.0.0:8081");
        props.setProperty("specific.avro.reader", "true");
        // Create the consumer using props.
        final Consumer<String, Event> consumer = new KafkaConsumer<String, Event>(props);
        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList("demoGatlingTopic"));
        consumer.poll(Duration.ZERO);
        consumer.commitSync();
        return consumer;

    }

    static void runConsumer() throws InterruptedException {
        final Consumer<String, Event> consumer = createConsumer();
        final int giveUp = 100;
        int noRecordsCount = 0;
        while (true) {
            final ConsumerRecords<String, Event> consumerRecords =
                    consumer.poll(Duration.of(500, ChronoUnit.MILLIS));
            consumerRecords.forEach(record -> {
                System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                        record.key(), record.value(),
                        record.partition(), record.offset());
                Event theEvent = record.value();

                persistence.insertElement(theEvent);


            });

            consumer.commitSync();

        }

    }
}
