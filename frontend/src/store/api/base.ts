import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/**
 * Shared API slice. All tag types are declared here so the store's
 * middleware knows about them before any endpoint file calls injectEndpoints.
 * Endpoints live in the four domain files: projects, tasks, members, comments.
 */
export const taskflowApi = createApi({
  reducerPath: "taskflowApi",
  baseQuery: fetchBaseQuery({
    baseUrl: BASE_URL,
    // credentials: "include" sends cookies with every request — required once
    // cookie-based JWT auth is wired in. The backend CORS origin list must be
    // explicit (not *) when allowCredentials is true.
    credentials: "include",
  }),
  tagTypes: ["Project", "Task", "Member", "Comment"],
  endpoints: () => ({}),
});
