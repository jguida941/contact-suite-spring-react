import { test, expect } from '@playwright/test';

/**
 * E2E happy-path test for Contacts CRUD flow.
 *
 * This test mocks the API to run independently of the backend.
 * For full integration testing, run the Spring Boot backend first.
 */

// Mock data
const mockContacts = [
  {
    id: 'TEST001',
    firstName: 'Alice',
    lastName: 'Smith',
    phone: '1234567890',
    address: '123 Test St',
  },
];

test.describe('Contacts CRUD Happy Path', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API responses
    await page.route('**/api/v1/contacts', async (route) => {
      const method = route.request().method();

      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockContacts),
        });
      } else if (method === 'POST') {
        const body = route.request().postDataJSON();
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(body),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/v1/contacts/*', async (route) => {
      const method = route.request().method();

      if (method === 'PUT') {
        const body = route.request().postDataJSON();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...mockContacts[0], ...body }),
        });
      } else if (method === 'DELETE') {
        await route.fulfill({ status: 204 });
      } else {
        await route.continue();
      }
    });
  });

  test('displays contacts list', async ({ page }) => {
    await page.goto('/contacts');

    // Wait for contacts to load (use h2 which is the page title, not sidebar)
    await expect(page.locator('h2', { hasText: /contacts/i })).toBeVisible();
    await expect(page.getByText('Alice Smith')).toBeVisible();
    await expect(page.getByText('1234567890')).toBeVisible();
  });

  test('creates a new contact', async ({ page }) => {
    await page.goto('/contacts');

    // Click add button
    await page.getByRole('button', { name: /add contact/i }).click();

    // Fill form
    await expect(page.getByRole('heading', { name: /new contact/i })).toBeVisible();
    await page.getByLabel(/^id$/i).fill('NEW001');
    await page.getByLabel(/first name/i).fill('Bob');
    await page.getByLabel(/last name/i).fill('Jones');
    await page.getByLabel(/phone/i).fill('9876543210');
    await page.getByLabel(/address/i).fill('456 New Ave');

    // Submit
    await page.getByRole('button', { name: /create/i }).click();

    // Sheet should close (form submitted successfully)
    await expect(page.getByRole('heading', { name: /new contact/i })).not.toBeVisible();
  });

  test('shows validation errors for invalid input', async ({ page }) => {
    await page.goto('/contacts');

    // Click add button
    await page.getByRole('button', { name: /add contact/i }).click();

    // Submit empty form
    await page.getByRole('button', { name: /create/i }).click();

    // Should show validation errors
    await expect(page.getByText(/id is required/i)).toBeVisible();
  });

  test('edits an existing contact', async ({ page }) => {
    await page.goto('/contacts');

    // Wait for list to load
    await expect(page.getByText('Alice Smith')).toBeVisible();

    // Click on the row to open view mode, then click Edit button
    await page.getByText('Alice Smith').click();

    // Wait for view sheet to open
    await expect(page.getByText('Contact Details')).toBeVisible();

    // Click Edit button in the view sheet
    await page.getByRole('button', { name: /edit/i }).first().click();

    // Form should open with existing data
    await expect(page.getByText('Edit Contact')).toBeVisible();

    // Update first name
    await page.getByLabel(/first name/i).clear();
    await page.getByLabel(/first name/i).fill('Alicia');

    // Submit
    await page.getByRole('button', { name: /update/i }).click();

    // Sheet should close
    await expect(page.getByText('Edit Contact')).not.toBeVisible({ timeout: 10000 });
  });

  test('deletes a contact with confirmation', async ({ page }) => {
    await page.goto('/contacts');

    // Wait for list to load
    await expect(page.getByText('Alice Smith')).toBeVisible();

    // Click on the row to open view mode
    await page.getByText('Alice Smith').click();

    // Wait for view sheet to open
    await expect(page.getByText('Contact Details')).toBeVisible();

    // Click Delete button in the view sheet (use destructive variant)
    await page.getByRole('button', { name: /delete/i }).click();

    // Confirmation dialog should appear
    await expect(page.getByText(/are you sure you want to delete/i)).toBeVisible();

    // Confirm deletion (the dialog has a Delete button too - use dialog role, not alertdialog)
    await page.getByRole('dialog').getByRole('button', { name: /^delete$/i }).click();

    // Dialog should close
    await expect(page.getByText(/are you sure you want to delete/i)).not.toBeVisible({ timeout: 10000 });
  });
});
