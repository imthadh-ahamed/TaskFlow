import type { PageDto, TaskDto, TaskPriority, TaskStatus } from "@/types/api";
import { serverApiFetch } from "./server-client";

export function fetchTasks(
  projectId: number,
  filters: { priority?: TaskPriority } = {},
  page = 0,
  size = 100,
): Promise<PageDto<TaskDto>> {
  const q = new URLSearchParams({ page: String(page), size: String(size) });
  if (filters.priority) q.set("priority", filters.priority);
  return serverApiFetch(`/api/v1/projects/${projectId}/tasks?${q}`);
}

export function updateTaskStatus(
  taskId: number,
  status: TaskStatus,
): Promise<TaskDto> {
  return serverApiFetch(`/api/v1/tasks/${taskId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: status.toUpperCase() }),
  });
}
