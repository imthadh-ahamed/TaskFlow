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
