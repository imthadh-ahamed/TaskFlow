import type { TaskStatus } from "./enums/task-status";
import type { TaskPriority } from "./enums/task-priority";

export type { TaskStatus, TaskPriority };

export interface TaskDto {
  id: number;
  projectId: number;
  assigneeId: number | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskBody {
  title: string;
  description: string | null;
  priority: TaskPriority | null;
  dueDate: string | null;
}

export interface UpdateTaskBody {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  assigneeId?: number;
  clearAssignee?: boolean;
  dueDate?: string;
}
