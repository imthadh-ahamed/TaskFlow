export interface CommentDto {
  id: number;
  taskId: number;
  authorId: number;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentBody {
  body: string;
  /** Pre-auth: replaced by SecurityContext when auth is added. */
  authorId: number;
}
