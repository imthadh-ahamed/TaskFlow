"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createProject } from "@/lib/api/projects";
import { ApiError } from "@/lib/api/client";

export function CreateProjectForm() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [ownerId, setOwnerId] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFieldErrors({});
    setGeneralError(null);
    setPending(true);

    try {
      await createProject({
        name: name.trim(),
        description: description.trim() || null,
        ownerId: Number(ownerId),
      });
      setName("");
      setDescription("");
      setOwnerId("");
      router.refresh();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.details.length > 0) {
          const errors: Record<string, string> = {};
          for (const d of err.details) errors[d.field] = d.message;
          setFieldErrors(errors);
        } else {
          setGeneralError(err.message);
        }
      } else {
        setGeneralError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setPending(false);
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
        <label
          htmlFor="cpf-name"
          className="block text-sm font-medium"
        >
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

      <div>
        <label htmlFor="cpf-owner-id" className="block text-sm font-medium">
          Owner ID <span aria-hidden="true">*</span>{" "}
          <span className="font-normal text-zinc-500 dark:text-zinc-400">
            (dev-stage — replaced by authenticated user after auth is added)
          </span>
        </label>
        <input
          id="cpf-owner-id"
          type="number"
          value={ownerId}
          onChange={(e) => setOwnerId(e.target.value)}
          required
          min={1}
          aria-describedby={
            fieldErrors.ownerId ? "cpf-owner-id-error" : undefined
          }
          aria-invalid={fieldErrors.ownerId ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.ownerId && (
          <p
            id="cpf-owner-id-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.ownerId}
          </p>
        )}
      </div>

      <button
        type="submit"
        disabled={pending}
        className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
      >
        {pending ? "Creating…" : "Create project"}
      </button>
    </form>
  );
}
