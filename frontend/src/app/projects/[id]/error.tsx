"use client";

import { useEffect } from "react";
import Link from "next/link";

export default function ProjectDetailError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Project detail error:", error);
  }, [error]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div
        role="alert"
        className="rounded-lg border border-red-200 bg-red-50 p-4 dark:border-red-900 dark:bg-red-950"
      >
        <h2 className="font-medium text-red-800 dark:text-red-200">
          Failed to load project
        </h2>
        <p className="mt-1 text-sm text-red-700 dark:text-red-300">
          {error.message}
        </p>
        <div className="mt-3 flex gap-3">
          <button
            onClick={reset}
            className="rounded border border-red-300 px-3 py-1.5 text-sm text-red-700 hover:bg-red-100 dark:border-red-700 dark:text-red-300 dark:hover:bg-red-900"
          >
            Try again
          </button>
          <Link
            href="/projects"
            className="rounded border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          >
            Back to projects
          </Link>
        </div>
      </div>
    </div>
  );
}
