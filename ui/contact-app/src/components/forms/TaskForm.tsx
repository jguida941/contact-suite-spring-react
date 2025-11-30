import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { taskRequestSchema, ValidationLimits, type TaskRequest, type Task } from '@/lib/schemas';

interface TaskFormProps {
  task?: Task;
  onSubmit: (data: TaskRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function TaskForm({ task, onSubmit, onCancel, isLoading }: TaskFormProps) {
  const isEdit = !!task;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TaskRequest>({
    resolver: zodResolver(taskRequestSchema),
    defaultValues: task
      ? {
          id: task.id,
          name: task.name,
          description: task.description,
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
        <Label htmlFor="name">Name</Label>
        <Input
          id="name"
          {...register('name')}
          placeholder="Enter task name"
          maxLength={ValidationLimits.MAX_TASK_NAME_LENGTH}
        />
        {errors.name && (
          <p className="text-sm text-destructive">{errors.name.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Max {ValidationLimits.MAX_TASK_NAME_LENGTH} characters
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Input
          id="description"
          {...register('description')}
          placeholder="Enter task description"
          maxLength={ValidationLimits.MAX_DESCRIPTION_LENGTH}
        />
        {errors.description && (
          <p className="text-sm text-destructive">{errors.description.message}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Max {ValidationLimits.MAX_DESCRIPTION_LENGTH} characters
        </p>
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
