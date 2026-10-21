"use client";

import { useState } from "react";
import { Provider } from "react-redux";
import { makeStore, type AppStore } from "@/store/store";

export function StoreProvider({ children }: { children: React.ReactNode }) {
  // useState with a lazy initializer creates the store once per component
  // lifetime without violating React 19's rule against reading refs during render.
  const [store] = useState<AppStore>(makeStore);

  return <Provider store={store}>{children}</Provider>;
}
