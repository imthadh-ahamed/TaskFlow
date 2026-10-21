import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { authSlice } from "@/store/slices/authSlice";
import { LoginForm } from "./LoginForm";

// Mock RTK Query auth module
vi.mock("@/store/api", () => ({
  useLoginMutation: vi.fn(),
}));

// Mock Next.js navigation
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

import { useLoginMutation } from "@/store/api";

function makeStore() {
  return configureStore({
    reducer: { [authSlice.name]: authSlice.reducer },
  });
}

function renderLoginForm() {
  const store = makeStore();
  render(
    <Provider store={store}>
      <LoginForm />
    </Provider>,
  );
  return store;
}

describe("LoginForm", () => {
  const mockLogin = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.mocked(useLoginMutation).mockReturnValue([
      mockLogin,
      { isLoading: false, error: undefined, reset: mockReset } as ReturnType<typeof useLoginMutation>[1],
    ]);
  });

  it("renders email and password fields", () => {
    renderLoginForm();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it("renders sign in button", () => {
    renderLoginForm();
    expect(
      screen.getByRole("button", { name: /sign in/i }),
    ).toBeInTheDocument();
  });

  it("shows loading state while submitting", () => {
    vi.mocked(useLoginMutation).mockReturnValue([
      mockLogin,
      { isLoading: true, error: undefined, reset: mockReset } as ReturnType<typeof useLoginMutation>[1],
    ]);
    renderLoginForm();
    expect(screen.getByRole("button", { name: /signing in/i })).toBeDisabled();
  });

  it("shows general error message on failed login", () => {
    const error = {
      status: 401,
      data: { code: "INVALID_CREDENTIALS", message: "Invalid credentials", details: [] },
    };
    vi.mocked(useLoginMutation).mockReturnValue([
      mockLogin,
      { isLoading: false, error, reset: mockReset } as ReturnType<typeof useLoginMutation>[1],
    ]);
    renderLoginForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Invalid credentials");
  });

  it("calls login mutation with email and password on submit", async () => {
    mockLogin.mockResolvedValue({ data: { id: 1, email: "a@b.com", displayName: "A", role: "user" } });
    renderLoginForm();

    await userEvent.type(screen.getByLabelText(/email/i), "alice@example.com");
    await userEvent.type(screen.getByLabelText(/password/i), "password123");
    await userEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(mockLogin).toHaveBeenCalledWith({
      email: "alice@example.com",
      password: "password123",
    });
  });

  it("has a register link", () => {
    renderLoginForm();
    expect(screen.getByRole("link", { name: /register/i })).toBeInTheDocument();
  });
});
