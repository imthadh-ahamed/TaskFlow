"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { updateTaskStatus } from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { CreateTaskForm } from "./CreateTaskForm";
import type { TaskDto, TaskPriority, TaskStatus } from "@/types/api";

// ── Display maps ─────────────────────────────────────────────────────────────

const STATUS_COLUMNS: TaskStatus[] = [
  "todo",
  "in_progress",
  "in_review",
  "done",
];

const STATUS_LABELS: Record<TaskStatus, string> = {
  todo: "Todo",
  in_progress: "In Progress",
  in_review: "In Review",
  done: "Done",
};

// Which status a forward move lands on (undefined = no forward move)
const FORWARD: Partial<Record<TaskStatus, TaskStatus>> = {
  todo: "in_progress",
  in_progress: "in_review",
  in_review: "done",
};

// Which status a backward move lands on (undefined = no backward move)
const BACKWARD: Partial<Record<TaskStatus, TaskStatus>> = {
  in_progress: "todo",
  in_review: "in_progress",
};

const PRIORITY_BADGE: Record<TaskPriority, string> = {
  low: "bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400",
  medium: "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300",
  high: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
  urgent: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300",
};

const PRIORITY_OPTIONS: { value: "" | TaskPriority; label: string }[] = [
  { value: "", label: "All priorities" },
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
  { value: "urgent", label: "Urgent" },
];

// ── Types ─────────────────────────────────────────────────────────────────────

type Props = {
  projectId: number;
  tasks: TaskDto[];
  total: number;
  priorityFilter: TaskPriority | "";
};

// ── Component ─────────────────────────────────────────────────────────────────

export function TaskBoard({ projectId, tasks, total, priorityFilter }: Props) {
  const router = useRouter();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [movingTaskId, setMovingTaskId] = useState<number | null>(null);
  const [moveError, setMoveError] = useState<string | null>(null);

  const grouped = groupByStatus(tasks);

  function handlePriorityChange(value: string) {
    const params = new URLSearchParams();
    if (value) params.set("priority", value);
    const qs = params.toString();
    router.push(`/projects/${projectId}/tasks${qs ? `?${qs}` : ""}`);
  }

  async function handleMove(taskId: number, newStatus: TaskStatus) {
    setMoveError(null);
    setMovingTaskId(taskId);
    try {
      await updateTaskStatus(taskId, newStatus);
      router.refresh();
    } catch (err) {
      const msg =
        err instanceof ApiError ? err.message : "Failed to move task.";
      setMoveError(msg);
    } finally {
      setMovingTaskId(null);
    }
  }

  return (
    <div>
      {/* Filter bar + new task button */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <label htmlFor="priority-filter" className="text-sm font-medium">
            Priority
          </label>
          <select
            id="priority-filter"
            value={priorityFilter}
            onChange={(e) => handlePriorityChange(e.target.value)}
            className="rounded border border-zinc-300 px-3 py-1.5 text-sm focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
          >
            {PRIORITY_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          {total > tasks.length && (
            <span className="text-xs text-zinc-500">
              Showing {tasks.length} of {total}
            </span>
          )}
        </div>

        <button
          type="button"
          onClick={() => setShowCreateForm((v) => !v)}
          aria-expanded={showCreateForm}
          className="rounded bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
        >
          {showCreateForm ? "Cancel" : "+ New task"}
        </button>
      </div>

      {/* Move error */}
      {moveError && (
        <p
          role="alert"
          className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {moveError}
        </p>
      )}

      {/* Create form */}
      {showCreateForm && (
        <div className="mb-6">
          <CreateTaskForm
            projectId={projectId}
            onCreated={() => {
              setShowCreateForm(false);
              router.refresh();
            }}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Board columns */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {STATUS_COLUMNS.map((status) => {
          const columnTasks = grouped[status] ?? [];
          return (
            <section
              key={status}
              aria-labelledby={`col-${status}`}
              className="rounded-lg border border-zinc-200 bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-900/30"
            >
              <header className="flex items-center justify-between border-b border-zinc-200 px-3 py-2 dark:border-zinc-800">
                <h2
                  id={`col-${status}`}
                  className="text-sm font-semibold"
                >
                  {STATUS_LABELS[status]}
                </h2>
                <span
                  aria-label={`${columnTasks.length} tasks`}
                  className="rounded-full bg-zinc-200 px-2 py-0.5 text-xs font-medium dark:bg-zinc-800"
                >
                  {columnTasks.length}
                </span>
              </header>

              <ul
                className="space-y-2 p-2"
                role="list"
                aria-label={`${STATUS_LABELS[status]} tasks`}
              >
                {columnTasks.length === 0 && (
                  <li className="py-4 text-center text-xs text-zinc-400 dark:text-zinc-600">
                    No tasks
                  </li>
                )}
                {columnTasks.map((task) => (
                  <li key={task.id}>
                    <TaskCard
                      task={task}
                      moving={movingTaskId === task.id}
                      onMove={handleMove}
                    />
                  </li>
                ))}
              </ul>
            </section>
          );
        })}
      </div>
    </div>
  );
}

// ── TaskCard ──────────────────────────────────────────────────────────────────

type CardProps = {
  task: TaskDto;
  moving: boolean;
  onMove: (taskId: number, newStatus: TaskStatus) => void;
};

function TaskCard({ task, moving, onMove }: CardProps) {
  const back = BACKWARD[task.status];
  const forward = FORWARD[task.status];

  return (
    <article
      aria-label={task.title}
      className={[
        "rounded border border-zinc-200 bg-white p-3 text-sm dark:border-zinc-700 dark:bg-zinc-900",
        moving ? "opacity-50" : "",
      ]
        .join(" ")
        .trim()}
    >
      <p className="font-medium leading-snug">{task.title}</p>

      <div className="mt-2 flex flex-wrap items-center gap-2">
        <span
          className={`rounded px-1.5 py-0.5 text-xs font-medium capitalize ${PRIORITY_BADGE[task.priority]}`}
        >
          {task.priority.replace("_", " ")}
        </span>

        {task.dueDate && (
          <span className="text-xs text-zinc-500 dark:text-zinc-400">
            <time dateTime={task.dueDate}>
              Due {new Date(task.dueDate + "T00:00:00").toLocaleDateString(undefined, {
                month: "short",
                day: "numeric",
              })}
            </time>
          </span>
        )}
      </div>

      {(back ?? forward) && (
        <div className="mt-2 flex gap-1">
          {back && (
            <button
              type="button"
              disabled={moving}
              onClick={() => onMove(task.id, back)}
              aria-label={`Move "${task.title}" back to ${STATUS_LABELS[back]}`}
              className="rounded border border-zinc-200 px-2 py-0.5 text-xs hover:bg-zinc-100 disabled:opacity-40 dark:border-zinc-700 dark:hover:bg-zinc-800"
            >
              ← {STATUS_LABELS[back]}
            </button>
          )}
          {forward && (
            <button
              type="button"
              disabled={moving}
              onClick={() => onMove(task.id, forward)}
              aria-label={`Move "${task.title}" to ${STATUS_LABELS[forward]}`}
              className="rounded border border-zinc-200 px-2 py-0.5 text-xs hover:bg-zinc-100 disabled:opacity-40 dark:border-zinc-700 dark:hover:bg-zinc-800"
            >
              {STATUS_LABELS[forward]} →
            </button>
          )}
        </div>
      )}
    </article>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function groupByStatus(tasks: TaskDto[]): Record<TaskStatus, TaskDto[]> {
  const groups: Record<TaskStatus, TaskDto[]> = {
    todo: [],
    in_progress: [],
    in_review: [],
    done: [],
  };
  for (const task of tasks) {
    groups[task.status].push(task);
  }
  return groups;
}
