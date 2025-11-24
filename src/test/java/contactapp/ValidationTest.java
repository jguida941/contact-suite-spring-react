package contactapp;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused tests for {@link Validation} helpers to ensure PIT sees
 * their behavior when blank checks or boundary logic are mutated.
 */
class ValidationTest {

    private static final int MIN_LENGTH = 1;
    private static final int NAME_MAX = 10;
    private static final int ADDRESS_MAX = 30;
    private static final int PHONE_LENGTH = 10;

    /**
     * Ensures the helper allows inputs exactly at the configured min/max boundaries.
     */
    @Test
    void validateLengthAcceptsBoundaryValues() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("A", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJ", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("123456789012345678901234567890", "address", MIN_LENGTH, ADDRESS_MAX));
    }

    /**
     * Blank inputs must fail length validation for any label.
     */
    @Test
    void validateLengthRejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateLength("   ", "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    /**
     * Null inputs should fail immediately before trim/length checks.
     */
    @Test
    void validateLengthRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateLength(null, "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    /**
     * Covers the "too long" branch of {@link Validation#validateLength(String, String, int, int)}.
     */
    @Test
    void validateLengthRejectsTooLong() {
        assertThatThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJK", "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName length must be between 1 and 10");
    }

    /**
     * Covers the "too short" branch of {@link Validation#validateLength(String, String, int, int)}.
     */
    @Test
    void validateLengthRejectsTooShort() {
        assertThatThrownBy(() ->
                Validation.validateLength("A", "middleName", /*minLength*/ 2, /*maxLength*/ 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("middleName length must be between 2 and 5");
    }

    /**
     * Phone numbers must fail fast when blank.
     */
    @Test
    void validateNumeric10RejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateNumeric10("          ", "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }

    /**
     * Null phones must trigger the same blank check as blanks.
     */
    @Test
    void validateNumeric10RejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateNumeric10(null, "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }

    /**
     * Confirms future dates pass {@link Validation#validateDateNotPast(Date, String)}.
     */
    @Test
    void validateDateNotPastAcceptsFutureDate() {
        final Date future = Date.from(Instant.now().plus(Duration.ofHours(1)));
        assertThatNoException().isThrownBy(() ->
                Validation.validateDateNotPast(future, "appointmentDate"));
    }

    /**
     * Null dates must throw an explicit error.
     */
    @Test
    void validateDateNotPastRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateDateNotPast(null, "appointmentDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentDate must not be null");
    }

    /**
     * Past dates must be rejected so appointments cannot be scheduled retroactively.
     */
    @Test
    void validateDateNotPastRejectsPastDate() {
        final Date past = Date.from(Instant.now().minus(Duration.ofHours(1)));
        assertThatThrownBy(() ->
                Validation.validateDateNotPast(past, "appointmentDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentDate must not be in the past");
    }
}
