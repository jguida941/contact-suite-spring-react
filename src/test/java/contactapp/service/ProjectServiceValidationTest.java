package contactapp.service;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.persistence.store.InMemoryProjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-killing tests for ProjectService validation guards.
 *
 * <p>These tests target VoidMethodCallMutator and boundary condition mutations
 * that survive in the H2/in-memory code path. Uses InMemoryProjectStore to ensure
 * tests run on both Windows and Linux CI environments without Testcontainers.
 *
 * <p><b>NOT tagged with "legacy-singleton"</b> - these tests run in the main pipeline.
 */
class ProjectServiceValidationTest {

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(new InMemoryProjectStore());
        service.clearAllProjects();
    }

    // ==================== Mutation Testing: Validation Guards ====================

    /**
     * Kills VoidMethodCallMutator on getProjectById validation (line 373).
     * Ensures getProjectById throws IllegalArgumentException for null projectId, not NPE.
     */
    @Test
    void testGetProjectByIdNullIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getProjectById(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on getProjectById validation.
     * Ensures whitespace-only string is rejected.
     */
    @Test
    void testGetProjectByIdWhitespaceOnlyIdThrows() {
        assertThatThrownBy(() -> service.getProjectById("  \t  "))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on addContactToProject validation (line 435).
     * Ensures projectId parameter is validated.
     */
    @Test
    void testAddContactToProjectNullProjectIdThrows() {
        assertThatThrownBy(() -> service.addContactToProject(null, "contact1", "CLIENT"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on addContactToProject validation (line 436).
     * Ensures contactId validation is not skipped.
     */
    @Test
    void testAddContactToProjectNullContactIdThrows() {
        // First create a project
        Project project = new Project("pvm-01", "Project", "Desc", ProjectStatus.ACTIVE);
        service.addProject(project);

        assertThatThrownBy(() -> service.addContactToProject("pvm-01", null, "CLIENT"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on getProjectContacts validation (line 524).
     * Ensures projectId validation throws correct exception.
     */
    @Test
    void testGetProjectContactsNullIdThrows() {
        assertThatThrownBy(() -> service.getProjectContacts(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on getContactProjects validation (line 556).
     * Ensures contactId validation throws correct exception.
     */
    @Test
    void testGetContactProjectsNullIdThrows() {
        assertThatThrownBy(() -> service.getContactProjects(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on clearAllProjects (line 414).
     * Verifies that clearAllProjects actually removes data from the store,
     * not just returns without calling store.deleteAll().
     */
    @Test
    void testClearAllProjectsActuallyDeletesData() {
        // Add multiple projects
        service.addProject(new Project("clear-p1", "Project 1", "Desc 1", ProjectStatus.ACTIVE));
        service.addProject(new Project("clear-p2", "Project 2", "Desc 2", ProjectStatus.ON_HOLD));
        service.addProject(new Project("clear-p3", "Project 3", "Desc 3", ProjectStatus.COMPLETED));

        // Verify projects exist
        assertThat(service.getAllProjects()).hasSize(3);

        // Clear all projects
        service.clearAllProjects();

        // Verify all projects are gone - this kills the mutation that removes store.deleteAll()
        assertThat(service.getAllProjects()).isEmpty();

        // Also verify getProjectById returns empty for previously existing projects
        assertThat(service.getProjectById("clear-p1")).isEmpty();
        assertThat(service.getProjectById("clear-p2")).isEmpty();
        assertThat(service.getProjectById("clear-p3")).isEmpty();
    }

    // ==================== Mutation Testing: Boundary Conditions ====================

    /**
     * Boundary test: whitespace-only projectId should be rejected.
     */
    @Test
    void testAddContactToProjectWhitespaceProjectIdThrows() {
        assertThatThrownBy(() -> service.addContactToProject("\t\n", "contact1", "CLIENT"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Boundary test: whitespace-only contactId should be rejected.
     */
    @Test
    void testAddContactToProjectWhitespaceContactIdThrows() {
        Project project = new Project("pvm-02", "Project", "Desc", ProjectStatus.ACTIVE);
        service.addProject(project);

        assertThatThrownBy(() -> service.addContactToProject("pvm-02", "   ", "CLIENT"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    // ==================== RemoveContactFromProject Validation ====================

    /**
     * Kills VoidMethodCallMutator on removeContactFromProject validation (line 480).
     * Ensures projectId validation is not skipped.
     */
    @Test
    void testRemoveContactFromProjectNullProjectIdThrows() {
        assertThatThrownBy(() -> service.removeContactFromProject(null, "contact1"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on removeContactFromProject validation (line 481).
     * Ensures contactId validation is not skipped.
     */
    @Test
    void testRemoveContactFromProjectNullContactIdThrows() {
        assertThatThrownBy(() -> service.removeContactFromProject("proj1", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Boundary test: whitespace-only projectId for removeContactFromProject.
     */
    @Test
    void testRemoveContactFromProjectWhitespaceProjectIdThrows() {
        assertThatThrownBy(() -> service.removeContactFromProject("  \t  ", "contact1"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId must not be null or blank");
    }

    /**
     * Boundary test: whitespace-only contactId for removeContactFromProject.
     */
    @Test
    void testRemoveContactFromProjectWhitespaceContactIdThrows() {
        assertThatThrownBy(() -> service.removeContactFromProject("proj1", "\t\n"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Kills EmptyObjectReturnValsMutator on getProjectById (line 382).
     * Ensures legacy store path returns actual project, not empty.
     */
    @Test
    void testGetProjectByIdReturnsActualProject() {
        Project project = new Project("gpbi-01", "Test Project", "Description", ProjectStatus.ACTIVE);
        service.addProject(project);

        // Verify getProjectById returns the actual project
        var result = service.getProjectById("gpbi-01");
        assertThat(result).isPresent();
        assertThat(result.get().getProjectId()).isEqualTo("gpbi-01");
        assertThat(result.get().getName()).isEqualTo("Test Project");
    }
}
