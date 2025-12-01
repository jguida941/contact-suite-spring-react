import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { ContactForm } from './ContactForm';

describe('ContactForm', () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all form fields for new contact', () => {
    render(
      <ContactForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
    );

    expect(screen.getByLabelText(/id/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('hides ID field in edit mode', () => {
    const existingContact = {
      id: 'ABC123',
      firstName: 'John',
      lastName: 'Doe',
      phone: '1234567890',
      address: '123 Main St',
    };

    render(
      <ContactForm
        contact={existingContact}
        onSubmit={mockOnSubmit}
        onCancel={mockOnCancel}
      />
    );

    // ID label should not be visible in edit mode
    expect(screen.queryByLabelText(/^id$/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty required fields', async () => {
    const user = userEvent.setup();

    render(
      <ContactForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
    );

    // Submit without filling fields
    await user.click(screen.getByRole('button', { name: /create/i }));

    await waitFor(() => {
      expect(screen.getByText(/id is required/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('shows validation error for invalid phone', async () => {
    const user = userEvent.setup();

    render(
      <ContactForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
    );

    await user.type(screen.getByLabelText(/id/i), 'ABC123');
    await user.type(screen.getByLabelText(/first name/i), 'John');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    await user.type(screen.getByLabelText(/phone/i), '123'); // Too short
    await user.type(screen.getByLabelText(/address/i), '123 Main St');

    await user.click(screen.getByRole('button', { name: /create/i }));

    await waitFor(() => {
      // Error message starts with "Phone must be" to distinguish from helper text
      expect(screen.getByText(/phone must be exactly 10 digits/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('calls onCancel when cancel button clicked', async () => {
    const user = userEvent.setup();

    render(
      <ContactForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
    );

    await user.click(screen.getByRole('button', { name: /cancel/i }));

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('disables submit button when loading', () => {
    render(
      <ContactForm
        onSubmit={mockOnSubmit}
        onCancel={mockOnCancel}
        isLoading={true}
      />
    );

    expect(screen.getByRole('button', { name: /saving/i })).toBeDisabled();
  });
});
