/**
 * Server-side API fetch helper.
 *
 * Forwards the incoming request's cookies to the backend so that
 * Server Components can make authenticated requests on behalf of the
 * browser user. The HttpOnly tf_access cookie is forwarded as a Cookie
 * request header — its value is never exposed to client JavaScript.
 *
 * Only import this module from Server Components or Route Handlers.
 * Client Components use RTK Query (store/api/*) which handles credentials
 * via the browser's cookie jar automatically.
 */
import { cookies } from "next/headers";
import { apiFetch } from "./client";

export async function serverApiFetch<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  return apiFetch<T>(path, {
    ...init,
    headers: {
      ...init?.headers,
      ...(cookieHeader ? { Cookie: cookieHeader } : {}),
    },
  });
}
