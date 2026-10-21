import { describe, it, expect } from "vitest";
import { authSlice, setUser, clearUser } from "./authSlice";
import type { AuthUserDto } from "@/types/api";

const { reducer } = authSlice;

const alice: AuthUserDto = {
  id: 1,
  email: "alice@example.com",
  displayName: "Alice",
  role: "user",
};

describe("authSlice", () => {
  it("starts with null user and uninitialized", () => {
    const state = reducer(undefined, { type: "@@INIT" });
    expect(state.user).toBeNull();
    expect(state.initialized).toBe(false);
  });

  it("setUser stores user and marks initialized", () => {
    const state = reducer(undefined, setUser(alice));
    expect(state.user).toEqual(alice);
    expect(state.initialized).toBe(true);
  });

  it("clearUser sets user to null and marks initialized", () => {
    // Start from an authenticated state
    const authed = reducer(undefined, setUser(alice));
    const cleared = reducer(authed, clearUser());
    expect(cleared.user).toBeNull();
    expect(cleared.initialized).toBe(true);
  });

  it("setUser then clearUser resets to null", () => {
    let state = reducer(undefined, setUser(alice));
    state = reducer(state, clearUser());
    expect(state.user).toBeNull();
  });

  it("setUser preserves all user fields", () => {
    const state = reducer(undefined, setUser(alice));
    expect(state.user?.id).toBe(1);
    expect(state.user?.email).toBe("alice@example.com");
    expect(state.user?.displayName).toBe("Alice");
    expect(state.user?.role).toBe("user");
  });
});
