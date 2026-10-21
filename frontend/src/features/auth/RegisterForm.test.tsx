import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { authSlice } from "@/store/slices/authSlice";
import { RegisterForm } from "./RegisterForm";

vi.mock("@/store/api", () => ({
  useRegisterMutation: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

import { useRegisterMutation } from "@/store/api";

function makeStore() {
  return configureStore({
    reducer: { [authSlice.name]: authSlice.reducer },
  });
}

function renderRegisterForm() {
  render(
    <Provider store={makeStore()}>
      <RegisterForm />
    </Provider>,
  );
}

describe("RegisterForm", () => {
  const mockRegister = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.mocked(useRegisterMutation).mockReturnValue([
      mockRegister,
      { isLoading: false, error: undefined, reset: mockReset } as ReturnType<typeof useRegisterMutation>[1],
    ]);
  });

  it("renders all required fields", () => {
    renderRegisterForm();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it("shows loading state while submitting", () => {
    vi.mocked(useRegisterMutation).mockReturnValue([
      mockRegister,
      { isLoading: true, error: undefined, reset: mockReset } as ReturnType<typeof useRegisterMutation>[1],
    ]);
    renderRegisterForm();
    expect(
      screen.getByRole("button", { name: /registering/i }),
    ).toBeDisabled();
  });

  it("shows conflict error when email already exists", () => {
    const error = {
      status: 409,
      data: { code: "EMAIL_ALREADY_EXISTS", message: "Email already in use", details: [] },
    };
    vi.mocked(useRegisterMutation).mockReturnValue([
      mockRegister,
      { isLoading: false, error, reset: mockReset } as ReturnType<typeof useRegisterMutation>[1],
    ]);
    renderRegisterForm();
    expect(screen.getByRole("alert")).toHaveTextContent("Email already in use");
  });

  it("calls register mutation with all fields on submit", async () => {
    mockRegister.mockResolvedValue({
      data: { id: 1, email: "new@example.com", displayName: "New User", role: "user" },
    });
    renderRegisterForm();

    await userEvent.type(screen.getByLabelText(/email/i), "new@example.com");
    await userEvent.type(screen.getByLabelText(/display name/i), "New User");
    await userEvent.type(screen.getByLabelText(/password/i), "securepass1");
    await userEvent.click(screen.getByRole("button", { name: /create account/i }));

    expect(mockRegister).toHaveBeenCalledWith({
      email: "new@example.com",
      displayName: "New User",
      password: "securepass1",
    });
  });

  it("has a sign in link", () => {
    renderRegisterForm();
    expect(screen.getByRole("link", { name: /sign in/i })).toBeInTheDocument();
  });
});
