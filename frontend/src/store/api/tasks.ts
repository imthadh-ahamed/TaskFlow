import type {
  TaskDto,
  PageDto,
  TaskStatus,
  TaskPriority,
  CreateTaskBody,
  UpdateTaskBody,
} from "@/types/api";
import { taskflowApi } from "./base";

// Project-scoped task collection: POST /…/tasks  GET /…/tasks
const PROJECTS_BASE = "/api/v1/projects";

// Direct task access: GET /tasks/:id  PATCH /tasks/:id  PATCH /tasks/:id/status
const BASE = "/api/v1/tasks";

const tasksApi = taskflowApi.injectEndpoints({
  endpoints: (builder) => ({
    listTasks: builder.query<
      PageDto<TaskDto>,
      {
        projectId: number;
        priority?: TaskPriority;
        status?: TaskStatus;
        assigneeId?: number;
        page?: number;
        size?: number;
      }
    >({
      query: ({ projectId, priority, status, assigneeId, page, size }) => ({
        url: `${PROJECTS_BASE}/${projectId}/tasks`,
        params: {
          page: page ?? 0,
          size: size ?? 100,
          ...(priority && { priority }),
          ...(status && { status }),
          ...(assigneeId && { assigneeId }),
        },
      }),
      providesTags: (result, _error, { projectId }) =>
        result
          ? [
              ...result.content.map(({ id }) => ({
                type: "Task" as const,
                id,
              })),
              { type: "Task" as const, id: `PROJECT-${projectId}` },
            ]
          : [{ type: "Task" as const, id: `PROJECT-${projectId}` }],
    }),

    getTask: builder.query<TaskDto, number>({
      query: (taskId) => `${BASE}/${taskId}`,
      providesTags: (_result, _error, taskId) => [{ type: "Task", id: taskId }],
    }),

    createTask: builder.mutation<
      TaskDto,
      { projectId: number; body: CreateTaskBody }
    >({
      query: ({ projectId, body }) => ({
        url: `${PROJECTS_BASE}/${projectId}/tasks`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { projectId }) => [
        { type: "Task", id: `PROJECT-${projectId}` },
      ],
    }),

    updateTask: builder.mutation<
      TaskDto,
      { taskId: number; body: UpdateTaskBody }
    >({
      query: ({ taskId, body }) => ({
        url: `${BASE}/${taskId}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Task", id: taskId },
      ],
    }),

    updateTaskStatus: builder.mutation<
      TaskDto,
      { taskId: number; status: TaskStatus }
    >({
      query: ({ taskId, status }) => ({
        url: `${BASE}/${taskId}/status`,
        method: "PATCH",
        // Backend TaskStatus enum expects uppercase (e.g. IN_PROGRESS)
        body: { status: status.toUpperCase() },
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Task", id: taskId },
      ],
    }),
  }),
  overrideExisting: false,
});

export const {
  useListTasksQuery,
  useGetTaskQuery,
  useCreateTaskMutation,
  useUpdateTaskMutation,
  useUpdateTaskStatusMutation,
} = tasksApi;
