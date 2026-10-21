/**
 * Route protection middleware.
 *
 * Runs on the server before protected pages are rendered. Forwards the
 * browser's cookies to the backend GET /me endpoint to verify the session.
 * Redirects unauthenticated requests to /login.
 *
 * CSRF: the backend uses SameSite=Strict cookies + Origin header validation
 * (OriginCheckFilter) instead of a token-based CSRF scheme. No frontend
 * CSRF token is needed; the browser supplies the Origin header automatically
 * on cross-origin requests.
 *
 * Cookie forwarding: HttpOnly token values are forwarded to the backend in
 * the Cookie request header. They are never read or logged here — this is the
 * deliberate server-side API strategy required for authenticated SSR.
 */
import { NextResponse, type NextRequest } from "next/server";

// Backend URL used server-side only — NOT exposed to the browser bundle.
const INTERNAL_API =
  process.env.NEXT_SERVER_API_URL ?? "http://localhost:8080";

export async function proxy(request: NextRequest) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  let authenticated = false;
  try {
    const res = await fetch(`${INTERNAL_API}/api/v1/auth/me`, {
      headers: { cookie: cookieHeader },
      // Ensure Next.js does not cache this check
      cache: "no-store",
    });
    authenticated = res.ok;
  } catch {
    // Backend unreachable — let the page handle the error rather than
    // redirecting (avoids a redirect loop when the backend is starting up).
    authenticated = true;
  }

  if (!authenticated) {
    const url = request.nextUrl.clone();
    const next = request.nextUrl.pathname + request.nextUrl.search;
    url.pathname = "/login";
    if (next !== "/") url.searchParams.set("next", next);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/projects/:path*", "/tasks/:path*"],
};
