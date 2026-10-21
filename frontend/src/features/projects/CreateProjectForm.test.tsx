import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { authSlice } from "@/store/slices/authSlice";
import { CreateProjectForm } from "./CreateProjectForm";

vi.mock("@/store/api", () => ({
  useCreateProjectMutation: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ refresh: vi.fn() }),
}));

import { useCreateProjectMutation } from "@/store/api";

function makeStore() {
  return configureStore({
    reducer: { [authSlice.name]: authSlice.reducer },
  });
}

function renderForm() {
  render(
    <Provider store={makeStore()}>
      <CreateProjectForm />
    </Provider>,
  );
}

describe("CreateProjectForm", () => {
  const mockCreate = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.mocked(useCreateProjectMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateProjectMutation>[1],
    ]);
  });

  it("renders name and description fields", () => {
    renderForm();
    expect(screen.getByLabelText(/^name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
  });

  it("renders create project button", () => {
    renderForm();
    expect(
      screen.getByRole("button", { name: /create project/i }),
    ).toBeInTheDocument();
  });

  it("shows loading state while submitting", () => {
    vi.mocked(useCreateProjectMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: true,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateProjectMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("button", { name: /creating/i })).toBeDisabled();
  });

  it("shows general error on server failure", () => {
    vi.mocked(useCreateProjectMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: {
          status: 500,
          data: { code: "INTERNAL_ERROR", message: "Unexpected server error", details: [] },
        },
        reset: mockReset,
      } as ReturnType<typeof useCreateProjectMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Unexpected server error");
  });

  it("shows field error for name when returned by the server", () => {
    vi.mocked(useCreateProjectMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: false,
        error: {
          status: 422,
          data: {
            code: "VALIDATION_ERROR",
            message: "Validation failed",
            details: [{ field: "name", message: "Name is required" }],
          },
        },
        reset: mockReset,
      } as ReturnType<typeof useCreateProjectMutation>[1],
    ]);
    renderForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Name is required");
  });

  it("calls create mutation with trimmed name and null description when empty", async () => {
    mockCreate.mockReturnValue(
      Object.assign(Promise.resolve({}), {
        unwrap: () => Promise.resolve({ id: 1, name: "My Project", description: null }),
      }),
    );
    renderForm();

    await userEvent.type(screen.getByLabelText(/^name/i), "  My Project  ");
    await userEvent.click(screen.getByRole("button", { name: /create project/i }));

    await waitFor(() =>
      expect(mockCreate).toHaveBeenCalledWith({
        name: "My Project",
        description: null,
      }),
    );
  });

  it("calls create mutation with description when provided", async () => {
    mockCreate.mockReturnValue(
      Object.assign(Promise.resolve({}), {
        unwrap: () => Promise.resolve({ id: 2, name: "Alpha", description: "Details" }),
      }),
    );
    renderForm();

    await userEvent.type(screen.getByLabelText(/^name/i), "Alpha");
    await userEvent.type(screen.getByLabelText(/description/i), "Details");
    await userEvent.click(screen.getByRole("button", { name: /create project/i }));

    await waitFor(() =>
      expect(mockCreate).toHaveBeenCalledWith({
        name: "Alpha",
        description: "Details",
      }),
    );
  });

  it("does not submit when already loading", async () => {
    vi.mocked(useCreateProjectMutation).mockReturnValue([
      mockCreate,
      {
        isLoading: true,
        error: undefined,
        reset: mockReset,
      } as ReturnType<typeof useCreateProjectMutation>[1],
    ]);
    const { container } = render(
      <Provider store={makeStore()}>
        <CreateProjectForm />
      </Provider>,
    );
    const form = container.querySelector("form")!;
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    expect(mockCreate).not.toHaveBeenCalled();
  });
});
