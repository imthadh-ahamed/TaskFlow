import type { CommentDto, PageDto, CreateCommentBody } from "@/types/api";
import { taskflowApi } from "./base";

// Comments are nested under tasks: /api/v1/tasks/:taskId/comments
const BASE = "/api/v1/tasks";

const commentsApi = taskflowApi.injectEndpoints({
  endpoints: (builder) => ({
    listComments: builder.query<
      PageDto<CommentDto>,
      { taskId: number; page?: number; size?: number }
    >({
      query: ({ taskId, page, size }) => ({
        url: `${BASE}/${taskId}/comments`,
        params: { page: page ?? 0, size: size ?? 20 },
      }),
      providesTags: (_result, _error, { taskId }) => [
        { type: "Comment", id: taskId },
      ],
    }),

    createComment: builder.mutation<
      CommentDto,
      { taskId: number; body: CreateCommentBody }
    >({
      query: ({ taskId, body }) => ({
        url: `${BASE}/${taskId}/comments`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Comment", id: taskId },
      ],
    }),
  }),
  overrideExisting: false,
});

export const { useListCommentsQuery, useCreateCommentMutation } = commentsApi;
