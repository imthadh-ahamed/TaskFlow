# procedures/

Canonical reference files for PL/pgSQL stored procedures. These files document the current intended definition of each procedure. Procedures are introduced and changed exclusively through versioned migration files in `migrations/`.

## Conventions

See `docs/conventions.md` for the full SQL conventions reference. Key rules for procedures:

- Use `CREATE OR REPLACE PROCEDURE` for definitions.
- Use PL/pgSQL (`LANGUAGE plpgsql`) for any procedural logic.
- Use explicit, descriptive parameter names and fully qualified types.
- Schema-qualify object references where ambiguity is possible (e.g. `public.table_name`).
- Handle exceptions deliberately — do not use bare `EXCEPTION WHEN OTHERS THEN NULL`.
- Document transaction behavior (`COMMIT`/`ROLLBACK`) and any side effects in a header comment.
- Use parameterized queries; never concatenate untrusted input into dynamic SQL strings.
- Keep business logic in procedures only when there is a clear, documented requirement. Prefer the application layer for complex logic.

## File Naming

```
<schema>_<object_name>.sql
```

Example: `public_archive_completed_tasks.sql`

## Procedure inventory

| File | Procedure | Introduced | Returns |
|------|-----------|------------|---------|
| `GetCommentsTaskId.sql` | `public.GetCommentsTaskId` | V7 | REFCURSOR (paginated comments) |
| `GetProjectById.sql` | `public.GetProjectById` | V8 | REFCURSOR (0–1 rows) |
| `GetAllProjects.sql` | `public.GetAllProjects` | V8 | REFCURSOR (paginated) |
| `GetProjectsByMemberId.sql` | `public.GetProjectsByMemberId` | V8 | REFCURSOR (paginated) |
| `CountProjectsByMemberId.sql` | `public.CountProjectsByMemberId` | V8 | INOUT BIGINT |
| `CountAllProjects.sql` | `public.CountAllProjects` | V8 | INOUT BIGINT |
| `UpdateProject.sql` | `public.UpdateProject` | V8 | REFCURSOR (0–1 rows) |
| `DeleteProject.sql` | `public.DeleteProject` | V8 | INOUT BIGINT (rows deleted) |
| `AddProjectMember.sql` | `public.AddProjectMember` | V9 | REFCURSOR (new row) |
| `GetMembersByProjectId.sql` | `public.GetMembersByProjectId` | V9 | REFCURSOR |
| `GetMemberByProjectAndUser.sql` | `public.GetMemberByProjectAndUser` | V9 | REFCURSOR (0–1 rows) |
| `UpdateMemberRole.sql` | `public.UpdateMemberRole` | V9 | REFCURSOR (0–1 rows) |
| `SaveTask.sql` | `public.SaveTask` | V10 | REFCURSOR (new row) |
| `GetTaskById.sql` | `public.GetTaskById` | V10 | REFCURSOR (0–1 rows) |
| `GetTasksByProjectId.sql` | `public.GetTasksByProjectId` | V10 | REFCURSOR (paginated, nullable filters) |
| `CountTasksByProjectId.sql` | `public.CountTasksByProjectId` | V10 | INOUT BIGINT |
| `UpdateTask.sql` | `public.UpdateTask` | V10 | REFCURSOR (0–1 rows) |
| `UpdateTaskStatus.sql` | `public.UpdateTaskStatus` | V10 | REFCURSOR (0–1 rows) |
| `GetTasksByProjectIdWithAssignee.sql` | `public.GetTasksByProjectIdWithAssignee` | V10 | REFCURSOR (includes `assignee_name`) |
| `GetTaskByIdWithAssignee.sql` | `public.GetTaskByIdWithAssignee` | V10 | REFCURSOR (includes `assignee_name`) |
| `SaveComment.sql` | `public.SaveComment` | V11 | REFCURSOR (new row, CTE insert) |
| `CountCommentsByTaskId.sql` | `public.CountCommentsByTaskId` | V11 | INOUT BIGINT |

## REFCURSOR calling pattern

Procedures that return a REFCURSOR require the `CALL` and `FETCH ALL FROM` statements to execute on the **same connection within the same transaction**. Java callers must use `JdbcTemplate.execute(ConnectionCallback<T>)` inside a `@Transactional` service method.

```java
jdbc.execute((ConnectionCallback<List<T>>) conn -> {
    try (CallableStatement cs = conn.prepareCall("CALL public.Proc(?, ?)")) {
        cs.setLong(1, id);
        cs.registerOutParameter(2, Types.OTHER);   // REFCURSOR
        cs.execute();
        try (Statement fetch = conn.createStatement();
             ResultSet rs = fetch.executeQuery("FETCH ALL FROM \"" + cs.getString(2) + "\"")) {
            // map rows
        }
    }
});
