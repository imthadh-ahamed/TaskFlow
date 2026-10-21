"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useCreateProjectMutation } from "@/store/api";
import { getRtkErrorMessage, getRtkFieldErrors } from "@/lib/api/rtk-error";

export function CreateProjectForm() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const [createProject, { isLoading, error, reset }] =
    useCreateProjectMutation();

  const fieldErrors = Object.fromEntries(
    getRtkFieldErrors(error).map((d) => [d.field, d.message]),
  );
  const generalError =
    getRtkFieldErrors(error).length === 0 ? getRtkErrorMessage(error) : null;

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isLoading) return;
    reset();

    try {
      await createProject({
        name: name.trim(),
        description: description.trim() || null,
      }).unwrap();
      setName("");
      setDescription("");
      router.refresh();
    } catch {
      // error state is tracked by the mutation hook
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-4 space-y-4" noValidate>
      {generalError && (
        <p
          role="alert"
          className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {generalError}
        </p>
      )}

      <div>
        <label htmlFor="cpf-name" className="block text-sm font-medium">
          Name <span aria-hidden="true">*</span>
        </label>
        <input
          id="cpf-name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={200}
          required
          aria-describedby={fieldErrors.name ? "cpf-name-error" : undefined}
          aria-invalid={fieldErrors.name ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.name && (
          <p
            id="cpf-name-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.name}
          </p>
        )}
      </div>

      <div>
        <label htmlFor="cpf-description" className="block text-sm font-medium">
          Description
        </label>
        <textarea
          id="cpf-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={2}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
      >
        {isLoading ? "Creating…" : "Create project"}
      </button>
    </form>
  );
}
