import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { AuthUserDto } from "@/types/api";

interface AuthState {
  user: AuthUserDto | null;
  /** true once the initial /me check has resolved (success or failure) */
  initialized: boolean;
}

const initialState: AuthState = {
  user: null,
  initialized: false,
};

export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setUser(state, action: PayloadAction<AuthUserDto>) {
      state.user = action.payload;
      state.initialized = true;
    },
    clearUser(state) {
      state.user = null;
      state.initialized = true;
    },
  },
});

export const { setUser, clearUser } = authSlice.actions;
