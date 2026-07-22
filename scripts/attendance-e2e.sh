#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
OA_USERNAME="${OA_USERNAME:-admin}"
TOKEN="${OA_TOKEN:-}"
TRACE_PREFIX="${TRACE_PREFIX:-attendance-e2e-$(date +%s)}"
TODAY="$(date +%Y-%m-%d)"
MONTH="$(date +%Y-%m)"
RESPONSE_BODY=""

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

request() {
    local name="$1"
    local method="$2"
    local path="$3"
    local allowed_codes="$4"
    local payload="${5:-}"
    local trace_id="${TRACE_PREFIX}-${name}"
    local args=(-sS -X "$method" "${BASE_URL}${path}" -H "X-Trace-Id: ${trace_id}")

    if [[ -n "$TOKEN" ]]; then
        args+=(-H "Authorization: Bearer ${TOKEN}")
    fi
    if [[ -n "$payload" ]]; then
        args+=(-H "Content-Type: application/json" --data "$payload")
    fi

    local raw body http_status code returned_trace
    raw="$(curl "${args[@]}" -w $'\n%{http_code}')"
    body="${raw%$'\n'*}"
    http_status="${raw##*$'\n'}"
    code="$(jq -r '.code // empty' <<<"$body")"
    returned_trace="$(jq -r '.traceId // empty' <<<"$body")"

    case ",${allowed_codes}," in
        *",${code},"*) ;;
        *) echo "${name} failed: HTTP ${http_status}, code=${code}, body=${body}" >&2; exit 1 ;;
    esac
    [[ "$returned_trace" == "$trace_id" ]] || {
        echo "${name} trace mismatch: expected ${trace_id}, got ${returned_trace}" >&2
        exit 1
    }
    echo "${name}: HTTP ${http_status}, code=${code}, traceId=${returned_trace}"
    RESPONSE_BODY="$body"
}

if [[ -z "$TOKEN" ]]; then
    : "${OA_PASSWORD:?Set OA_PASSWORD or provide OA_TOKEN}"
    login_payload="$(jq -nc --arg username "$OA_USERNAME" --arg password "$OA_PASSWORD" \
        '{username:$username,password:$password}')"
    request "login" POST "/api/v1/auth/login" "0" "$login_payload"
    TOKEN="$(jq -er '.data.accessToken' <<<"$RESPONSE_BODY")"
fi

request "check-in" POST "/api/v1/attendance/check-in" "0,B0110"
request "today" GET "/api/v1/attendance/today" "0"
request "check-out" POST "/api/v1/attendance/check-out" "0,B0111"
request "records" GET "/api/v1/attendance/records?startDate=${TODAY}&endDate=${TODAY}&page=1&size=20" "0"
jq -e '.data.total >= 1' <<<"$RESPONSE_BODY" >/dev/null || {
    echo "records did not contain today's attendance row" >&2
    exit 1
}
request "monthly" GET "/api/v1/attendance/statistics/monthly?month=${MONTH}" "0"
jq -e '.data.totalRecords >= 1' <<<"$RESPONSE_BODY" >/dev/null || {
    echo "monthly statistics did not include today's attendance row" >&2
    exit 1
}
request "summary" GET "/api/v1/attendance/statistics/summary?startDate=${TODAY}&endDate=${TODAY}" "0"

echo "Attendance Gateway end-to-end verification passed."
