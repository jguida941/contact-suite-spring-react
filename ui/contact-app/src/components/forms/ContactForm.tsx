import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { contactRequestSchema, ValidationLimits, type ContactRequest, type Contact } from '@/lib/schemas';

interface ContactFormProps {
  contact?: Contact;
  onSubmit: (data: ContactRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function ContactForm({ contact, onSubmit, onCancel, isLoading }: ContactFormProps) {
  const isEdit = !!contact;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ContactRequest>({
    resolver: zodResolver(contactRequestSchema),
    defaultValues: contact
      ? {
          id: contact.id,
          firstName: contact.firstName,
          lastName: contact.lastName,
          phone: contact.phone,
          address: contact.address,
        }
      : undefined,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {isEdit ? (
        // Hidden input to include ID in edit submissions
        <input type="hidden" {...register('id')} />
      ) : (
        <div className="space-y-2">
          <Label htmlFor="id">ID</Label>
          <Input
            id="id"
            {...register('id')}
            placeholder="Unique ID (max 10 chars)"
            maxLength={ValidationLimits.MAX_ID_LENGTH}
          />
          {errors.id && (
            <p className="text-sm text-destructive">{errors.id.message}</p>
          )}
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="firstName">First Name</Label>
        <Input
          id="firstName"
          {...register('firstName')}
          placeholder="Enter first name"
          maxLength={ValidationLimits.MAX_NAME_LENGTH}
        />
        {errors.firstName && (
          <p className="text-sm text-destructive">{errors.firstName.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="lastName">Last Name</Label>
        <Input
          id="lastName"
          {...register('lastName')}
          placeholder="Enter last name"
          maxLength={ValidationLimits.MAX_NAME_LENGTH}
        />
        {errors.lastName && (
          <p className="text-sm text-destructive">{errors.lastName.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="phone">Phone</Label>
        <Input
          id="phone"
          {...register('phone')}
          placeholder="1234567890"
          maxLength={ValidationLimits.PHONE_LENGTH}
        />
        {errors.phone && (
          <p className="text-sm text-destructive">{errors.phone.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Must be exactly {ValidationLimits.PHONE_LENGTH} digits
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="address">Address</Label>
        <Input
          id="address"
          {...register('address')}
          placeholder="Enter address"
          maxLength={ValidationLimits.MAX_ADDRESS_LENGTH}
        />
        {errors.address && (
          <p className="text-sm text-destructive">{errors.address.message}</p>
        )}
      </div>

      <div className="flex justify-end gap-2 pt-4">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
        </Button>
      </div>
    </form>
  );
}
