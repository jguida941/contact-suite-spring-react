package contactapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration to enforce strict type checking.
 *
 * <p>By default, Jackson coerces non-string types to strings (e.g., {@code false} → {@code "false"}).
 * This configuration disables that behavior to ensure API requests match the OpenAPI schema exactly.
 *
 * <h2>Why This Matters</h2>
 * <p>Without this configuration:
 * <ul>
 *   <li>{@code {"address": false}} would be accepted as {@code {"address": "false"}}</li>
 *   <li>{@code {"description": 123}} would be accepted as {@code {"description": "123"}}</li>
 * </ul>
 *
 * <p>This violates the OpenAPI contract which declares these fields as strings.
 * API fuzzing tools like Schemathesis catch this as "API accepted schema-violating request".
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/3013">Jackson coercion config</a>
 */
@Configuration
public class JacksonConfig {

    /**
     * Configures Jackson to reject type coercion for string fields.
     *
     * <p>This ensures that boolean, integer, and float values are not silently
     * converted to strings, enforcing strict schema compliance.
     *
     * @param builder the Spring-provided ObjectMapper builder
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper(final Jackson2ObjectMapperBuilder builder) {
        final ObjectMapper mapper = builder.build();

        // Disable coercion of boolean to String
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

        // Disable coercion of integer to String
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);

        // Disable coercion of float to String
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);

        return mapper;
    }
}
