"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { APP_NAME } from "@/constants/app";

type NavLink = { href: string; label: string; placeholder?: true };

const NAV_LINKS: NavLink[] = [
  { href: "/", label: "Dashboard" },
  { href: "/projects", label: "Projects", placeholder: true },
  { href: "/tasks", label: "Tasks", placeholder: true },
];

export default function NavBar() {
  const pathname = usePathname();

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
      </div>
    </header>
  );
}
