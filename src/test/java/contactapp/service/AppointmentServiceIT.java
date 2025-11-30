package contactapp.service;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import contactapp.persistence.repository.AppointmentRepository;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack AppointmentService tests against Postgres (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class AppointmentServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @Transactional
    void addAppointmentPersistsRecord() {
        Appointment appointment = new Appointment(
                "it-appt",
                Date.from(Instant.now().plusSeconds(86_400)),
                "Integration appointment"
        );

        boolean added = appointmentService.addAppointment(appointment);

        assertThat(added).isTrue();
        assertThat(appointmentRepository.findById("it-appt")).isPresent();
    }

    @Test
    @Transactional
    void databaseRejectsNullDescription() {
        AppointmentEntity entity = new AppointmentEntity(
                "it-appt-null",
                Instant.now().plusSeconds(86_400),
                null
        );

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(entity))
                .as("description column is NOT NULL")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
