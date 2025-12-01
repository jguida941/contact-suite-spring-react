import { describe, it, expect } from 'vitest';
import { contactSchema, taskSchema, appointmentSchema, ValidationLimits } from './schemas';

describe('contactSchema', () => {
  it('validates a correct contact', () => {
    const result = contactSchema.safeParse({
      id: 'ABC123',
      firstName: 'John',
      lastName: 'Doe',
      phone: '1234567890',
      address: '123 Main St',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty ID', () => {
    const result = contactSchema.safeParse({
      id: '',
      firstName: 'John',
      lastName: 'Doe',
      phone: '1234567890',
      address: '123 Main St',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('ID is required');
    }
  });

  it('rejects ID over max length', () => {
    const result = contactSchema.safeParse({
      id: 'A'.repeat(ValidationLimits.MAX_ID_LENGTH + 1),
      firstName: 'John',
      lastName: 'Doe',
      phone: '1234567890',
      address: '123 Main St',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain('at most');
    }
  });

  it('rejects phone with wrong length', () => {
    const result = contactSchema.safeParse({
      id: 'ABC123',
      firstName: 'John',
      lastName: 'Doe',
      phone: '123456789', // 9 digits
      address: '123 Main St',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain('exactly');
    }
  });

  it('rejects phone with non-digit characters', () => {
    const result = contactSchema.safeParse({
      id: 'ABC123',
      firstName: 'John',
      lastName: 'Doe',
      phone: '123-456-78',
      address: '123 Main St',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain('digits');
    }
  });
});

describe('taskSchema', () => {
  it('validates a correct task', () => {
    const result = taskSchema.safeParse({
      id: 'TASK001',
      name: 'Buy groceries',
      description: 'Milk, bread, eggs',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty name', () => {
    const result = taskSchema.safeParse({
      id: 'TASK001',
      name: '',
      description: 'Some task',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Name is required');
    }
  });

  it('rejects description over max length', () => {
    const result = taskSchema.safeParse({
      id: 'TASK001',
      name: 'Test',
      description: 'A'.repeat(ValidationLimits.MAX_DESCRIPTION_LENGTH + 1),
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain('at most');
    }
  });
});

describe('appointmentSchema', () => {
  it('validates a correct appointment', () => {
    const result = appointmentSchema.safeParse({
      id: 'APT001',
      appointmentDate: '2025-12-25T10:00:00.000Z',
      description: 'Doctor visit',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty description', () => {
    const result = appointmentSchema.safeParse({
      id: 'APT001',
      appointmentDate: '2025-12-25T10:00:00.000Z',
      description: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Description is required');
    }
  });
});
