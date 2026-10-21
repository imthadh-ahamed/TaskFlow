export default function TaskBoardLoading() {
  return (
    <div
      className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8"
      aria-busy="true"
      aria-label="Loading task board"
    >
      <div className="mb-6 h-4 w-56 animate-pulse rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="mb-6 h-8 w-64 animate-pulse rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="rounded-lg border border-zinc-200 bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-900/30"
          >
            <div className="border-b border-zinc-200 px-3 py-2 dark:border-zinc-800">
              <div className="h-4 w-20 animate-pulse rounded bg-zinc-200 dark:bg-zinc-800" />
            </div>
            <div className="space-y-2 p-2">
              {[1, 2].map((j) => (
                <div
                  key={j}
                  className="h-16 animate-pulse rounded border border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900"
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
