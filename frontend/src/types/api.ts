// status/priority are lowercase strings matching DB values (e.g. "in_progress", "high")
export type TaskStatus = "todo" | "in_progress" | "in_review" | "done";
export type TaskPriority = "low" | "medium" | "high" | "urgent";

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

export interface ProjectDto {
  id: number;
  ownerId: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ApiErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  status: number;
  error: string;
  code: string;
  message: string;
  details: ApiErrorDetail[];
  timestamp: string;
  path: string;
}
