export interface ProjectDto {
  id: number;
  ownerId: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectBody {
  name: string;
  description: string | null;
}

export interface UpdateProjectBody {
  name?: string;
  description?: string | null;
}
