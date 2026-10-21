import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { authSlice, setUser } from "@/store/slices/authSlice";
import NavBar from "./NavBar";

vi.mock("@/store/api", () => ({
  useLogoutMutation: vi.fn(),
  taskflowApi: {
    reducerPath: "api",
    reducer: (s = {}) => s,
    middleware: () => (next: (a: unknown) => unknown) => (action: unknown) => next(action),
  },
}));

vi.mock("next/navigation", () => ({
  usePathname: vi.fn(() => "/"),
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("@/constants/app", () => ({
  APP_NAME: "TaskFlow",
}));

import { useLogoutMutation } from "@/store/api";
import { usePathname } from "next/navigation";

function makeStore(authenticated = false) {
  const store = configureStore({
    reducer: { [authSlice.name]: authSlice.reducer },
  });
  if (authenticated) {
    store.dispatch(
      setUser({ id: 1, email: "alice@example.com", displayName: "Alice", role: "user" }),
    );
  }
  return store;
}

function renderNavBar(authenticated = false) {
  const store = makeStore(authenticated);
  render(
    <Provider store={store}>
      <NavBar />
    </Provider>,
  );
  return store;
}

describe("NavBar", () => {
  const mockLogout = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogoutMutation).mockReturnValue([
      mockLogout,
      {
        isLoading: false,
        reset: mockReset,
      } as ReturnType<typeof useLogoutMutation>[1],
    ]);
    vi.mocked(usePathname).mockReturnValue("/");
  });

  it("renders app name", () => {
    renderNavBar();
    expect(screen.getByText("TaskFlow")).toBeInTheDocument();
  });

  it("renders main navigation links", () => {
    renderNavBar();
    expect(screen.getByRole("link", { name: /dashboard/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /projects/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /tasks/i })).toBeInTheDocument();
  });

  it("marks the active page link with aria-current", () => {
    vi.mocked(usePathname).mockReturnValue("/");
    renderNavBar();
    expect(screen.getByRole("link", { name: /dashboard/i })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("does not mark inactive links as current", () => {
    vi.mocked(usePathname).mockReturnValue("/");
    renderNavBar();
    expect(screen.getByRole("link", { name: /projects/i })).not.toHaveAttribute(
      "aria-current",
    );
  });

  it("shows sign in link when unauthenticated", () => {
    renderNavBar(false);
    expect(screen.getByRole("link", { name: /sign in/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /sign out/i })).not.toBeInTheDocument();
  });

  it("shows display name and sign out button when authenticated", () => {
    renderNavBar(true);
    expect(screen.getByText("Alice")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /sign in/i })).not.toBeInTheDocument();
  });

  it("shows loading state while logging out", () => {
    vi.mocked(useLogoutMutation).mockReturnValue([
      mockLogout,
      {
        isLoading: true,
        reset: mockReset,
      } as ReturnType<typeof useLogoutMutation>[1],
    ]);
    renderNavBar(true);
    expect(screen.getByRole("button", { name: /signing out/i })).toBeDisabled();
  });

  it("calls logout mutation and navigates on sign out click", async () => {
    mockLogout.mockResolvedValue({});
    renderNavBar(true);
    await userEvent.click(screen.getByRole("button", { name: /sign out/i }));
    expect(mockLogout).toHaveBeenCalled();
  });
});
