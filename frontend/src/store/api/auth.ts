import type { AuthUserDto, LoginBody, RegisterBody } from "@/types/api";
import { taskflowApi } from "./base";

const authApi = taskflowApi.injectEndpoints({
  endpoints: (builder) => ({
    register: builder.mutation<AuthUserDto, RegisterBody>({
      query: (body) => ({
        url: "/api/v1/auth/register",
        method: "POST",
        body,
      }),
    }),

    login: builder.mutation<AuthUserDto, LoginBody>({
      query: (body) => ({
        url: "/api/v1/auth/login",
        method: "POST",
        body,
      }),
    }),

    logout: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/auth/logout",
        method: "POST",
      }),
      // Clear all cached data on logout
      invalidatesTags: ["Project", "Task", "Member", "Comment"],
    }),

    getMe: builder.query<AuthUserDto, void>({
      query: () => "/api/v1/auth/me",
    }),
  }),
  overrideExisting: false,
});

export const {
  useRegisterMutation,
  useLoginMutation,
  useLogoutMutation,
  useGetMeQuery,
} = authApi;
