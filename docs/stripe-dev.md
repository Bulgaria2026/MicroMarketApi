# Stripe setup for local development

The checkout flow uses Stripe Checkout Sessions. Locally, the API calls Stripe with your **test-mode** secret key to create sessions, and the Stripe CLI forwards webhook events back to the app so orders transition to `PAID` after a test purchase.

Everything below runs in Stripe's test mode. No bank details, no identity verification, no charges.

---

## What you'll have at the end

- `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` set in your `.env`
- `stripe listen` running in a terminal, forwarding events to `localhost:8080/api/v1/webhooks/stripe`
- Any checkout you trigger locally shows up in your Stripe dashboard

You need to repeat **step 3 (start the listener)** each time you start a dev session. The rest is one-time setup.

---

## 1. Create a Stripe account

Go to <https://dashboard.stripe.com/register> and sign up with an email and password.

You **don't** need to activate the account or provide any business/identity information. An unactivated account stays permanently in test mode, which is exactly what we want for development. Test-mode keys begin with `sk_test_…` or `pk_test_…`.

## 2. Copy the secret key into `.env`

From <https://dashboard.stripe.com/test/apikeys>, copy the **Secret key** (starts with `sk_test_`).

If you haven't already, create your env file:

```bash
cp .env.example .env
```

Then set:

```properties
STRIPE_SECRET_KEY=sk_test_paste_yours_here
```

> **Never commit a real key.** `.env` is gitignored; only `.env.example` (with empty values) is tracked.

## 3. Install the Stripe CLI

The CLI tunnels webhook events from Stripe into your local API — without it, orders never transition past `PENDING_PAYMENT` because the webhook can't reach `localhost`.

Install instructions for all platforms: <https://github.com/stripe/stripe-cli#installation>

Quick reference:

- **macOS:** `brew install stripe/stripe-cli/stripe`
- **Linux (Debian/Ubuntu):** follow the apt instructions on the install page
- **Windows:** `scoop install stripe`
- **Anywhere:** download a release binary from <https://github.com/stripe/stripe-cli/releases>

Verify the install:

```bash
stripe --version
```

## 4. Log the CLI into your Stripe account

```bash
stripe login
```

This prints a pairing code and opens your browser. Confirm the code matches, click **Allow access**, and return to the terminal. The CLI stores a test-mode API key at `~/.config/stripe/config.toml` and uses it for subsequent commands.

You only need to do this once per machine.

## 5. Start the webhook listener

In a terminal you'll keep open during development:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

The CLI prints a banner similar to:

```text
Ready! You are using Stripe API Version [2026-03-25.dahlia]. Your webhook signing secret is whsec_…
```

## 6. Copy the webhook signing secret into `.env`

Grab the `whsec_…` value from step 5 and paste it into `.env`:

```properties
STRIPE_WEBHOOK_SECRET=whsec_abc123…
```

> This secret is **specific to this CLI session**. If you stop `stripe listen` and start it again, the CLI reuses the same secret, so you only need to paste it once. On a different machine, or after `stripe logout`, expect a new secret.

Your `.env` should now match the shape of `.env.example` with both Stripe values filled in:

```properties
STRIPE_SECRET_KEY=sk_test_…
STRIPE_WEBHOOK_SECRET=whsec_…
STRIPE_SUCCESS_URL=http://localhost:5173/checkout/success?session_id={CHECKOUT_SESSION_ID}
STRIPE_CANCEL_URL=http://localhost:5173/checkout/cancel
STRIPE_SESSION_EXPIRATION_MINUTES=30
```

## 7. Start the API

With the listener still running, start the app:

```bash
./gradlew bootRun
```

To exercise the flow, `POST /api/v1/order` (see Swagger UI at <http://localhost:8080/api/v1/swagger-ui/index.html>). Follow the returned Checkout URL, pay with a Stripe test card — the universal one is `4242 4242 4242 4242`, any future expiry, any CVC — and watch:

- the `stripe listen` terminal log the `checkout.session.completed` event
- the API log the webhook acceptance
- the order transition to `PAID` (e.g. via `GET /api/v1/checkout/sessions/{sessionId}/status`)
- the transaction appear under **Payments** in <https://dashboard.stripe.com/test/payments>

---

## Troubleshooting

**`401 Unauthorized` from the API on webhook receipt.**
The `STRIPE_WEBHOOK_SECRET` in `.env` doesn't match what `stripe listen` printed. Re-copy it and restart the API.

**Order stays `PENDING_PAYMENT` after paying.**
`stripe listen` isn't running, or it's forwarding to the wrong URL. Confirm the target is exactly `localhost:8080/api/v1/webhooks/stripe` (note the `/api/v1` context path).

**`stripe login` fails to open a browser.**
Copy the URL it prints and open it manually, or run `stripe login --interactive` and paste the displayed code on the Stripe authorization page.
