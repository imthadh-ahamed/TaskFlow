import type { MemberRole } from "./enums/member-role";

export type { MemberRole };

export interface MemberDto {
  projectId: number;
  userId: number;
  role: MemberRole;
  createdAt: string;
}

/** role must not be OWNER — the project owner cannot be added as a non-owner member. */
export interface AddMemberBody {
  userId: number;
  role: Exclude<MemberRole, "OWNER">;
}

export interface UpdateMemberRoleBody {
  role: Exclude<MemberRole, "OWNER">;
}
