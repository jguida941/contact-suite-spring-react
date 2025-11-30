package contactapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ensures {@link JacksonConfig} disables string coercion so schema violations surface as 400s.
 */
class JacksonConfigTest {

    private JacksonConfig config;

    @BeforeEach
    void setUp() {
        config = new JacksonConfig();
    }

    @Test
    void objectMapperRejectsBooleanCoercion() throws Exception {
        ObjectMapper mapper = config.objectMapper(new Jackson2ObjectMapperBuilder());
        assertThat(mapper).isNotNull();

        String payload = """
                {"value": false}
                """;

        assertThatThrownBy(() -> mapper.readValue(payload, SamplePayload.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void objectMapperRejectsNumericCoercion() throws Exception {
        ObjectMapper mapper = config.objectMapper(new Jackson2ObjectMapperBuilder());
        String payload = """
                {"value": 42}
                """;

        assertThatThrownBy(() -> mapper.readValue(payload, SamplePayload.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    private static final class SamplePayload {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
