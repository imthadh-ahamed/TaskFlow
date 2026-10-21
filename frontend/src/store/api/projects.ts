import type {
  ProjectDto,
  PageDto,
  CreateProjectBody,
  UpdateProjectBody,
} from "@/types/api";
import { taskflowApi } from "./base";

const BASE = "/api/v1/projects";

const projectsApi = taskflowApi.injectEndpoints({
  endpoints: (builder) => ({
    listProjects: builder.query<
      PageDto<ProjectDto>,
      { page?: number; size?: number } | void
    >({
      query: (params) => ({
        url: BASE,
        params: { page: params?.page ?? 0, size: params?.size ?? 20 },
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({
                type: "Project" as const,
                id,
              })),
              { type: "Project" as const, id: "LIST" },
            ]
          : [{ type: "Project" as const, id: "LIST" }],
    }),

    getProject: builder.query<ProjectDto, number>({
      query: (id) => `${BASE}/${id}`,
      providesTags: (_result, _error, id) => [{ type: "Project", id }],
    }),

    createProject: builder.mutation<ProjectDto, CreateProjectBody>({
      query: (body) => ({ url: BASE, method: "POST", body }),
      invalidatesTags: [{ type: "Project", id: "LIST" }],
    }),

    updateProject: builder.mutation<
      ProjectDto,
      { id: number; body: UpdateProjectBody }
    >({
      query: ({ id, body }) => ({
        url: `${BASE}/${id}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Project", id },
        { type: "Project", id: "LIST" },
      ],
    }),

    deleteProject: builder.mutation<void, number>({
      query: (id) => ({ url: `${BASE}/${id}`, method: "DELETE" }),
      invalidatesTags: (_result, _error, id) => [
        { type: "Project", id },
        { type: "Project", id: "LIST" },
      ],
    }),
  }),
  overrideExisting: false,
});

export const {
  useListProjectsQuery,
  useGetProjectQuery,
  useCreateProjectMutation,
  useUpdateProjectMutation,
  useDeleteProjectMutation,
} = projectsApi;
