import type { PageDto, ProjectDto } from "@/types/api";
import { apiFetch } from "./client";

export function fetchProjects(
  page = 0,
  size = 20,
): Promise<PageDto<ProjectDto>> {
  return apiFetch(`/api/v1/projects?page=${page}&size=${size}`);
}

export function fetchProject(id: number): Promise<ProjectDto> {
  return apiFetch(`/api/v1/projects/${id}`);
}

export function createProject(body: {
  name: string;
  description: string | null;
  ownerId: number;
}): Promise<ProjectDto> {
  return apiFetch("/api/v1/projects", {
    method: "POST",
    body: JSON.stringify(body),
  });
}
