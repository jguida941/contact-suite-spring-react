import type { Contact, ContactRequest, Task, TaskRequest, Appointment, AppointmentRequest } from './schemas';

const API_BASE = '/api/v1';

/**
 * Normalized API error with message and optional field errors.
 */
export interface ApiError {
  message: string;
  status: number;
  errors?: Record<string, string>;
}

/**
 * Handles fetch response, throwing ApiError on non-2xx status.
 * Returns parsed JSON for successful responses.
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    let errors: Record<string, string> | undefined;

    try {
      const body = await response.json();
      message = body.message || body.error || message;
      errors = body.errors;
    } catch {
      // Response body is not JSON, use status text
      message = response.statusText || message;
    }

    const error: ApiError = { message, status: response.status, errors };
    throw error;
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

// ==================== Contacts API ====================

export const contactsApi = {
  getAll: async (): Promise<Contact[]> => {
    const response = await fetch(`${API_BASE}/contacts`);
    return handleResponse<Contact[]>(response);
  },

  getById: async (id: string): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`);
    return handleResponse<Contact>(response);
  },

  create: async (data: ContactRequest): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  update: async (id: string, data: Partial<ContactRequest>): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// ==================== Tasks API ====================

export const tasksApi = {
  getAll: async (): Promise<Task[]> => {
    const response = await fetch(`${API_BASE}/tasks`);
    return handleResponse<Task[]>(response);
  },

  getById: async (id: string): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`);
    return handleResponse<Task>(response);
  },

  create: async (data: TaskRequest): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  update: async (id: string, data: Partial<TaskRequest>): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// ==================== Appointments API ====================

export const appointmentsApi = {
  getAll: async (): Promise<Appointment[]> => {
    const response = await fetch(`${API_BASE}/appointments`);
    return handleResponse<Appointment[]>(response);
  },

  getById: async (id: string): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`);
    return handleResponse<Appointment>(response);
  },

  create: async (data: AppointmentRequest): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  update: async (id: string, data: Partial<AppointmentRequest>): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};
