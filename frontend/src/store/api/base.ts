import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from "@reduxjs/toolkit/query/react";
import type { AppDispatch } from "../store";
import { clearUser } from "../slices/authSlice";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const rawBaseQuery = fetchBaseQuery({
  baseUrl: BASE_URL,
  // credentials: "include" sends HttpOnly cookies with every cross-origin request.
  // The backend CORS config allows http://localhost:3000 with allowCredentials: true.
  credentials: "include",
});

// Shared promise to deduplicate concurrent refresh calls when multiple
// in-flight requests all get 401 at the same time.
let refreshPromise: Promise<{ error?: FetchBaseQueryError }> | null = null;

const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  let result = await rawBaseQuery(args, api, extraOptions);

  if (result.error?.status === 401) {
    const url = typeof args === "string" ? args : args?.url;
    // Skip refresh for login and refresh endpoints to avoid infinite loops
    const skipReauth =
      typeof url === "string" &&
      (url.includes("/auth/login") || url.includes("/auth/refresh"));

    if (!skipReauth) {
      if (!refreshPromise) {
        refreshPromise = (async () => {
          try {
            return await rawBaseQuery(
              { url: "/api/v1/auth/refresh", method: "POST" },
              api,
              extraOptions,
            );
          } finally {
            refreshPromise = null;
          }
        })();
      }

      const refreshResult = await refreshPromise;
      if (!refreshResult?.error) {
        // Refresh succeeded — retry the original request
        result = await rawBaseQuery(args, api, extraOptions);
      } else {
        // Refresh failed — session is gone; clear client-side user state
        (api.dispatch as AppDispatch)(clearUser());
      }
    }
  }

  return result;
};

/**
 * Shared API slice. All tag types are declared here so the store's
 * middleware knows about them before any endpoint file calls injectEndpoints.
 * Endpoints live in the five domain files: auth, projects, tasks, members, comments.
 */
export const taskflowApi = createApi({
  reducerPath: "taskflowApi",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["Project", "Task", "Member", "Comment"],
  endpoints: () => ({}),
});
