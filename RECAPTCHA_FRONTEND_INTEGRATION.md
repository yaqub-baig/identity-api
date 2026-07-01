# 🤖 Frontend reCAPTCHA Integration Guide - AutoAdda

## 📋 Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [How It Works](#how-it-works)
4. [Implementation - Plain HTML / JS](#implementation---plain-html--js)
5. [Implementation - React](#implementation---react)
6. [API Contract](#api-contract)
7. [Error Handling](#error-handling)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The registration endpoint (`POST /auth/user/signup`) now requires a Google reCAPTCHA token. The backend verifies this token with Google before creating the user or sending an OTP. If verification fails, the request is rejected with a `400` error — no user is created and no OTP is sent.

**Site:** registered at https://www.google.com/recaptcha/admin/site/759125604/setup
**Widget type:** reCAPTCHA v2 ("I'm not a robot" checkbox)

You need the **Site Key** (public, safe to expose in frontend code) from that admin console page. The **Secret Key** stays on the backend only — never put it in frontend code.

---

## Prerequisites

1. Get the **Site Key** from the reCAPTCHA admin console (same page where the site was registered):
   https://www.google.com/recaptcha/admin/site/759125604/setup
2. Add the domain(s) your frontend runs on (e.g. `localhost`, `autoadda.mylstech.com`) to the site's allowed domains list in that console — reCAPTCHA will refuse to render otherwise.

---

## How It Works

```
User fills signup form
        ↓
User completes the reCAPTCHA checkbox widget
        ↓
Google reCAPTCHA JS returns a short-lived token
        ↓
Frontend includes token as "captchaToken" in the signup request body
        ↓
Backend calls Google's siteverify API with the token + secret key
        ↓
Valid → registration proceeds (user created / OTP sent)
Invalid or missing → 400 "Captcha verification failed", registration blocked
```

Notes:
- The token is single-use and expires after ~2 minutes. If the user takes too long to submit the form after solving the captcha, re-render the widget and get a fresh token.
- This applies **only** to `POST /auth/user/signup`. Resend-OTP, forgot-password, sign-in, etc. are unaffected.

---

## Implementation - Plain HTML / JS

### 1. Load the reCAPTCHA script

```html
<script src="https://www.google.com/recaptcha/api.js" async defer></script>
```

### 2. Render the widget in the signup form

```html
<form id="signup-form">
  <input type="text" name="firstName" placeholder="First Name" required />
  <input type="text" name="lastName" placeholder="Last Name" required />
  <input type="email" name="emailId" placeholder="Email" required />
  <input type="text" name="mobileNumber" placeholder="Mobile Number" required />
  <input type="password" name="password" placeholder="Password" required />

  <div class="g-recaptcha" data-sitekey="YOUR_SITE_KEY"></div>

  <button type="submit">Sign Up</button>
</form>
```

### 3. Read the token and submit

```javascript
document.getElementById('signup-form').addEventListener('submit', async (e) => {
  e.preventDefault();

  const captchaToken = grecaptcha.getResponse();
  if (!captchaToken) {
    alert('Please complete the captcha.');
    return;
  }

  const form = e.target;
  const payload = {
    firstName: form.firstName.value,
    lastName: form.lastName.value,
    emailId: form.emailId.value,
    mobileNumber: form.mobileNumber.value,
    password: form.password.value,
    captchaToken,
  };

  const response = await fetch('https://<api-base-url>/auth/user/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  const result = await response.json();

  if (!response.ok) {
    // Reset the widget so the user can retry with a fresh token
    grecaptcha.reset();
    alert(result.message || 'Registration failed');
    return;
  }

  console.log('Registered:', result);
});
```

---

## Implementation - React

### 1. Install

```bash
npm install react-google-recaptcha
```

### 2. Signup component

```jsx
import { useState, useRef } from 'react';
import ReCAPTCHA from 'react-google-recaptcha';

const RECAPTCHA_SITE_KEY = 'YOUR_SITE_KEY';

export default function SignupForm() {
  const recaptchaRef = useRef(null);
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    emailId: '',
    mobileNumber: '',
    password: '',
  });
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const captchaToken = recaptchaRef.current.getValue();
    if (!captchaToken) {
      setError('Please complete the captcha.');
      return;
    }

    try {
      const res = await fetch('https://<api-base-url>/auth/user/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...form, captchaToken }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      // proceed to OTP verification screen
      console.log('Registered:', data);
    } catch (err) {
      setError(err.message);
    } finally {
      // Token is single-use — always reset after a submit attempt
      recaptchaRef.current.reset();
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="firstName" placeholder="First Name" onChange={handleChange} required />
      <input name="lastName" placeholder="Last Name" onChange={handleChange} required />
      <input name="emailId" type="email" placeholder="Email" onChange={handleChange} required />
      <input name="mobileNumber" placeholder="Mobile Number" onChange={handleChange} required />
      <input name="password" type="password" placeholder="Password" onChange={handleChange} required />

      <ReCAPTCHA ref={recaptchaRef} sitekey={RECAPTCHA_SITE_KEY} />

      {error && <p className="error">{error}</p>}
      <button type="submit">Sign Up</button>
    </form>
  );
}
```

---

## API Contract

`POST /auth/user/signup`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "emailId": "john@example.com",
  "mobileNumber": "501234567",
  "password": "SecurePass123",
  "captchaToken": "03AGdBq27..."
}
```

`captchaToken` is **required**. If it's missing, blank, or invalid/expired, the backend returns:

```json
{
  "status": "failed",
  "code": "400",
  "message": "Captcha verification failed"
}
```

Success response is unchanged from the existing signup flow (OTP sent via SMS/Email).

---

## Error Handling

- Always call `grecaptcha.reset()` (plain JS) or `recaptchaRef.current.reset()` (React) after **every** submit attempt — success or failure — since a captcha token can only be used once.
- If the signup form stays open for a while before the user submits, the token may expire before the request is even sent. reCAPTCHA v2 widgets show an expired state automatically and clear the response; re-check `grecaptcha.getResponse()` returns a non-empty value right before submitting.
- Treat a `400` with message `"Captcha verification failed"` as a generic captcha error and prompt the user to retry the widget, rather than showing it as a form validation error tied to a specific field.

---

## Testing

Google provides official test keys that always pass verification, useful for local development without needing a real domain allow-listed:

- Site key: `6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI`
- Secret key: `6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe`

These can be used temporarily on `localhost` while building the UI. Switch to the real site key (and have the backend switch to the real secret key) before testing against staging/production.

---

## Troubleshooting

| Symptom | Likely Cause |
|---|---|
| Widget doesn't render / console error "Invalid domain for site key" | Current domain isn't added to the allowed domains list in the reCAPTCHA admin console |
| Signup always returns `400 Captcha verification failed` even after solving the widget | Token wasn't included in the request body under `captchaToken`, or it was reused after a previous failed submit |
| Works locally but fails in production | Site key/domain mismatch — make sure the production domain is added in the admin console, and that the frontend is using the correct (non-test) site key |
| Backend always rejects, even with a fresh token | Backend `recaptcha.secret` in `application.properties` may still be the placeholder value — confirm with backend team that it's been set to the real secret key |
