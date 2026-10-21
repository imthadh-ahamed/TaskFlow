import Link from "next/link";

export default function TasksPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-semibold tracking-tight">Tasks</h1>
      <p className="mt-2 text-zinc-500 dark:text-zinc-400">
        Tasks are organised by project. Open a project to view its task board.
      </p>
      <Link
        href="/projects"
        className="mt-4 inline-flex items-center rounded border border-zinc-300 px-4 py-2 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
      >
        Go to Projects →
      </Link>
    </div>
  );
}
