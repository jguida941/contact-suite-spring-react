# Task Service Requirements Checklist

## Task Class Requirements

- [ ] Task ID is required, non-null, max length 10 characters, and immutable.
- [ ] Task name is required, non-null, max length 20 characters, and updatable.
- [ ] Task description is required, non-null, max length 50 characters, and updatable.

## Task Service Requirements
- [ ] Service can add a task only when the task ID is unique.
- [ ] Service can delete an existing task by task ID.
- [ ] Service can update an existing task’s name (respecting constraints).
- [ ] Service can update an existing task’s description (respecting constraints).
