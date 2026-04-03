# Public API HTTPS Runbook

This runbook documents the HTTPS/public-domain setup used for the published API so we can recover the configuration later without re-discovering it from scratch.

## Goal

Expose the backend publicly under a stable hostname:

- frontend: `https://oryzem.com`
- API: `https://api.oryzem.com`

The browser should no longer call the raw ALB hostname directly.

## Problem We Solved

The published frontend was originally calling the API over the raw ALB endpoint and the browser was failing during the CORS preflight / HTTPS layer.

The final fix was to:

1. create a public API hostname
2. attach a valid ACM certificate to the ALB
3. open ALB listener port `443`
4. keep the backend service unchanged behind the same target group

## Final AWS Resources

- ALB ARN: `arn:aws:elasticloadbalancing:sa-east-1:439533253319:loadbalancer/app/program-management-system-alb/a9879419e7fcb5bc`
- ALB DNS name: `program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- ALB target group: `arn:aws:elasticloadbalancing:sa-east-1:439533253319:targetgroup/program-management-system-alb-tg/1425d73086a3393d`
- ALB security group: `sg-0707623b7ced46517`
- ACM wildcard certificate ARN: `arn:aws:acm:sa-east-1:439533253319:certificate/b8a6478b-ac67-467b-b911-c2d32c500b88`
- API hostname: `api.oryzem.com`

## DNS Configuration

In Hostinger, the API host was created as:

- Type: `CNAME`
- Host/Name: `api`
- Value/Target: `program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- TTL: `300`

Result:

- `api.oryzem.com` resolves to the ALB DNS name

## ALB Listener Configuration

The ALB should expose:

- `HTTP:80` forwarding to the API target group
- `HTTPS:443` forwarding to the same API target group

The `HTTPS:443` listener should use the wildcard ACM certificate for `*.oryzem.com`.

Security group inbound rules should allow:

- `HTTP 80` from `0.0.0.0/0`
- `HTTPS 443` from `0.0.0.0/0`

## Backend CORS Configuration

The backend already permits the published frontend origin and public auth routes.

Relevant config:

- `src/main/java/com/oryzem/programmanagementsystem/platform/auth/SecurityConfig.java`
- `src/main/resources/application.yaml`

Important settings:

- allowed origin: `https://oryzem.com`
- allowed methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- allowed headers: `Authorization, Content-Type, Accept, Origin, X-Access-Context, X-Correlation-Id`

## Implementation Notes

The repository contains the helper script:

- `scripts/enable-alb-https.ps1`

That script provisions or updates:

- HTTPS listener on `443`
- optional HTTP -> HTTPS redirect on `80`
- ALB certificate attachment
- listener forwarding to the API target group

## Verification Commands

After the setup, these checks should succeed:

```powershell
Resolve-DnsName api.oryzem.com
```

```powershell
curl.exe -i --max-time 20 https://api.oryzem.com/public/ping
```

```powershell
curl.exe -i --max-time 20 https://api.oryzem.com/public/auth/config
```

```powershell
curl.exe -i -X OPTIONS --max-time 20 -H "Origin: https://oryzem.com" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type,accept" https://api.oryzem.com/public/auth/login
```

### Verified Responses

`GET /public/ping`

- `200 OK`
- `{"status":"public-ok", ...}`

`OPTIONS /public/auth/login`

- `200 OK`
- `Access-Control-Allow-Origin: https://oryzem.com`
- `Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS`
- `Access-Control-Allow-Headers: content-type, accept`
- `Access-Control-Allow-Credentials: true`
- `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`

## Historical Troubleshooting Notes

- The raw ALB hostname was initially being used directly by the browser.
- The browser failed before login completed because HTTPS/DNS/CORS were not aligned.
- A wildcard certificate for `*.oryzem.com` was created and issued in ACM.
- The API hostname was moved to `api.oryzem.com`.
- The ALB security group was confirmed to allow `443`.
- The login preflight was validated successfully after the final DNS and listener setup.

## When Updating This Later

If this setup changes, update:

1. this runbook
2. `infra/ecs/README.md`
3. the frontend API base URL configuration
4. any scripts that still assume the raw ALB hostname
