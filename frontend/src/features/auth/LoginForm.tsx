"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLoginMutation } from "@/store/api";
import { useAppDispatch } from "@/store/store";
import { setUser } from "@/store/slices/authSlice";
import { getRtkErrorMessage, getRtkFieldErrors } from "@/lib/api/rtk-error";

export function LoginForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [login, { isLoading, error, reset }] = useLoginMutation();

  const fieldErrors = Object.fromEntries(
    getRtkFieldErrors(error).map((d) => [d.field, d.message]),
  );
  const generalError =
    getRtkFieldErrors(error).length === 0 ? getRtkErrorMessage(error) : null;

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    reset();

    try {
      const user = await login({ email, password }).unwrap();
      dispatch(setUser(user));
      router.push("/");
    } catch {
      // error state is tracked by the mutation hook
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {generalError && (
        <p
          role="alert"
          className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {generalError}
        </p>
      )}

      <div>
        <label htmlFor="lf-email" className="block text-sm font-medium">
          Email <span aria-hidden="true">*</span>
        </label>
        <input
          id="lf-email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          aria-describedby={fieldErrors.email ? "lf-email-error" : undefined}
          aria-invalid={fieldErrors.email ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.email && (
          <p
            id="lf-email-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.email}
          </p>
        )}
      </div>

      <div>
        <label htmlFor="lf-password" className="block text-sm font-medium">
          Password <span aria-hidden="true">*</span>
        </label>
        <input
          id="lf-password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          aria-describedby={
            fieldErrors.password ? "lf-password-error" : undefined
          }
          aria-invalid={fieldErrors.password ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.password && (
          <p
            id="lf-password-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.password}
          </p>
        )}
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
      >
        {isLoading ? "Signing in…" : "Sign in"}
      </button>

      <p className="text-center text-sm text-zinc-500 dark:text-zinc-400">
        No account?{" "}
        <Link
          href="/register"
          className="font-medium text-zinc-900 underline dark:text-zinc-100"
        >
          Register
        </Link>
      </p>
    </form>
  );
}
