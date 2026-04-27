# Resend setup for local development

Emails are sent via [Resend](https://resend.com). Locally you can use Resend's sandbox from-address without verifying a domain, but it only delivers to the email you signed up with — that's the anti-spam guard, same as every email provider.

---

## What you'll have at the end

- `RESEND_API_KEY` set in your `.env`
- `MAIL_ENABLED=true` in your `.env`
- Your Resend-account email used as the MicroMarket account email, so emails actually arrive

---

## 1. Create a Resend account

Sign up at <https://resend.com/signup> with the email you want to receive dev mails on. No domain needed in test mode — Resend gives every account the sandbox from-address `onboarding@resend.dev` out of the box.

## 2. Copy the API key into `.env`

From <https://resend.com/api-keys>, create a key and copy the `re_…` value.

If you haven't already:

```bash
cp .env.example .env
```

Then set:

```properties
MAIL_ENABLED=true
RESEND_API_KEY=re_paste_yours_here
MAIL_FROM_ADDRESS=onboarding@resend.dev
```

> **Never commit a real key.** `.env` is gitignored; only `.env.example` is tracked.

## 3. Use the same email for your MicroMarket account

Resend's sandbox only delivers to the email you registered the Resend account with. Whenever you trigger an email flow (e.g. placing an order), use a MicroMarket account or guest checkout with **that same email** — the mail will land in that inbox.

## 4. Start the API

```bash
./gradlew bootRun
```

Trigger any mail-sending flow and the email should appear within a few seconds. You can also watch every send in the [Resend logs dashboard](https://resend.com/emails).

---

## Troubleshooting

**No email arrives, no errors in the log.**
`MAIL_ENABLED` is probably still `false` in `.env`. The `MailSender` logs `Mail disabled; skipping send of …` when that's the case.

**`422` / "You can only send testing emails to your own email address" in the API log.**
You're trying to send to a different address than your Resend account email. Use the account email, or [verify a domain](https://resend.com/domains) and set `MAIL_FROM_ADDRESS=no-reply@yourdomain.com`.

**`401` / "API key is invalid".**
`RESEND_API_KEY` in `.env` doesn't match the one in the dashboard. Regenerate or re-copy.
