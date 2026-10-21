import type { SerializedError } from "@reduxjs/toolkit";
import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import type { ApiErrorBody, ApiErrorDetail } from "@/types/api";

/** Extract a user-facing message from an RTK Query error. */
export function getRtkErrorMessage(
  error: FetchBaseQueryError | SerializedError | undefined,
): string {
  if (!error) return "";
  if ("status" in error) {
    if (error.status === "FETCH_ERROR")
      return "Network error. Check your connection and try again.";
    if (error.status === "PARSING_ERROR")
      return "Unexpected response from server.";
    const body = error.data as Partial<ApiErrorBody>;
    return body?.message ?? `Request failed (${error.status})`;
  }
  return error.message ?? "An unexpected error occurred.";
}

/** Extract field-level validation errors from an RTK Query error. */
export function getRtkFieldErrors(
  error: FetchBaseQueryError | SerializedError | undefined,
): ApiErrorDetail[] {
  if (!error || !("status" in error)) return [];
  const body = error.data as Partial<ApiErrorBody>;
  return body?.details ?? [];
}
