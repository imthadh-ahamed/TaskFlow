import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { authSlice } from "@/store/slices/authSlice";
import { CreateTaskForm } from "./CreateTaskForm";

vi.mock("@/store/api", () => ({
  useCreateTaskMutation: vi.fn(),
}));

import { useCreateTaskMutation } from "@/store/api";

function makeStore() {
  return configureStore({
    reducer: { [authSlice.name]: authSlice.reducer },
  });
}

const onCreated = vi.fn();
const onCancel = vi.fn();

function renderForm() {
  render(
    <Provider store={makeStore()}>
      <CreateTaskForm projectId={42} onCreated={onCreated} onCancel={onCancel} />
    </Provider>,
  );
}

describe("CreateTaskForm", () => {
  const mockCreate = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useCreateTaskMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateTaskMutation>[1],
    ]);
  });

  it("renders title, priority, due-date, and description fields", () => {
    renderForm();
    expect(screen.getByLabelText(/^title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/priority/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/due date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
  });

  it("renders create task and cancel buttons", () => {
    renderForm();
    expect(screen.getByRole("button", { name: /create task/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  it("defaults priority to medium", () => {
    renderForm();
    expect(screen.getByLabelText(/priority/i)).toHaveValue("medium");
  });

  it("shows all four priority options", () => {
    renderForm();
    const select = screen.getByLabelText(/priority/i);
    const options = Array.from((select as HTMLSelectElement).options).map(
      (o) => o.value,
    );
    expect(options).toEqual(["low", "medium", "high", "urgent"]);
  });

  it("shows loading state while submitting", () => {
    vi.mocked(useCreateTaskMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: true,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateTaskMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("button", { name: /creating/i })).toBeDisabled();
  });

  it("shows general error on server failure", () => {
    vi.mocked(useCreateTaskMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: {
          status: 403,
          data: { code: "FORBIDDEN", message: "Not a project member", details: [] },
        },
        reset: mockReset,
      } as ReturnType<typeof useCreateTaskMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Not a project member");
  });

  it("shows field error for title when returned by the server", () => {
    vi.mocked(useCreateTaskMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: {
          status: 422,
          data: {
            code: "VALIDATION_ERROR",
            message: "Validation failed",
            details: [{ field: "title", message: "Title must not be blank" }],
          },
        },
        reset: mockReset,
      } as ReturnType<typeof useCreateTaskMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Title must not be blank");
  });

  it("calls create mutation with correct projectId and body on submit", async () => {
    mockCreate.mockReturnValue(
      Object.assign(Promise.resolve({ id: 99 }), {
        unwrap: () => Promise.resolve({ id: 99, title: "Fix bug", priority: "high" }),
      }),
    );
    renderForm();

    await userEvent.type(screen.getByLabelText(/^title/i), "Fix bug");
    await userEvent.selectOptions(screen.getByLabelText(/priority/i), "high");
    await userEvent.click(screen.getByRole("button", { name: /create task/i }));

    await waitFor(() =>
      expect(mockCreate).toHaveBeenCalledWith({
        projectId: 42,
        body: {
          title: "Fix bug",
          description: null,
          priority: "high",
          dueDate: null,
        },
      }),
    );
  });

  it("calls onCreated after successful submission", async () => {
    mockCreate.mockReturnValue(
      Object.assign(Promise.resolve({ id: 10 }), {
        unwrap: () => Promise.resolve({ id: 10, title: "Done" }),
      }),
    );
    renderForm();

    await userEvent.type(screen.getByLabelText(/^title/i), "Done");
    await userEvent.click(screen.getByRole("button", { name: /create task/i }));

    await waitFor(() => expect(onCreated).toHaveBeenCalled());
  });

  it("calls onCancel when cancel is clicked", async () => {
    renderForm();
    await userEvent.click(screen.getByRole("button", { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalled();
  });

  it("does not submit when already loading", async () => {
    vi.mocked(useCreateTaskMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: true,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateTaskMutation>[1],
    ]);
    const { container } = render(
      <Provider store={makeStore()}>
        <CreateTaskForm projectId={1} onCreated={onCreated} onCancel={onCancel} />
      </Provider>,
    );
    const form = container.querySelector("form")!;
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    expect(mockCreate).not.toHaveBeenCalled();
  });
});
