"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { APP_NAME } from "@/constants/app";
import { useAppSelector, useAppDispatch } from "@/store/store";
import { useLogoutMutation } from "@/store/api";
import { clearUser } from "@/store/slices/authSlice";

type NavLink = { href: string; label: string; placeholder?: true };

const NAV_LINKS: NavLink[] = [
  { href: "/", label: "Dashboard" },
  { href: "/projects", label: "Projects", placeholder: true },
  { href: "/tasks", label: "Tasks", placeholder: true },
];

export default function NavBar() {
  const pathname = usePathname();
  const router = useRouter();
  const dispatch = useAppDispatch();
  const user = useAppSelector((s) => s.auth.user);
  const [logout, { isLoading: isLoggingOut }] = useLogoutMutation();

  async function handleLogout() {
    try {
      await logout().unwrap();
    } catch {
      // Even if the backend call fails, clear local state
    }
    dispatch(clearUser());
    router.push("/login");
  }

  return (
    <header className="sticky top-0 z-10 border-b border-zinc-200 bg-background dark:border-zinc-800">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="text-lg font-semibold tracking-tight text-foreground hover:opacity-80"
        >
          {APP_NAME}
        </Link>

        <nav aria-label="Main navigation">
          <ul className="flex items-center gap-1" role="list">
            {NAV_LINKS.map(({ href, label, placeholder }) => {
              const isActive = pathname === href;
              return (
                <li key={href}>
                  <Link
                    href={href}
                    aria-current={isActive ? "page" : undefined}
                    className={[
                      "rounded px-3 py-1.5 text-sm font-medium transition-colors",
                      isActive
                        ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                        : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100",
                    ].join(" ")}
                  >
                    {label}
                    {placeholder && (
                      <span className="sr-only"> (coming soon)</span>
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="ml-auto flex items-center gap-3">
          {user ? (
            <>
              <span className="hidden text-sm text-zinc-600 dark:text-zinc-400 sm:block">
                {user.displayName}
              </span>
              <button
                type="button"
                onClick={handleLogout}
                disabled={isLoggingOut}
                className="rounded border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:hover:bg-zinc-800"
              >
                {isLoggingOut ? "Signing out…" : "Sign out"}
              </button>
            </>
          ) : (
            <Link
              href="/login"
              className="rounded border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
            >
              Sign in
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
