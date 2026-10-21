"use client";

import { useEffect, useState } from "react";
import { Provider } from "react-redux";
import { makeStore, type AppStore, useAppDispatch } from "@/store/store";
import { useGetMeQuery } from "@/store/api";
import { setUser, clearUser } from "@/store/slices/authSlice";

/**
 * Calls GET /me on mount to restore the session from the HttpOnly cookies.
 * The re-auth base query handles access-token refresh automatically.
 * Dispatches setUser on success or clearUser on failure so the rest of the
 * app knows the initialization state without reading cookie values.
 */
function SessionInitializer() {
  const dispatch = useAppDispatch();
  const { data, isError, isSuccess } = useGetMeQuery();

  useEffect(() => {
    if (isSuccess && data) {
      dispatch(setUser(data));
    } else if (isError) {
      dispatch(clearUser());
    }
  }, [isSuccess, isError, data, dispatch]);

  return null;
}

export function StoreProvider({ children }: { children: React.ReactNode }) {
  // useState with a lazy initializer creates the store once per component
  // lifetime without violating React 19's rule against reading refs during render.
  const [store] = useState<AppStore>(makeStore);

  return (
    <Provider store={store}>
      <SessionInitializer />
      {children}
    </Provider>
  );
}
