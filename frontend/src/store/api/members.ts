import type { MemberDto, AddMemberBody, UpdateMemberRoleBody } from "@/types/api";
import { taskflowApi } from "./base";

// Members are nested under projects: /api/v1/projects/:projectId/members
const BASE = "/api/v1/projects";

const membersApi = taskflowApi.injectEndpoints({
  endpoints: (builder) => ({
    listMembers: builder.query<MemberDto[], number>({
      query: (projectId) => `${BASE}/${projectId}/members`,
      providesTags: (_result, _error, projectId) => [
        { type: "Member", id: projectId },
      ],
    }),

    addMember: builder.mutation<
      MemberDto,
      { projectId: number; body: AddMemberBody }
    >({
      query: ({ projectId, body }) => ({
        url: `${BASE}/${projectId}/members`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { projectId }) => [
        { type: "Member", id: projectId },
      ],
    }),

    updateMemberRole: builder.mutation<
      MemberDto,
      { projectId: number; userId: number; body: UpdateMemberRoleBody }
    >({
      query: ({ projectId, userId, body }) => ({
        url: `${BASE}/${projectId}/members/${userId}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { projectId }) => [
        { type: "Member", id: projectId },
      ],
    }),

    removeMember: builder.mutation<void, { projectId: number; userId: number }>({
      query: ({ projectId, userId }) => ({
        url: `${BASE}/${projectId}/members/${userId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { projectId }) => [
        { type: "Member", id: projectId },
      ],
    }),
  }),
  overrideExisting: false,
});

export const {
  useListMembersQuery,
  useAddMemberMutation,
  useUpdateMemberRoleMutation,
  useRemoveMemberMutation,
} = membersApi;
