import type { PageDto, ProjectDto } from "@/types/api";
import { serverApiFetch } from "./server-client";

export function fetchProjects(
  page = 0,
  size = 20,
): Promise<PageDto<ProjectDto>> {
  return serverApiFetch(`/api/v1/projects?page=${page}&size=${size}`);
}

export function fetchProject(id: number): Promise<ProjectDto> {
  return serverApiFetch(`/api/v1/projects/${id}`);
}
