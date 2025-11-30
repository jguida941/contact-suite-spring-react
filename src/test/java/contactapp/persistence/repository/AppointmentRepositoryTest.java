package contactapp.persistence.repository;

import contactapp.persistence.entity.AppointmentEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for {@link AppointmentRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class AppointmentRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private AppointmentRepository repository;

    @Test
    void saveAndFindAppointment() {
        Instant instant = Instant.now().plusSeconds(86_400);
        AppointmentEntity entity = new AppointmentEntity("appt-101", instant, "Repo Appointment");

        repository.saveAndFlush(entity);

        assertThat(repository.findById("appt-101"))
                .isPresent()
                .get()
                .extracting(AppointmentEntity::getAppointmentDate)
                .isEqualTo(instant);
    }
}
