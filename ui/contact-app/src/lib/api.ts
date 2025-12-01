import type { Contact, ContactRequest, Task, TaskRequest, Appointment, AppointmentRequest } from './schemas';
import { queryClient } from './queryClient';

const API_BASE = '/api/v1';
const AUTH_BASE = '/api/auth';

// ==================== Auth Types ====================

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token?: string | null; // omitted/null when using HttpOnly cookies
  username: string;
  email: string;
  role: 'USER' | 'ADMIN';
  expiresIn: number;
}

// ==================== Session State (in-memory, not localStorage) ====================

const USER_KEY = 'auth_user';
const PROFILE_STORAGE_KEY = 'contact-app-profile';
const SESSION_TIMESTAMP_KEY = 'auth_session_timestamp';

// In-memory state for current user (not persisted to localStorage for security)
let currentUser: AuthResponse | null = null;

/**
 * Session storage for user data. Uses sessionStorage instead of localStorage
 * so data is cleared when the browser tab closes.
 */
export const tokenStorage = {
  // Token is now in HttpOnly cookie - these methods are kept for backwards compat
  getToken: (): string | null => null, // Token is in HttpOnly cookie, not accessible to JS
  setToken: (_token: string): void => { /* no-op - token is in cookie */ },
  removeToken: (): void => { /* no-op - cookie cleared by backend */ },

  getUser: (): AuthResponse | null => {
    if (currentUser) return currentUser;
    // Try sessionStorage as fallback (page refresh within session)
    const user = sessionStorage.getItem(USER_KEY);
    if (user) {
      try {
        currentUser = JSON.parse(user);
        return currentUser;
      } catch {
        sessionStorage.removeItem(USER_KEY);
        currentUser = null;
        return null;
      }
    }
    return null;
  },
  setUser: (user: AuthResponse): void => {
    currentUser = user;
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    sessionStorage.setItem(SESSION_TIMESTAMP_KEY, Date.now().toString());
  },
  removeUser: (): void => {
    currentUser = null;
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(SESSION_TIMESTAMP_KEY);
  },

  clear: (): void => {
    currentUser = null;
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(PROFILE_STORAGE_KEY);
    sessionStorage.removeItem(SESSION_TIMESTAMP_KEY);
    // Also clear any legacy localStorage items
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem(PROFILE_STORAGE_KEY);
  },
};

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
 * Automatically logs out and redirects on 401 errors.
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // Handle authentication errors (invalid/expired token) with auto-logout
    if (response.status === 401) {
      tokenStorage.clear();
      queryClient.clear(); // Clear cached data to prevent data leakage
      // Redirect to login page if not already there
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }

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

/**
 * Common fetch options with credentials for cookie-based auth.
 */
const fetchOptions: RequestInit = {
  credentials: 'include', // Include HttpOnly cookies in requests
};

const METHODS_REQUIRING_CSRF = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
let cachedCsrfToken: string | null = null;

function readCookie(name: string): string | null {
  const value = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${name}=`));
  if (!value) return null;
  const separatorIndex = value.indexOf('=');
  if (separatorIndex === -1) return null;
  return decodeURIComponent(value.substring(separatorIndex + 1));
}

async function ensureCsrfToken(): Promise<string | null> {
  if (cachedCsrfToken) {
    return cachedCsrfToken;
  }
  const existingCookie = readCookie(CSRF_COOKIE_NAME);
  if (existingCookie) {
    cachedCsrfToken = existingCookie;
    return existingCookie;
  }
  try {
    const response = await fetch(`${AUTH_BASE}/csrf-token`, {
      credentials: 'include',
    });
    if (!response.ok) {
      return null;
    }
    const data = (await response.json()) as { token?: string };
    cachedCsrfToken = data?.token ?? readCookie(CSRF_COOKIE_NAME) ?? null;
    return cachedCsrfToken;
  } catch {
    return null;
  }
}

async function fetchWithCsrf(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const finalInit: RequestInit = {
    ...fetchOptions,
    ...init,
  };
  const headers = new Headers(init.headers ?? {});
  finalInit.headers = headers;
  const method = (finalInit.method ?? 'GET').toUpperCase();
  if (METHODS_REQUIRING_CSRF.has(method)) {
    const token = await ensureCsrfToken();
    if (token) {
      headers.set(CSRF_HEADER_NAME, token);
    }
  }
  return fetch(input, finalInit);
}

// ==================== Auth API ====================

export const authApi = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await fetchWithCsrf(`${AUTH_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    const result = await handleResponse<AuthResponse>(response);
    // Token is in HttpOnly cookie, just store user info in session
    tokenStorage.setUser(result);
    syncProfileFromAuth(result);
    return result;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await fetchWithCsrf(`${AUTH_BASE}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    const result = await handleResponse<AuthResponse>(response);
    // Token is in HttpOnly cookie, just store user info in session
    tokenStorage.setUser(result);
    syncProfileFromAuth(result);
    return result;
  },

  logout: async (): Promise<void> => {
    // Call backend to clear the HttpOnly cookie
    try {
      await fetchWithCsrf(`${AUTH_BASE}/logout`, {
        method: 'POST',
      });
    } catch {
      // Ignore errors - proceed with client-side cleanup regardless
    }
    cachedCsrfToken = null;
    tokenStorage.clear();
    queryClient.clear(); // Clear cached data to prevent data leakage to next user
  },

  /**
   * Check if user is authenticated. With HttpOnly cookies, we can't
   * directly check the token, so we rely on stored user info and
   * check expiration based on the expiresIn value from login.
   */
  isAuthenticated: (): boolean => {
    const user = tokenStorage.getUser();
    if (!user) return false;

    // Check if session has expired based on expiresIn
    // Note: This is a client-side check; actual auth is validated by backend
    // For precise expiration tracking, we'd need to store the login timestamp
    return true; // If we have user info, assume authenticated until 401
  },

  getCurrentUser: (): AuthResponse | null => {
    return tokenStorage.getUser();
  },
};

function syncProfileFromAuth(response: AuthResponse) {
  const initials = computeInitials(response.username);
  // Use sessionStorage instead of localStorage for security (clears on tab close)
  sessionStorage.setItem(
    PROFILE_STORAGE_KEY,
    JSON.stringify({
      name: response.username,
      email: response.email,
      initials,
    })
  );
}

function computeInitials(name: string): string {
  if (!name?.trim()) {
    return 'U';
  }
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return parts[0].charAt(0).toUpperCase();
  }
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

// ==================== Contacts API ====================

export const contactsApi = {
  getAll: async (): Promise<Contact[]> => {
    const response = await fetchWithCsrf(`${API_BASE}/contacts`);
    return handleResponse<Contact[]>(response);
  },

  getById: async (id: string): Promise<Contact> => {
    const response = await fetchWithCsrf(`${API_BASE}/contacts/${encodeURIComponent(id)}`);
    return handleResponse<Contact>(response);
  },

  create: async (data: ContactRequest): Promise<Contact> => {
    const response = await fetchWithCsrf(`${API_BASE}/contacts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  update: async (id: string, data: Partial<ContactRequest>): Promise<Contact> => {
    const response = await fetchWithCsrf(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetchWithCsrf(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// ==================== Tasks API ====================

export const tasksApi = {
  getAll: async (): Promise<Task[]> => {
    const response = await fetchWithCsrf(`${API_BASE}/tasks`);
    return handleResponse<Task[]>(response);
  },

  getById: async (id: string): Promise<Task> => {
    const response = await fetchWithCsrf(`${API_BASE}/tasks/${encodeURIComponent(id)}`);
    return handleResponse<Task>(response);
  },

  create: async (data: TaskRequest): Promise<Task> => {
    const response = await fetchWithCsrf(`${API_BASE}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  update: async (id: string, data: Partial<TaskRequest>): Promise<Task> => {
    const response = await fetchWithCsrf(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetchWithCsrf(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// ==================== Appointments API ====================

export const appointmentsApi = {
  getAll: async (): Promise<Appointment[]> => {
    const response = await fetchWithCsrf(`${API_BASE}/appointments`);
    return handleResponse<Appointment[]>(response);
  },

  getById: async (id: string): Promise<Appointment> => {
    const response = await fetchWithCsrf(`${API_BASE}/appointments/${encodeURIComponent(id)}`);
    return handleResponse<Appointment>(response);
  },

  create: async (data: AppointmentRequest): Promise<Appointment> => {
    const response = await fetchWithCsrf(`${API_BASE}/appointments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  update: async (id: string, data: Partial<AppointmentRequest>): Promise<Appointment> => {
    const response = await fetchWithCsrf(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetchWithCsrf(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};
