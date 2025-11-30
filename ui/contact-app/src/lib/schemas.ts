import { z } from 'zod';

/**
 * Validation constants matching backend Validation.java
 * @see src/main/java/contactapp/domain/Validation.java
 */
export const ValidationLimits = {
  MAX_ID_LENGTH: 10,
  MAX_NAME_LENGTH: 10,
  MAX_ADDRESS_LENGTH: 30,
  MAX_TASK_NAME_LENGTH: 20,
  MAX_DESCRIPTION_LENGTH: 50,
  PHONE_LENGTH: 10,
} as const;

// ==================== Contact Schemas ====================

export const contactSchema = z.object({
  id: z
    .string()
    .min(1, 'ID is required')
    .max(ValidationLimits.MAX_ID_LENGTH, `ID must be at most ${ValidationLimits.MAX_ID_LENGTH} characters`),
  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(ValidationLimits.MAX_NAME_LENGTH, `First name must be at most ${ValidationLimits.MAX_NAME_LENGTH} characters`),
  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(ValidationLimits.MAX_NAME_LENGTH, `Last name must be at most ${ValidationLimits.MAX_NAME_LENGTH} characters`),
  phone: z
    .string()
    .length(ValidationLimits.PHONE_LENGTH, `Phone must be exactly ${ValidationLimits.PHONE_LENGTH} digits`)
    .regex(/^\d+$/, 'Phone must only contain digits'),
  address: z
    .string()
    .min(1, 'Address is required')
    .max(ValidationLimits.MAX_ADDRESS_LENGTH, `Address must be at most ${ValidationLimits.MAX_ADDRESS_LENGTH} characters`),
});

export const contactRequestSchema = contactSchema;

export type Contact = z.infer<typeof contactSchema>;
export type ContactRequest = z.infer<typeof contactRequestSchema>;

// ==================== Task Schemas ====================

export const taskSchema = z.object({
  id: z
    .string()
    .min(1, 'ID is required')
    .max(ValidationLimits.MAX_ID_LENGTH, `ID must be at most ${ValidationLimits.MAX_ID_LENGTH} characters`),
  name: z
    .string()
    .min(1, 'Name is required')
    .max(ValidationLimits.MAX_TASK_NAME_LENGTH, `Name must be at most ${ValidationLimits.MAX_TASK_NAME_LENGTH} characters`),
  description: z
    .string()
    .min(1, 'Description is required')
    .max(ValidationLimits.MAX_DESCRIPTION_LENGTH, `Description must be at most ${ValidationLimits.MAX_DESCRIPTION_LENGTH} characters`),
});

export const taskRequestSchema = taskSchema;

export type Task = z.infer<typeof taskSchema>;
export type TaskRequest = z.infer<typeof taskRequestSchema>;

// ==================== Appointment Schemas ====================

export const appointmentSchema = z.object({
  id: z
    .string()
    .min(1, 'ID is required')
    .max(ValidationLimits.MAX_ID_LENGTH, `ID must be at most ${ValidationLimits.MAX_ID_LENGTH} characters`),
  appointmentDate: z.string().datetime({ message: 'Invalid date format' }),
  description: z
    .string()
    .min(1, 'Description is required')
    .max(ValidationLimits.MAX_DESCRIPTION_LENGTH, `Description must be at most ${ValidationLimits.MAX_DESCRIPTION_LENGTH} characters`),
});

export const appointmentRequestSchema = appointmentSchema.extend({
  // Accept any non-empty string - let the form convert it to ISO before submission
  appointmentDate: z.string().min(1, 'Date is required'),
}).transform((data) => ({
  ...data,
  // Ensure date is valid before submission
  appointmentDate: data.appointmentDate,
}));

export type Appointment = z.infer<typeof appointmentSchema>;
export type AppointmentRequest = z.infer<typeof appointmentRequestSchema>;
