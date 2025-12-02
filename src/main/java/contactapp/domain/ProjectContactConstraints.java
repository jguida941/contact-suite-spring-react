package contactapp.domain;

/**
 * Shared constraints for project/contact linkage features.
 */
public final class ProjectContactConstraints {

    private ProjectContactConstraints() {
        // utility class
    }

    /**
     * Maximum length for the optional role description when associating a contact to a project.
     */
    public static final int MAX_ROLE_LENGTH = 50;
}
