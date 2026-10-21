import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchProject } from "@/lib/api/projects";
import { ApiError } from "@/lib/api/client";

export default async function ProjectDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const projectId = Number(id);

  if (!Number.isFinite(projectId) || projectId <= 0) {
    notFound();
  }

  let project;
  try {
    project = await fetchProject(projectId);
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
          <li aria-current="page" className="text-foreground">
            {project.name}
          </li>
        </ol>
      </nav>

      <h1 className="text-2xl font-semibold tracking-tight">{project.name}</h1>

      {project.description && (
        <p className="mt-2 text-zinc-600 dark:text-zinc-400">
          {project.description}
        </p>
      )}

      <div className="mt-6">
        <Link
          href={`/projects/${projectId}/tasks`}
          className="inline-flex items-center rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
        >
          View tasks →
        </Link>
      </div>

      <dl className="mt-6 space-y-2 text-sm">
        <div className="flex gap-2">
          <dt className="font-medium">Owner ID:</dt>
          <dd className="text-zinc-600 dark:text-zinc-400">{project.ownerId}</dd>
        </div>
        <div className="flex gap-2">
          <dt className="font-medium">Created:</dt>
          <dd className="text-zinc-600 dark:text-zinc-400">
            <time dateTime={project.createdAt}>
              {new Date(project.createdAt).toLocaleDateString(undefined, {
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </time>
          </dd>
        </div>
        <div className="flex gap-2">
          <dt className="font-medium">Updated:</dt>
          <dd className="text-zinc-600 dark:text-zinc-400">
            <time dateTime={project.updatedAt}>
              {new Date(project.updatedAt).toLocaleDateString(undefined, {
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </time>
          </dd>
        </div>
      </dl>
    </div>
  );
}
