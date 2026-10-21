import Link from "next/link";
import { fetchProjects } from "@/lib/api/projects";
import { CreateProjectForm } from "@/features/projects/CreateProjectForm";

export default async function ProjectsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const params = await searchParams;
  const page = Math.max(0, Number(params.page ?? 0) || 0);
  const data = await fetchProjects(page);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-semibold tracking-tight">Projects</h1>

      <section
        aria-labelledby="create-project-heading"
        className="mt-6 rounded-lg border border-zinc-200 p-4 dark:border-zinc-800"
      >
        <h2 id="create-project-heading" className="text-base font-medium">
          New project
        </h2>
        <CreateProjectForm />
      </section>

      <section aria-labelledby="projects-list-heading" className="mt-8">
        <h2 id="projects-list-heading" className="sr-only">
          Project list
        </h2>

        {data.content.length === 0 ? (
          <p className="text-zinc-500 dark:text-zinc-400">
            No projects yet. Create one above.
          </p>
        ) : (
          <>
            <ul className="space-y-3" role="list">
              {data.content.map((project) => (
                <li key={project.id}>
                  <Link
                    href={`/projects/${project.id}`}
                    className="block rounded-lg border border-zinc-200 p-4 transition-colors hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-600"
                  >
                    <p className="font-medium">{project.name}</p>
                    {project.description && (
                      <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                        {project.description}
                      </p>
                    )}
                  </Link>
                </li>
              ))}
            </ul>

            {data.totalPages > 1 && (
              <nav
                aria-label="Project list pagination"
                className="mt-6 flex items-center gap-3"
              >
                {page > 0 && (
                  <Link
                    href={`/projects?page=${page - 1}`}
                    className="rounded border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
                  >
                    Previous
                  </Link>
                )}
                <span className="text-sm text-zinc-500" aria-live="polite">
                  Page {page + 1} of {data.totalPages}
                </span>
                {page < data.totalPages - 1 && (
                  <Link
                    href={`/projects?page=${page + 1}`}
                    className="rounded border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
                  >
                    Next
                  </Link>
                )}
              </nav>
            )}
          </>
        )}
      </section>
    </div>
  );
}
