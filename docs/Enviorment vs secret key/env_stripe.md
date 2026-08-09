# Environment Provider 5: Stripe Environment Variables (`env_stripe.md`)

লোকাল `.env` ফাইল থেকে প্রাপ্ত সকল অরিজিনাল Stripe Payment Credentials-এর তালিকা:

| No. | Real Environment Variable Key | Real Scanned Value / Pattern | Status / Usage |
| :--- | :--- | :--- | :--- |
| 1 | `STRIPE_PUBLISHABLE_KEY` | `mk_1Tt72fRubqzVH9crTlBv7xeG` | Stripe Client-side Public Key |
| 2 | `STRIPE_API_KEY` | `mk_1Tt7F5RubqzVH9crsUEBCxVy` | Stripe Secret API Key |
| 3 | `STRIPE_WEBHOOK_SECRET` | `whsec_8o7uDFhiHJI9r4nIVQIRLHpaZt86844j` | Stripe Event Webhook Signing Secret |
| 4 | `CHECKOUT_BASE_URL` | `https://supremeai.onrender.com` | Payment Redirect Checkout Base URL |
