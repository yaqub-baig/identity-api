# Subscription & Payment Frontend Integration Guide

## Overview

This document provides frontend integration details for the subscription and payment system in the identity-api. The system supports multi-dashboard subscriptions (GARAGE, OTHER_SERVICE) with PayBy payment gateway integration.

---

## Base URL

```
Production: https://your-domain.com/api/v1
Local: http://localhost:8010/api/v1
```

## Authentication

All authenticated endpoints require the header:
```
X-Auto-Adda-Id: {userId}
```

---

## Subscription Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SUBSCRIPTION PAYMENT FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. User views available plans                                               │
│     GET /subscriptions/plans/active                                          │
│                                                                              │
│  2. User selects plan and creates subscription                               │
│     POST /subscriptions/create                                               │
│     → Returns subscription with status: PENDING                              │
│                                                                              │
│  3. User initiates payment                                                   │
│     POST /payment/initiate                                                   │
│     → Returns QR code image + payment URL                                    │
│                                                                              │
│  4. User completes payment                                                   │
│     - Scan QR code OR                                                        │
│     - Click payment URL → PayBy page                                         │
│                                                                              │
│  5. After payment                                                            │
│     - User redirected to success/failure URL                                 │
│     - Subscription auto-activated if successful                              │
│                                                                              │
│  6. Check subscription status                                                │
│     GET /subscriptions/status                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Get Active Plans (Public)

Fetch available subscription plans for user to choose from.

**Endpoint:** `GET /subscriptions/plans/active`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Monthly Plan",
    "durationMonths": 1,
    "price": 99.00,
    "description": "Monthly subscription for dashboard access",
    "applicableTo": "ALL",
    "isActive": true,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  },
  {
    "id": 2,
    "name": "Semi-Annual Plan",
    "durationMonths": 6,
    "price": 499.00,
    "description": "6-month subscription (16% savings)",
    "applicableTo": "ALL",
    "isActive": true,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  },
  {
    "id": 3,
    "name": "Annual Plan",
    "durationMonths": 12,
    "price": 899.00,
    "description": "12-month subscription (25% savings)",
    "applicableTo": "ALL",
    "isActive": true,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  }
]
```

---

### 2. Get Plans by Entity Type

Fetch plans applicable to a specific entity type.

**Endpoint:** `GET /subscriptions/plans/active/{entityType}`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| entityType | String | `GARAGE` or `OTHER_SERVICE` |

**Example:** `GET /subscriptions/plans/active/GARAGE`

---

### 3. Check Subscription Status

Check current subscription status for an entity.

**Endpoint:** `GET /subscriptions/status`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| entityType | String | Yes | `GARAGE` or `OTHER_SERVICE` |
| entityId | Integer | Yes | The garage ID or other service ID |

**Example:** `GET /subscriptions/status?entityType=GARAGE&entityId=123`

**Response:**
```json
{
  "entityId": 123,
  "entityType": "GARAGE",
  "hasActiveSubscription": true,
  "status": "ACTIVE",
  "planName": "Monthly Plan",
  "isTrial": false,
  "startDate": "2024-01-15",
  "endDate": "2024-02-15",
  "daysRemaining": 25,
  "isExpiringSoon": false,
  "message": "Your subscription is active. 25 day(s) remaining."
}
```

**Status Values:**
| Status | Description |
|--------|-------------|
| `NO_SUBSCRIPTION` | No subscription exists |
| `PENDING` | Subscription created, awaiting payment |
| `ACTIVE` | Subscription is active |
| `EXPIRED` | Subscription has expired |
| `CANCELLED` | Subscription was cancelled |

**Message Examples:**
- `"No subscription found. Please subscribe to access the dashboard."`
- `"Your subscription is pending activation. Please complete payment."`
- `"Your subscription is active. 25 day(s) remaining."`
- `"Your subscription expires in 5 day(s). Please renew soon."`
- `"Your free trial is active. 20 day(s) remaining."`
- `"Your free trial has expired. Please subscribe to continue."`

---

### 4. Create Subscription (Self-Service)

Create a new subscription for payment.

**Endpoint:** `POST /subscriptions/create`

**Headers:**
```
X-Auto-Adda-Id: {userId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "entityId": 123,
  "entityType": "GARAGE",
  "planId": 1
}
```

**Request Parameters:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| entityId | Integer | Yes | Garage ID or Other Service ID |
| entityType | String | Yes | `GARAGE` or `OTHER_SERVICE` |
| planId | Integer | Yes | Selected plan ID |

**Success Response (201 Created):**
```json
{
  "id": 456,
  "userId": 789,
  "userName": "John Doe",
  "entityId": 123,
  "entityType": "GARAGE",
  "plan": {
    "id": 1,
    "name": "Monthly Plan",
    "durationMonths": 1,
    "price": 99.00,
    "description": "Monthly subscription",
    "applicableTo": "ALL",
    "isActive": true
  },
  "startDate": "2024-01-20",
  "endDate": "2024-02-20",
  "status": "PENDING",
  "paymentStatus": "NOT_INITIATED",
  "activatedBy": null,
  "activatedAt": null,
  "paymentReference": null,
  "paymentAmount": 99.00,
  "notes": "Self-service subscription - awaiting payment",
  "isTrial": false,
  "daysRemaining": 31,
  "isExpiringSoon": false,
  "createdAt": "2024-01-20T14:30:00",
  "updatedAt": "2024-01-20T14:30:00"
}
```

**Error Responses:**
| Code | Message |
|------|---------|
| 400 | "Entity already has an active subscription" |
| 400 | "A pending subscription already exists. Please complete payment or cancel it first." |
| 400 | "Selected plan is not active" |
| 404 | "Plan not found: {planId}" |

---

### 5. Get Pending Subscription

Get the pending subscription that needs payment.

**Endpoint:** `GET /subscriptions/pending`

**Headers:**
```
X-Auto-Adda-Id: {userId}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| entityType | String | Yes | `GARAGE` or `OTHER_SERVICE` |
| entityId | Integer | Yes | The entity ID |

**Example:** `GET /subscriptions/pending?entityType=GARAGE&entityId=123`

**Response:** Same as Create Subscription response

---

### 6. Initiate Payment

Start the payment process and get QR code/payment URL.

**Endpoint:** `POST /payment/initiate`

**Headers:**
```
X-Auto-Adda-Id: {userId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "subscriptionId": 456,
  "mobileNumber": "+971501234567",
  "sendSms": false
}
```

**Request Parameters:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| subscriptionId | Integer | Yes | The pending subscription ID |
| mobileNumber | String | No | Mobile number for SMS notification |
| sendSms | Boolean | No | Send payment link via SMS |

**Success Response (200 OK):**
```json
{
  "subscriptionId": 456,
  "transactionId": "TXN123456789",
  "qrCodeImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "paymentUrl": "https://payby.com/pay/TXN123456789",
  "amount": 99.00,
  "currency": "AED",
  "expiryMinutes": 15,
  "status": "INITIATED",
  "message": "Scan QR code or click payment link to complete payment"
}
```

**Error Responses:**
| Code | Message |
|------|---------|
| 400 | "Only PENDING subscriptions can be paid for" |
| 400 | "Payment already initiated. Please complete or wait for expiry." |
| 400 | "Payment already completed for this subscription." |
| 403 | "You are not authorized to pay for this subscription" |
| 404 | "Subscription not found: {id}" |
| 502 | "Payment processing failed: {error}" |

---

### 7. Check Payment Status

Check the current payment status for a subscription.

**Endpoint:** `GET /payment/status/{subscriptionId}`

**Headers:**
```
X-Auto-Adda-Id: {userId}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| subscriptionId | Integer | The subscription ID |

**Example:** `GET /payment/status/456`

**Response:**
```json
{
  "subscriptionId": 456,
  "paymentStatus": "SUCCESS",
  "message": "Payment completed successfully"
}
```

**Payment Status Values:**
| Status | Description | User Action |
|--------|-------------|-------------|
| `NOT_INITIATED` | Payment not started | Show "Pay Now" button |
| `PENDING` | Payment in progress | Show "Complete Payment" / QR code |
| `SUCCESS` | Payment completed | Show success, subscription active |
| `FAILED` | Payment failed | Show "Retry Payment" button |
| `CANCELLED` | Payment cancelled | Show "Retry Payment" button |
| `EXPIRED` | Payment link expired | Show "Retry Payment" button |

---

### 8. Get Subscription History

Get all subscriptions for an entity.

**Endpoint:** `GET /subscriptions/history`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| entityType | String | Yes | `GARAGE` or `OTHER_SERVICE` |
| entityId | Integer | Yes | The entity ID |

**Example:** `GET /subscriptions/history?entityType=GARAGE&entityId=123`

**Response:**
```json
[
  {
    "id": 456,
    "userId": 789,
    "entityId": 123,
    "entityType": "GARAGE",
    "plan": { ... },
    "startDate": "2024-01-20",
    "endDate": "2024-02-20",
    "status": "ACTIVE",
    "paymentStatus": "SUCCESS",
    "isTrial": false,
    "daysRemaining": 25,
    "createdAt": "2024-01-20T14:30:00"
  },
  {
    "id": 400,
    "userId": 789,
    "entityId": 123,
    "entityType": "GARAGE",
    "plan": { ... },
    "startDate": "2023-12-20",
    "endDate": "2024-01-20",
    "status": "EXPIRED",
    "paymentStatus": "SUCCESS",
    "isTrial": true,
    "daysRemaining": 0,
    "createdAt": "2023-12-20T10:00:00"
  }
]
```

---

### 9. Check Dashboard Access

Quick check if entity has active subscription.

**Endpoint:** `GET /subscriptions/check`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| entityType | String | Yes | `GARAGE` or `OTHER_SERVICE` |
| entityId | Integer | Yes | The entity ID |

**Example:** `GET /subscriptions/check?entityType=GARAGE&entityId=123`

**Response:**
```json
{
  "hasActiveSubscription": true
}
```

---

## Frontend Implementation Examples

### React/TypeScript Example

```typescript
// types.ts
interface SubscriptionPlan {
  id: number;
  name: string;
  durationMonths: number;
  price: number;
  description: string;
  applicableTo: string;
  isActive: boolean;
}

interface SubscriptionStatus {
  entityId: number;
  entityType: string;
  hasActiveSubscription: boolean;
  status: string;
  planName: string;
  isTrial: boolean;
  startDate: string;
  endDate: string;
  daysRemaining: number;
  isExpiringSoon: boolean;
  message: string;
}

interface PaymentInitiation {
  subscriptionId: number;
  transactionId: string;
  qrCodeImage: string;
  paymentUrl: string;
  amount: number;
  currency: string;
  expiryMinutes: number;
  status: string;
  message: string;
}

// api.ts
const API_BASE = 'http://localhost:8010/api/v1';

const getHeaders = (userId: number) => ({
  'Content-Type': 'application/json',
  'X-Auto-Adda-Id': userId.toString(),
});

// Get active plans
export const getActivePlans = async (): Promise<SubscriptionPlan[]> => {
  const response = await fetch(`${API_BASE}/subscriptions/plans/active`);
  return response.json();
};

// Check subscription status
export const getSubscriptionStatus = async (
  entityType: string,
  entityId: number
): Promise<SubscriptionStatus> => {
  const response = await fetch(
    `${API_BASE}/subscriptions/status?entityType=${entityType}&entityId=${entityId}`
  );
  return response.json();
};

// Create subscription
export const createSubscription = async (
  userId: number,
  entityId: number,
  entityType: string,
  planId: number
) => {
  const response = await fetch(`${API_BASE}/subscriptions/create`, {
    method: 'POST',
    headers: getHeaders(userId),
    body: JSON.stringify({ entityId, entityType, planId }),
  });
  return response.json();
};

// Initiate payment
export const initiatePayment = async (
  userId: number,
  subscriptionId: number
): Promise<PaymentInitiation> => {
  const response = await fetch(`${API_BASE}/payment/initiate`, {
    method: 'POST',
    headers: getHeaders(userId),
    body: JSON.stringify({ subscriptionId }),
  });
  return response.json();
};

// Check payment status
export const getPaymentStatus = async (
  userId: number,
  subscriptionId: number
) => {
  const response = await fetch(
    `${API_BASE}/payment/status/${subscriptionId}`,
    { headers: getHeaders(userId) }
  );
  return response.json();
};
```

### Subscription Flow Component

```tsx
// SubscriptionFlow.tsx
import React, { useState, useEffect } from 'react';
import {
  getActivePlans,
  getSubscriptionStatus,
  createSubscription,
  initiatePayment
} from './api';

interface Props {
  userId: number;
  entityId: number;
  entityType: 'GARAGE' | 'OTHER_SERVICE';
}

const SubscriptionFlow: React.FC<Props> = ({ userId, entityId, entityType }) => {
  const [step, setStep] = useState<'check' | 'plans' | 'payment' | 'success'>('check');
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [status, setStatus] = useState<SubscriptionStatus | null>(null);
  const [paymentData, setPaymentData] = useState<PaymentInitiation | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkStatus();
  }, []);

  const checkStatus = async () => {
    setLoading(true);
    const result = await getSubscriptionStatus(entityType, entityId);
    setStatus(result);

    if (result.hasActiveSubscription) {
      setStep('success');
    } else {
      loadPlans();
    }
    setLoading(false);
  };

  const loadPlans = async () => {
    const activePlans = await getActivePlans();
    setPlans(activePlans);
    setStep('plans');
  };

  const handleSelectPlan = async (planId: number) => {
    setLoading(true);
    try {
      // Create subscription
      const subscription = await createSubscription(userId, entityId, entityType, planId);

      // Initiate payment
      const payment = await initiatePayment(userId, subscription.id);
      setPaymentData(payment);
      setStep('payment');
    } catch (error) {
      console.error('Error:', error);
    }
    setLoading(false);
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      {step === 'plans' && (
        <div className="plans-grid">
          <h2>Choose a Plan</h2>
          {plans.map(plan => (
            <div key={plan.id} className="plan-card">
              <h3>{plan.name}</h3>
              <p className="price">AED {plan.price}</p>
              <p>{plan.description}</p>
              <p>{plan.durationMonths} month(s)</p>
              <button onClick={() => handleSelectPlan(plan.id)}>
                Select Plan
              </button>
            </div>
          ))}
        </div>
      )}

      {step === 'payment' && paymentData && (
        <div className="payment-section">
          <h2>Complete Payment</h2>
          <p>Amount: AED {paymentData.amount}</p>
          <p>Expires in: {paymentData.expiryMinutes} minutes</p>

          {/* QR Code */}
          <div className="qr-code">
            <img src={paymentData.qrCodeImage} alt="Payment QR Code" />
          </div>

          {/* Or payment link */}
          <a
            href={paymentData.paymentUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="payment-button"
          >
            Pay Now
          </a>

          <p className="hint">Scan QR code or click the button above</p>
        </div>
      )}

      {step === 'success' && status && (
        <div className="success-section">
          <h2>Subscription Active</h2>
          <p>{status.message}</p>
          <p>Plan: {status.planName}</p>
          <p>Valid until: {status.endDate}</p>
          <p>Days remaining: {status.daysRemaining}</p>
        </div>
      )}
    </div>
  );
};

export default SubscriptionFlow;
```

---

## Redirect URLs

After payment completion, users are redirected to:

| Result | Redirect URL |
|--------|--------------|
| Success | `{frontend-success-url}?subscriptionId={id}` |
| Failure | `{frontend-failure-url}?subscriptionId={id}` |

**Configure these in backend:**
```properties
payment.gateway.frontend-success-url=https://your-app.com/subscription/success
payment.gateway.frontend-failure-url=https://your-app.com/subscription/failed
```

### Success Page Example

```tsx
// SubscriptionSuccess.tsx
import { useSearchParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { getPaymentStatus } from './api';

const SubscriptionSuccess = ({ userId }) => {
  const [searchParams] = useSearchParams();
  const subscriptionId = searchParams.get('subscriptionId');
  const [status, setStatus] = useState(null);

  useEffect(() => {
    if (subscriptionId) {
      getPaymentStatus(userId, parseInt(subscriptionId))
        .then(setStatus);
    }
  }, [subscriptionId]);

  return (
    <div className="success-page">
      <h1>Payment Successful!</h1>
      <p>Your subscription has been activated.</p>
      {status && (
        <p>Status: {status.paymentStatus}</p>
      )}
      <a href="/dashboard">Go to Dashboard</a>
    </div>
  );
};
```

---

## Error Handling

### Common Error Response Format

```json
{
  "status": "failed",
  "code": "400",
  "message": "Error description here"
}
```

### Error Codes

| HTTP Code | Description | Frontend Action |
|-----------|-------------|-----------------|
| 400 | Bad Request | Show error message to user |
| 401 | Unauthorized | Redirect to login |
| 403 | Forbidden | Show "Not authorized" message |
| 404 | Not Found | Show "Not found" message |
| 502 | Bad Gateway | Show "Payment service unavailable, try later" |
| 500 | Server Error | Show generic error, retry later |

---

## Polling for Payment Status

When user is on payment page, poll for status updates:

```typescript
const pollPaymentStatus = (
  userId: number,
  subscriptionId: number,
  onSuccess: () => void,
  onFailure: () => void
) => {
  const interval = setInterval(async () => {
    const result = await getPaymentStatus(userId, subscriptionId);

    if (result.paymentStatus === 'SUCCESS') {
      clearInterval(interval);
      onSuccess();
    } else if (['FAILED', 'CANCELLED', 'EXPIRED'].includes(result.paymentStatus)) {
      clearInterval(interval);
      onFailure();
    }
  }, 5000); // Poll every 5 seconds

  // Stop polling after 20 minutes
  setTimeout(() => clearInterval(interval), 20 * 60 * 1000);

  return () => clearInterval(interval);
};
```

---

## Subscription Status UI States

| Status | UI Display |
|--------|------------|
| No subscription | Show plans, "Subscribe Now" CTA |
| Pending | Show "Complete Payment" with payment link |
| Active | Show dashboard, days remaining badge |
| Active + Expiring Soon | Show warning banner "Expires in X days" |
| Expired | Block dashboard, show "Renew Now" |
| Trial Active | Show "Trial: X days left" badge |
| Trial Expired | Show "Trial ended, Subscribe now" |

---

## Testing

### Test Scenarios

1. **New User Flow:**
   - Check status → No subscription
   - View plans → Select plan
   - Create subscription → Initiate payment
   - Complete payment → Subscription active

2. **Renewal Flow:**
   - Status shows expiring soon
   - Create new subscription
   - Pay and activate

3. **Failed Payment:**
   - Initiate payment → Close without paying
   - Payment status = EXPIRED after 15 mins
   - Retry with new payment initiation

4. **Trial User:**
   - Check if trial available
   - Activate trial
   - After trial expires → Subscribe prompt

---

## Contact

For API issues or questions, contact the backend team.
