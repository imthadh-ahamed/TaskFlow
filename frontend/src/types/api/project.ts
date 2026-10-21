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
  /** Pre-auth: replaced by the authenticated user's ID once auth is added. */
  ownerId: number;
}

export interface UpdateProjectBody {
  name?: string;
  description?: string | null;
}
