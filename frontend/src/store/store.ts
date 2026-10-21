import { configureStore } from "@reduxjs/toolkit";
import { taskflowApi } from "./api";

export const makeStore = () =>
  configureStore({
    reducer: {
      [taskflowApi.reducerPath]: taskflowApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(taskflowApi.middleware),
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
