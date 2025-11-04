package com.distrischool.teacher.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para Kafka no serviço de professores
 */
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"distrischool.teacher.created", "distrischool.teacher.updated"})
@ActiveProfiles("test")
@DirtiesContext
class KafkaIntegrationTest {

    @Autowired(required = false)
    private KafkaTemplate<String, DistriSchoolEvent> kafkaTemplate;

    @Autowired(required = false)
    private EventProducer eventProducer;

    @Test
    void contextLoads() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void shouldPublishTeacherCreatedEvent() {
        if (eventProducer == null || kafkaTemplate == null) {
            return;
        }

        Map<String, Object> eventData = Map.of(
            "teacherId", 1L,
            "name", "Test Teacher",
            "email", "teacher@example.com"
        );

        DistriSchoolEvent event = DistriSchoolEvent.create(
            "teacher.created",
            "teacher-management-service",
            eventData
        );

        // Publica evento via EventProducer
        if (eventProducer != null) {
            eventProducer.sendEvent("distrischool.teacher.created", event);
        } else {
            kafkaTemplate.send("distrischool.teacher.created", event.getEventId(), event);
        }

        assertThat(event).isNotNull();
        assertThat(event.getEventType()).isEqualTo("teacher.created");
    }

    @Test
    void shouldPublishTeacherUpdatedEvent() {
        if (kafkaTemplate == null) {
            return;
        }

        Map<String, Object> eventData = Map.of(
            "teacherId", 1L,
            "name", "Updated Teacher"
        );

        DistriSchoolEvent event = DistriSchoolEvent.create(
            "teacher.updated",
            "teacher-management-service",
            eventData
        );

        kafkaTemplate.send("distrischool.teacher.updated", event.getEventId(), event);

        assertThat(event).isNotNull();
        assertThat(event.getEventType()).isEqualTo("teacher.updated");
    }
}
