import type { PageDto, TaskDto, TaskPriority, TaskStatus } from "@/types/api";
import { apiFetch } from "./client";

export function fetchTasks(
  projectId: number,
  filters: { priority?: TaskPriority } = {},
  page = 0,
  size = 100,
): Promise<PageDto<TaskDto>> {
  const q = new URLSearchParams({ page: String(page), size: String(size) });
  if (filters.priority) q.set("priority", filters.priority);
  return apiFetch(`/api/v1/projects/${projectId}/tasks?${q}`);
}

export function createTask(
  projectId: number,
  body: {
    title: string;
    description: string | null;
    priority: TaskPriority | null;
    dueDate: string | null;
  },
): Promise<TaskDto> {
  return apiFetch(`/api/v1/projects/${projectId}/tasks`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateTaskStatus(
  taskId: number,
  status: TaskStatus,
): Promise<TaskDto> {
  return apiFetch(`/api/v1/tasks/${taskId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: status.toUpperCase() }),
  });
}
