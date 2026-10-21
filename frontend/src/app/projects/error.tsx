"use client";

import { useEffect } from "react";

export default function ProjectsError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Projects page error:", error);
  }, [error]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div
        role="alert"
        className="rounded-lg border border-red-200 bg-red-50 p-4 dark:border-red-900 dark:bg-red-950"
      >
        <h2 className="font-medium text-red-800 dark:text-red-200">
          Failed to load projects
        </h2>
        <p className="mt-1 text-sm text-red-700 dark:text-red-300">
          {error.message}
        </p>
        <button
          onClick={reset}
          className="mt-3 rounded border border-red-300 px-3 py-1.5 text-sm text-red-700 hover:bg-red-100 dark:border-red-700 dark:text-red-300 dark:hover:bg-red-900"
        >
          Try again
        </button>
      </div>
    </div>
  );
}
