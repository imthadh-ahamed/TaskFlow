export default function ProjectDetailLoading() {
  return (
    <div
      className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8"
      aria-busy="true"
      aria-label="Loading project"
    >
      <div className="mb-6 h-4 w-40 animate-pulse rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="h-8 w-64 animate-pulse rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="mt-4 h-12 w-full max-w-lg animate-pulse rounded bg-zinc-100 dark:bg-zinc-900" />
      <div className="mt-6 space-y-2">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-5 w-48 animate-pulse rounded bg-zinc-100 dark:bg-zinc-900"
          />
        ))}
      </div>
    </div>
  );
}
