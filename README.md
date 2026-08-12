# ClickKart Captcha Service

Self-hosted image CAPTCHA — bot detection for the platform's abuse-prone public endpoints, with
**no third-party provider** and no API keys to obtain.

- **Port:** `8084`
- **Datastore:** Redis (challenges only — deliberately not Postgres)
- **Callers:** browsers (via the Gateway) for challenges; Auth Service (server-to-server) for verification

## How it works

1. Client calls `POST /api/v1/captcha/challenge` and receives a `challengeId` plus a base64 PNG.
2. Only the **SHA-256 hash** of the answer is stored in Redis, under a short TTL (120s default).
3. Auth Service calls `POST /api/v1/captcha/verify` with the id and the user's typed answer.
4. The challenge is consumed on the **first** verification attempt regardless of outcome — one
   guess per issued image.

Answers are compared case-insensitively after trimming. The alphabet deliberately excludes
visually ambiguous characters (`0`/`O`, `1`/`I`/`L`) — a CAPTCHA a human can't reliably read is
worse than useless.

## Endpoints

| Method | Path | Exposure |
|---|---|---|
| `POST` | `/api/v1/captcha/challenge` | **Public** — Gateway-routed, browser-facing |
| `POST` | `/api/v1/captcha/verify` | **Internal only** — never Gateway-routed |

`/verify` is intentionally excluded from the Gateway's routes so answers can only be checked
server-to-server, by Auth Service's Feign client.

## Why Redis and not Postgres

A challenge is ephemeral by design — short TTL, deleted on first use. There is no "restart loses
data" concern the way there is for the platform's Postgres-backed services, and storing only the
answer hash means a Redis compromise doesn't hand an attacker a bank of solved answers.

## Rate limiting

`POST /challenge` is rate-limited per IP (20 requests / 60s default). Without it an attacker
could mass-generate challenges to burn CPU on image rendering or flood Redis — the one-time-use
design protects a single issued challenge, not the generation endpoint itself.

The IP resolver only trusts `X-Forwarded-For` from a configured trusted-proxy CIDR; otherwise it
uses the immediate socket address. A client reaching this service directly cannot spoof the
header to evade the limit.

## Failure behaviour

Fails **closed**. If Redis is unreachable, challenge generation and verification both return
`503`, and Auth Service's circuit breaker surfaces that as a `503` on registration/password
reset. Bot protection never silently degrades into "allow everything".

## Security posture — read this

This is a lightweight, credential-free implementation. It stops naive scripted abuse. It is
**meaningfully weaker** than a commercial CAPTCHA against a determined attacker with OCR or a
solving service. If you need strong guarantees, swap in reCAPTCHA/hCaptcha — the Feign contract
in Auth Service is provider-agnostic, so only this service's internals change.

## Configuration

| Variable | Required in | Notes |
|---|---|---|
| `CAPTCHA_REDIS_HOST` | prod | Managed cache endpoint; separate instance from the Gateway's |
| `CAPTCHA_REDIS_PASSWORD` | test/qa/prod | |
| `EUREKA_DASHBOARD_USERNAME` / `_PASSWORD` | test/qa/prod | |
| `CAPTCHA_SERVICE_HOSTNAME` | prod | Eureka advertise address |
| `captcha.ttl-seconds` | — | Default `120` |
| `captcha.code-length` | — | Default `6` |

## Running locally

```bash
docker compose -f docker-compose.dev-infra.yml -f docker-compose.app-tier.yml up -d

# get a challenge (through the Gateway, as a browser would)
curl -X POST http://localhost:8080/api/v1/captcha/challenge
```

Decode `imageBase64` to a PNG to read the code, then pass `captchaChallengeId` and
`captchaAnswer` to `/api/v1/auth/register` or `/api/v1/auth/forgot-password`.

## Build

```bash
mvn -B verify
```

## Related

- [clickkart-platform](https://github.com/kripals1199/clickkart-platform) — architecture, local setup
- [clickkart-auth-service](https://github.com/kripals1199/clickkart-auth-service) — consumes verification
