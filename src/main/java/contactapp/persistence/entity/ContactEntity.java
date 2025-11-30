package contactapp.persistence.entity;

import contactapp.domain.Validation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mirroring the {@link contactapp.domain.Contact} structure.
 *
 * <p>Domain objects remain final/value-focused while this mutable entity exists purely
 * for persistence. Column lengths mirror {@link Validation} constants so the database
 * schema always matches domain constraints.
 */
@Entity
@Table(name = "contacts")
public class ContactEntity {

    @Id
    @Column(name = "contact_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String contactId;

    @Column(name = "first_name", length = Validation.MAX_NAME_LENGTH, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = Validation.MAX_NAME_LENGTH, nullable = false)
    private String lastName;

    @Column(name = "phone", length = Validation.PHONE_LENGTH, nullable = false)
    private String phone;

    @Column(name = "address", length = Validation.MAX_ADDRESS_LENGTH, nullable = false)
    private String address;

    /** Protected no-arg constructor required by JPA. */
    protected ContactEntity() {
        // JPA only
    }

    public ContactEntity(
            final String contactId,
            final String firstName,
            final String lastName,
            final String phone,
            final String address) {
        this.contactId = contactId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
    }

    public String getContactId() {
        return contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }
}
