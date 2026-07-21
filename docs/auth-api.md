# Authentication API

## Local test account

Run `sql/02-seed-local-admin.sql` after the schema scripts.

- Username: `admin`
- Password: `123456`

The database stores a BCrypt hash. This account is for local development only and must be changed before deployment.

## Login

`POST /api/v1/auth/login` is public at Gateway.

```json
{
  "username": "admin",
  "password": "123456"
}
```

The response contains `tokenType`, `accessToken`, `expiresIn`, and the current user's roles and permissions.

## Current user

`GET /api/v1/users/me` requires this header:

```text
Authorization: Bearer <accessToken>
```

Gateway validates the signature and expiry, removes untrusted identity headers, and forwards trusted `X-User-Id`, `X-Username`, `X-Roles`, and `X-Permissions` headers to the user service.
