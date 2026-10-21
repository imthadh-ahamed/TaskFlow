-- V5__add_query_indexes.sql
-- Adds composite indexes for the most common filtered query patterns.
-- Existing single-column indexes (V1, V2, V4) cover the base lookup paths;
-- these indexes cover frequently-filtered combinations observed in the
-- repository's JDBC query methods.
--
-- Write-overhead analysis:
--   Tasks are inserted once and read many times. The two composite task indexes
--   add two extra B-tree entries per INSERT/UPDATE, which is negligible compared
--   to the read gain on large project task lists.
--   Projects are created infrequently; the single-column index on created_at
--   has minimal write cost.

-- ---------------------------------------------------------------------------
-- tasks: project + status filter
-- Query pattern (JdbcTaskRepository.findByProjectId / countByProjectId):
--   WHERE project_id = ? AND status = ?  ORDER BY created_at DESC
-- Without this index, PostgreSQL scans all tasks for the project and filters
-- by status in memory. With it, the planner can satisfy both predicates from
-- the index and return an already-bounded row set.
-- ---------------------------------------------------------------------------
create index idx_tasks_project_status
    on public.tasks (project_id, status);

-- ---------------------------------------------------------------------------
-- tasks: project + priority filter
-- Query pattern (JdbcTaskRepository.findByProjectId / countByProjectId):
--   WHERE project_id = ? AND priority = ?  ORDER BY created_at DESC
-- Mirrors the status index above for the priority filter path.
-- ---------------------------------------------------------------------------
create index idx_tasks_project_priority
    on public.tasks (project_id, priority);

-- ---------------------------------------------------------------------------
-- projects: ordered listing
-- Query pattern (JdbcProjectRepository.findAll):
--   ORDER BY created_at DESC  LIMIT ? OFFSET ?
-- Without this index, findAll requires a full sequential scan + sort.
-- With it, PostgreSQL can perform an ordered index scan with early termination
-- at the LIMIT, avoiding a full sort on large project sets.
-- Also benefits findByMemberId when the planner chooses a merge/sort join
-- rather than a nested-loop join over project_members.
-- ---------------------------------------------------------------------------
create index idx_projects_created_at
    on public.projects (created_at desc);
