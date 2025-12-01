import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { ContactsPage } from './ContactsPage';
import * as api from '@/lib/api';

// Mock the API module
vi.mock('@/lib/api', () => ({
  contactsApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockContacts = [
  {
    id: 'ABC123',
    firstName: 'John',
    lastName: 'Doe',
    phone: '1234567890',
    address: '123 Main St',
  },
  {
    id: 'DEF456',
    firstName: 'Jane',
    lastName: 'Smith',
    phone: '0987654321',
    address: '456 Oak Ave',
  },
];

describe('ContactsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders page title and add button', async () => {
    vi.mocked(api.contactsApi.getAll).mockResolvedValue([]);

    render(<ContactsPage />);

    expect(screen.getByRole('heading', { name: /contacts/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add contact/i })).toBeInTheDocument();
  });

  it('shows loading state while fetching', () => {
    vi.mocked(api.contactsApi.getAll).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(<ContactsPage />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('shows error message when fetch fails', async () => {
    vi.mocked(api.contactsApi.getAll).mockRejectedValue(new Error('Network error'));

    render(<ContactsPage />);

    await waitFor(() => {
      expect(screen.getByText(/failed to load contacts/i)).toBeInTheDocument();
    });
  });

  it('shows empty state when no contacts exist', async () => {
    vi.mocked(api.contactsApi.getAll).mockResolvedValue([]);

    render(<ContactsPage />);

    await waitFor(() => {
      expect(screen.getByText(/no contacts found/i)).toBeInTheDocument();
    });
  });

  it('renders contacts in table', async () => {
    vi.mocked(api.contactsApi.getAll).mockResolvedValue(mockContacts);

    render(<ContactsPage />);

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
      expect(screen.getByText('1234567890')).toBeInTheDocument();
      expect(screen.getByText('123 Main St')).toBeInTheDocument();
    });
  });

  it('opens create sheet when add button clicked', async () => {
    vi.mocked(api.contactsApi.getAll).mockResolvedValue([]);
    const user = userEvent.setup();

    render(<ContactsPage />);

    await waitFor(() => {
      expect(screen.getByText(/no contacts found/i)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /add contact/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /new contact/i })).toBeInTheDocument();
    });
  });
});
