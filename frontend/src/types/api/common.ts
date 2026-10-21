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

/** Stable error envelope — matches the backend ErrorResponse shape. */
export interface ApiErrorBody {
  status: number;
  error: string;
  code: string;
  message: string;
  details: ApiErrorDetail[];
  timestamp: string;
  path: string;
}
