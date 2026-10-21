import type { ApiErrorDetail } from "@/types/api";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details: ApiErrorDetail[] = [],
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    cache: "no-store",
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!res.ok) {
    let body: Partial<{
      code: string;
      message: string;
      details: ApiErrorDetail[];
    }> = {};
    try {
      body = (await res.json()) as typeof body;
    } catch {
      // ignore parse failures — use fallback values below
    }
    throw new ApiError(
      res.status,
      body.code ?? "UNKNOWN_ERROR",
      body.message ?? `Request failed with status ${res.status}`,
      body.details ?? [],
    );
  }

  return res.json() as Promise<T>;
}
