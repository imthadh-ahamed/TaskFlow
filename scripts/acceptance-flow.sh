#!/usr/bin/env bash
# T12.4 — End-to-end acceptance flow
#
# Exercises the minimum critical path against a running local backend.
# Prerequisites: backend running at http://localhost:8080, PostgreSQL up.
#
# Usage:
#   bash scripts/acceptance-flow.sh
#
# Exit codes: 0 = all checks passed, 1 = at least one check failed.

set -euo pipefail

BASE="http://localhost:8080/api/v1"
SUFFIX="$(date +%s)"
EMAIL="acceptance${SUFFIX}@example.com"
EMAIL2="member${SUFFIX}@example.com"
PASS="Accept@nc3Pass!"
PASS2="Member@nc3Pass!"
FAIL=0

pass() { echo "  ✓  $*"; }
fail() { echo "  ✗  $*" >&2; FAIL=1; }
section() { echo; echo "── $* ──"; }

require_jq() {
  if ! command -v jq &>/dev/null; then
    echo "ERROR: jq is required (https://stedolan.github.io/jq/)" >&2
    exit 1
  fi
}

http_status() { echo "$1" | head -1; }
http_body()   { echo "$1" | tail -n +2; }

call() {
  # call <method> <path> [extra curl args...]
  local method="$1" path="$2"; shift 2
  curl -s -w "\n%{http_code}" -X "$method" "$BASE$path" \
    -H "Content-Type: application/json" \
    "${COOKIE_ARGS[@]}" \
    "$@"
}

require_jq

# ── 1. Register two users ──────────────────────────────────────────────────
section "1. Register users"

TMPJAR="$(mktemp /tmp/taskflow-cookies-XXXXXX.jar)"
COOKIE_ARGS=("--cookie-jar" "$TMPJAR" "--cookie" "$TMPJAR")

resp=$(call POST /auth/register -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"displayName\":\"Acceptance User\"}")
status=$(http_status "$resp"); body=$(http_body "$resp")
if [ "$status" = "201" ]; then pass "Register owner (201)"; else fail "Register owner: expected 201, got $status — $body"; fi

resp2=$(call POST /auth/register -d "{\"email\":\"$EMAIL2\",\"password\":\"$PASS2\",\"displayName\":\"Member User\"}")
status2=$(http_status "$resp2"); body2=$(http_body "$resp2")
if [ "$status2" = "201" ]; then pass "Register member (201)"; else fail "Register member: expected 201, got $status2 — $body2"; fi
MEMBER_ID=$(echo "$body2" | jq -r '.id')

# ── 2. Log in ─────────────────────────────────────────────────────────────
section "2. Log in"

resp=$(call POST /auth/login -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
status=$(http_status "$resp"); body=$(http_body "$resp")
if [ "$status" = "200" ]; then pass "Login (200)"; else fail "Login: expected 200, got $status — $body"; fi

# ── 3. Authenticated /me ──────────────────────────────────────────────────
section "3. /me check"

resp=$(call GET /auth/me)
status=$(http_status "$resp"); body=$(http_body "$resp")
me_email=$(echo "$body" | jq -r '.email' 2>/dev/null || echo "")
if [ "$status" = "200" ] && [ "$me_email" = "$EMAIL" ]; then
  pass "/me returns authenticated user (200)"
else
  fail "/me: expected 200 + email=$EMAIL, got $status — $body"
fi

# ── 4. Create a project ───────────────────────────────────────────────────
section "4. Create project"

resp=$(call POST /projects -d '{"name":"Acceptance Project","description":"E2E test project"}')
status=$(http_status "$resp"); body=$(http_body "$resp")
if [ "$status" = "201" ]; then
  PROJECT_ID=$(echo "$body" | jq -r '.id')
  pass "Create project (201) — id=$PROJECT_ID"
else
  fail "Create project: expected 201, got $status — $body"
  exit 1  # Cannot continue without a project
fi

# ── 5. Add a member ───────────────────────────────────────────────────────
section "5. Add member"

resp=$(call POST /projects/$PROJECT_ID/members -d "{\"userId\":$MEMBER_ID,\"role\":\"member\"}")
status=$(http_status "$resp")
if [ "$status" = "201" ]; then pass "Add member (201)"; else fail "Add member: expected 201, got $status"; fi

# ── 6. Create a task ──────────────────────────────────────────────────────
section "6. Create task"

resp=$(call POST /projects/$PROJECT_ID/tasks -d '{"title":"Acceptance Task","priority":"high"}')
status=$(http_status "$resp"); body=$(http_body "$resp")
if [ "$status" = "201" ]; then
  TASK_ID=$(echo "$body" | jq -r '.id')
  TASK_STATUS=$(echo "$body" | jq -r '.status')
  pass "Create task (201) — id=$TASK_ID status=$TASK_STATUS"
else
  fail "Create task: expected 201, got $status — $body"
  exit 1
fi

# ── 7. Update task status ─────────────────────────────────────────────────
section "7. Update task status"

resp=$(call PATCH /tasks/$TASK_ID/status -d '{"status":"IN_PROGRESS"}')
status=$(http_status "$resp"); body=$(http_body "$resp")
new_status=$(echo "$body" | jq -r '.status' 2>/dev/null || echo "")
if [ "$status" = "200" ] && [ "$new_status" = "in_progress" ]; then
  pass "Update task status → in_progress (200)"
else
  fail "Update task status: expected 200 + status=in_progress, got $status — $body"
fi

# ── 8. Filter tasks by status ─────────────────────────────────────────────
section "8. Filter tasks"

resp=$(call GET "/projects/$PROJECT_ID/tasks?status=in_progress")
status=$(http_status "$resp"); body=$(http_body "$resp")
count=$(echo "$body" | jq -r '.totalElements' 2>/dev/null || echo "0")
if [ "$status" = "200" ] && [ "$count" -ge "1" ]; then
  pass "Filter tasks by status=in_progress (200) — $count result(s)"
else
  fail "Filter tasks: expected 200 + ≥1 result, got $status totalElements=$count"
fi

# ── 9. Verify persistence (re-fetch task) ────────────────────────────────
section "9. Persistence check"

resp=$(call GET /tasks/$TASK_ID)
status=$(http_status "$resp"); body=$(http_body "$resp")
persisted_status=$(echo "$body" | jq -r '.status' 2>/dev/null || echo "")
if [ "$status" = "200" ] && [ "$persisted_status" = "in_progress" ]; then
  pass "Task persisted with status=in_progress after re-fetch (200)"
else
  fail "Persistence: expected 200 + status=in_progress, got $status — $body"
fi

# ── 10. Unauthenticated access is rejected ───────────────────────────────
section "10. Authorization"

# Call without the session cookie
resp=$(curl -s -w "\n%{http_code}" -X GET "$BASE/projects" -H "Content-Type: application/json")
status=$(http_status "$resp")
if [ "$status" = "401" ]; then
  pass "Unauthenticated GET /projects → 401"
else
  fail "Unauthenticated access: expected 401, got $status"
fi

# ── 11. Duplicate email rejected ─────────────────────────────────────────
section "11. Duplicate registration"

resp=$(call POST /auth/register -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"displayName\":\"Dup\"}")
status=$(http_status "$resp")
if [ "$status" = "409" ]; then pass "Duplicate email → 409"; else fail "Duplicate email: expected 409, got $status"; fi

# ── Cleanup ───────────────────────────────────────────────────────────────
rm -f "$TMPJAR"

# ── Summary ───────────────────────────────────────────────────────────────
echo
if [ "$FAIL" = "0" ]; then
  echo "✓ All acceptance checks passed."
else
  echo "✗ One or more acceptance checks FAILED." >&2
  exit 1
fi
