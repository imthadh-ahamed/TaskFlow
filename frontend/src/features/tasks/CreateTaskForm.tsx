"use client";

import { useState } from "react";
import { useCreateTaskMutation } from "@/store/api";
import { getRtkErrorMessage, getRtkFieldErrors } from "@/lib/api/rtk-error";
import type { TaskPriority } from "@/types/api";

const PRIORITIES: { value: TaskPriority; label: string }[] = [
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
  { value: "urgent", label: "Urgent" },
];

type Props = {
  projectId: number;
  onCreated: () => void;
  onCancel: () => void;
};

export function CreateTaskForm({ projectId, onCreated, onCancel }: Props) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TaskPriority>("medium");
  const [dueDate, setDueDate] = useState("");

  const [createTask, { isLoading, error, reset }] = useCreateTaskMutation();

  const fieldErrors = Object.fromEntries(
    getRtkFieldErrors(error).map((d) => [d.field, d.message]),
  );
  const generalError =
    getRtkFieldErrors(error).length === 0 ? getRtkErrorMessage(error) : null;

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    reset();

    try {
      await createTask({
        projectId,
        body: {
          title: title.trim(),
          description: description.trim() || null,
          priority,
          dueDate: dueDate || null,
        },
      }).unwrap();
      onCreated();
    } catch {
      // error state is tracked by the mutation hook
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-900/50"
      noValidate
      aria-label="New task"
    >
      <h3 className="text-sm font-semibold">New task</h3>

      {generalError && (
        <p
          role="alert"
          className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {generalError}
        </p>
      )}

      <div>
        <label htmlFor="ctf-title" className="block text-sm font-medium">
          Title <span aria-hidden="true">*</span>
        </label>
        <input
          id="ctf-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={255}
          required
          aria-describedby={fieldErrors.title ? "ctf-title-error" : undefined}
          aria-invalid={fieldErrors.title ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.title && (
          <p
            id="ctf-title-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.title}
          </p>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="ctf-priority" className="block text-sm font-medium">
            Priority
          </label>
          <select
            id="ctf-priority"
            value={priority}
            onChange={(e) => setPriority(e.target.value as TaskPriority)}
            className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
          >
            {PRIORITIES.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="ctf-due-date" className="block text-sm font-medium">
            Due date
          </label>
          <input
            id="ctf-due-date"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
          />
        </div>
      </div>

      <div>
        <label htmlFor="ctf-description" className="block text-sm font-medium">
          Description
        </label>
        <textarea
          id="ctf-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={2}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
      </div>

      <div className="flex items-center gap-2">
        <button
          type="submit"
          disabled={isLoading}
          className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
        >
          {isLoading ? "Creating…" : "Create task"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded border border-zinc-300 px-4 py-2 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
