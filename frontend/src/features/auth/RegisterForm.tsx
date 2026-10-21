"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useRegisterMutation } from "@/store/api";
import { getRtkErrorMessage, getRtkFieldErrors } from "@/lib/api/rtk-error";

export function RegisterForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");

  const [register, { isLoading, error, reset }] = useRegisterMutation();

  const fieldErrors = Object.fromEntries(
    getRtkFieldErrors(error).map((d) => [d.field, d.message]),
  );
  const generalError =
    getRtkFieldErrors(error).length === 0 ? getRtkErrorMessage(error) : null;

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    reset();

    try {
      await register({ email, displayName, password }).unwrap();
      // Registration does not issue tokens; redirect to login
      router.push("/login");
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
        <label htmlFor="rf-email" className="block text-sm font-medium">
          Email <span aria-hidden="true">*</span>
        </label>
        <input
          id="rf-email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          aria-describedby={fieldErrors.email ? "rf-email-error" : undefined}
          aria-invalid={fieldErrors.email ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.email && (
          <p
            id="rf-email-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.email}
          </p>
        )}
      </div>

      <div>
        <label htmlFor="rf-display-name" className="block text-sm font-medium">
          Display name <span aria-hidden="true">*</span>
        </label>
        <input
          id="rf-display-name"
          type="text"
          autoComplete="name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          minLength={2}
          maxLength={100}
          required
          aria-describedby={
            fieldErrors.displayName ? "rf-display-name-error" : undefined
          }
          aria-invalid={fieldErrors.displayName ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.displayName && (
          <p
            id="rf-display-name-error"
            role="alert"
            className="mt-1 text-xs text-red-600 dark:text-red-400"
          >
            {fieldErrors.displayName}
          </p>
        )}
      </div>

      <div>
        <label htmlFor="rf-password" className="block text-sm font-medium">
          Password <span aria-hidden="true">*</span>
        </label>
        <input
          id="rf-password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
          aria-describedby={
            fieldErrors.password ? "rf-password-error" : undefined
          }
          aria-invalid={fieldErrors.password ? true : undefined}
          className="mt-1 block w-full rounded border border-zinc-300 px-3 py-1.5 text-sm focus:border-zinc-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900"
        />
        {fieldErrors.password && (
          <p
            id="rf-password-error"
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
        {isLoading ? "Registering…" : "Create account"}
      </button>

      <p className="text-center text-sm text-zinc-500 dark:text-zinc-400">
        Already have an account?{" "}
        <Link
          href="/login"
          className="font-medium text-zinc-900 underline dark:text-zinc-100"
        >
          Sign in
        </Link>
      </p>
    </form>
  );
}
