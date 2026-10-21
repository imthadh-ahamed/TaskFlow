import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchProject } from "@/lib/api/projects";
import { fetchTasks } from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { TaskBoard } from "@/features/tasks/TaskBoard";
import type { TaskPriority } from "@/types/api";

const VALID_PRIORITIES = new Set<string>([
  "low",
  "medium",
  "high",
  "urgent",
]);

export default async function TaskBoardPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ priority?: string }>;
}) {
  const [{ id }, sp] = await Promise.all([params, searchParams]);
  const projectId = Number(id);

  if (!Number.isFinite(projectId) || projectId <= 0) {
    notFound();
  }

  const priorityFilter = VALID_PRIORITIES.has(sp.priority ?? "")
    ? (sp.priority as TaskPriority)
    : undefined;

  let project, tasksPage;
  try {
    [project, tasksPage] = await Promise.all([
      fetchProject(projectId),
      fetchTasks(projectId, priorityFilter ? { priority: priorityFilter } : {}),
    ]);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      notFound();
    }
    throw err;
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <nav aria-label="Breadcrumb" className="mb-6">
        <ol className="flex items-center gap-2 text-sm text-zinc-500">
          <li>
            <Link href="/projects" className="hover:text-foreground">
              Projects
            </Link>
          </li>
          <li aria-hidden="true">·</li>
          <li>
            <Link
              href={`/projects/${projectId}`}
              className="hover:text-foreground"
            >
              {project.name}
            </Link>
          </li>
          <li aria-hidden="true">·</li>
          <li aria-current="page" className="text-foreground">
            Tasks
          </li>
        </ol>
      </nav>

      <h1 className="text-2xl font-semibold tracking-tight">
        {project.name} — Tasks
      </h1>

      <div className="mt-6">
        <TaskBoard
          projectId={projectId}
          tasks={tasksPage.content}
          total={tasksPage.total}
          priorityFilter={priorityFilter ?? ""}
        />
      </div>
    </div>
  );
}
