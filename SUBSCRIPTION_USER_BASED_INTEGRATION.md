# AutoAdda Platform Subscription - Frontend Integration Guide (v2.0)

## What's Changed? (Migration from v1.0)

| Aspect | Old (v1.0) | New (v2.0) |
|--------|------------|------------|
| **Subscription Scope** | Per-entity (GARAGE, OTHER_SERVICE) | **Per-user** (unlocks ALL dashboards) |
| **Trial Duration** | 30 days | **1 calendar month** |
| **Plans** | Flexible (any duration/price) | **3 fixed: Monthly, Semi-Annual, Annual** |
| **Endpoints** | `/subscriptions/status?entityType=GARAGE&entityId=123` | **`/subscriptions/user/status`** |
| **ID Required** | `garageId` + `entityType` | **Only `userId` (from header)** |
| **Access Check** | Per garage/entity | **Per user (one subscription = all access)** |

### Breaking Changes

1. **No more `entityType` and `entityId` parameters** - User ID from header is sufficient
2. **Response fields changed** - `garageId`/`garageName` replaced with `userId`
3. **Trial is 1 month, not 30 days** - End date calculated using `plusMonths(1)`
4. **Old endpoints deprecated** - Still work but marked deprecated in Swagger

---

## Overview

The Platform Subscription Module now uses a **user-based model**. A single subscription unlocks access to ALL dashboards (garages, services, etc.) for that user.

**Authentication**: All endpoints require the `X-Auto-Adda-Id` header with the user's ID.

---

## Data Models

### SubscriptionPlanResponse (Unchanged)
```typescript
interface SubscriptionPlanResponse {
  id: number;
  name: string;                    // "Monthly", "Semi-Annual", "Annual"
  durationMonths: number;          // 1, 6, or 12
  price: number;                   // Set by admin
  description: string;
  applicableTo: string | null;     // DEPRECATED - always null now
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
```

### PlatformSubscriptionResponse
```typescript
interface PlatformSubscriptionResponse {
  id: number;
  userId: number;                  // NEW - replaces garageId
  userName: string;                // NEW - replaces garageName
  entityId: number | null;         // DEPRECATED - may be null
  entityType: string | null;       // DEPRECATED - may be null
  plan: SubscriptionPlanResponse;
  startDate: string;               // ISO date (YYYY-MM-DD)
  endDate: string;                 // ISO date (YYYY-MM-DD)
  status: 'PENDING' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  paymentStatus: 'NOT_INITIATED' | 'PENDING' | 'SUCCESS' | 'FAILED' | 'EXPIRED';
  activatedBy: number | null;
  activatedAt: string | null;
  paymentReference: string | null;
  paymentAmount: number | null;
  notes: string | null;
  isTrial: boolean;
  daysRemaining: number;
  isExpiringSoon: boolean;         // true if <= 7 days remaining
  createdAt: string;
  updatedAt: string;
}
```

### SubscriptionStatusResponse (Updated)
```typescript
interface SubscriptionStatusResponse {
  userId: number;                  // NEW - identifies the user
  entityId: number | null;         // DEPRECATED
  entityType: string | null;       // DEPRECATED
  hasActiveSubscription: boolean;
  status: 'ACTIVE' | 'EXPIRED' | 'PENDING' | 'CANCELLED' | 'NO_SUBSCRIPTION';
  planName: string | null;
  isTrial: boolean;
  startDate: string | null;
  endDate: string | null;
  daysRemaining: number;
  isExpiringSoon: boolean;
  message: string;
}
```

---

## New User-Based Endpoints

### 1. Check User Subscription Status (Primary Endpoint)

**Use Case**: Call on app load to check if user has access to ANY dashboard.

```http
GET /subscriptions/user/status
X-Auto-Adda-Id: {userId}
```

**Response** (200 OK):
```json
{
  "userId": 456,
  "hasActiveSubscription": true,
  "status": "ACTIVE",
  "planName": "Annual",
  "isTrial": false,
  "startDate": "2026-01-01",
  "endDate": "2027-01-01",
  "daysRemaining": 349,
  "isExpiringSoon": false,
  "message": "Your subscription is active. 349 day(s) remaining."
}
```

**Migration Note**:
```typescript
// OLD WAY (v1.0) - Required garageId and entityType
const response = await fetch(
  `/subscriptions/status?entityType=GARAGE&entityId=${garageId}`,
  { headers: { 'X-Auto-Adda-Id': ownerId.toString() } }
);

// NEW WAY (v2.0) - Just needs userId from header
const response = await fetch('/subscriptions/user/status', {
  headers: { 'X-Auto-Adda-Id': userId.toString() }
});
```

---

### 2. Check if User Has Active Subscription

**Use Case**: Quick boolean check for access control.

```http
GET /subscriptions/user/check
X-Auto-Adda-Id: {userId}
```

**Response** (200 OK):
```json
{
  "hasActiveSubscription": true
}
```

---

### 3. Check Dashboard Access

**Use Case**: Get detailed access status with message.

```http
GET /subscriptions/user/access
X-Auto-Adda-Id: {userId}
```

**Response** (200 OK):
```json
{
  "userId": 456,
  "hasActiveSubscription": true,
  "status": "ACTIVE",
  "planName": "Monthly",
  "isTrial": false,
  "startDate": "2026-01-01",
  "endDate": "2026-02-01",
  "daysRemaining": 15,
  "isExpiringSoon": false,
  "message": "Your subscription is active. 15 day(s) remaining."
}
```

---

### 4. Start Free Trial (1 Month)

**Use Case**: User starts their one-time free trial.

```http
POST /subscriptions/user/trial
X-Auto-Adda-Id: {userId}
```

**Response** (201 Created):
```json
{
  "id": 10,
  "userId": 456,
  "userName": "John Doe",
  "plan": { "id": 1, "name": "Monthly", "durationMonths": 1 },
  "startDate": "2026-01-17",
  "endDate": "2026-02-17",
  "status": "ACTIVE",
  "paymentStatus": "SUCCESS",
  "isTrial": true,
  "daysRemaining": 31,
  "isExpiringSoon": false,
  "notes": "1-month free trial - user subscription"
}
```

**Error Response** (400 Bad Request):
```json
{
  "timestamp": "2026-01-17T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Free trial has already been used"
}
```

**Migration Note**: Trial is now **1 calendar month**, not 30 days.
- Old: `startDate.plusDays(30)` = Fixed 30 days
- New: `startDate.plusMonths(1)` = Calendar month (28-31 days)

---

### 5. Check if User Has Used Trial

**Use Case**: Determine if "Start Free Trial" button should be shown.

```http
GET /subscriptions/user/trial/check
X-Auto-Adda-Id: {userId}
```

**Response** (200 OK):
```json
{
  "hasUsedTrial": false
}
```

---

### 6. Create Subscription (After Payment)

**Use Case**: Create a subscription in PENDING status, awaiting payment.

```http
POST /subscriptions/user/create
Content-Type: application/json
X-Auto-Adda-Id: {userId}

{
  "planId": 3,
  "notes": "Annual subscription"
}
```

**Response** (201 Created):
```json
{
  "id": 11,
  "userId": 456,
  "userName": "John Doe",
  "plan": { "id": 3, "name": "Annual", "durationMonths": 12, "price": 899.00 },
  "startDate": "2026-01-17",
  "endDate": "2027-01-17",
  "status": "PENDING",
  "paymentStatus": "NOT_INITIATED",
  "paymentAmount": 899.00,
  "isTrial": false,
  "daysRemaining": 365,
  "notes": "Annual subscription"
}
```

---

### 7. Get Available Plans (Unchanged)

```http
GET /subscriptions/plans
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Monthly",
    "durationMonths": 1,
    "price": 99.00,
    "description": "Monthly subscription - unlocks all dashboards",
    "isActive": true
  },
  {
    "id": 2,
    "name": "Semi-Annual",
    "durationMonths": 6,
    "price": 499.00,
    "description": "6-month subscription - unlocks all dashboards",
    "isActive": true
  },
  {
    "id": 3,
    "name": "Annual",
    "durationMonths": 12,
    "price": 899.00,
    "description": "12-month subscription - unlocks all dashboards",
    "isActive": true
  }
]
```

---

## Frontend Integration Examples

### Dashboard Access Guard (Updated)

```tsx
// OLD WAY - Checked per garage
function DashboardGuard({ children, garageId, ownerId }) {
  // Had to pass garageId and entityType
}

// NEW WAY - Checks per user (simpler!)
function DashboardGuard({ children, userId }: { children: React.ReactNode; userId: number }) {
  const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function checkAccess() {
      const response = await fetch('/subscriptions/user/status', {
        headers: { 'X-Auto-Adda-Id': userId.toString() }
      });
      const data = await response.json();
      setStatus(data);
      setLoading(false);
    }
    checkAccess();
  }, [userId]);

  if (loading) return <LoadingSpinner />;

  if (!status?.hasActiveSubscription) {
    return <SubscriptionPaywall status={status} />;
  }

  return (
    <>
      {status.isTrial && <TrialBanner status={status} />}
      {status.isExpiringSoon && !status.isTrial && <RenewalBanner status={status} />}
      {children}
    </>
  );
}
```

### Check Access on App Load

```typescript
async function checkDashboardAccess(userId: number): Promise<boolean> {
  const response = await fetch('/subscriptions/user/status', {
    headers: { 'X-Auto-Adda-Id': userId.toString() }
  });
  const status: SubscriptionStatusResponse = await response.json();

  if (!status.hasActiveSubscription) {
    redirectToSubscriptionPage();
    return false;
  }

  if (status.isTrial) {
    if (status.isExpiringSoon) {
      showTrialExpiringBanner(status.daysRemaining);
    } else {
      showTrialBadge(status.daysRemaining);
    }
  } else if (status.isExpiringSoon) {
    showRenewalBanner(status.daysRemaining);
  }

  return true;
}
```

### Start Free Trial Button

```tsx
function StartTrialButton({ userId }: { userId: number }) {
  const [hasUsedTrial, setHasUsedTrial] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    async function checkTrial() {
      const response = await fetch('/subscriptions/user/trial/check', {
        headers: { 'X-Auto-Adda-Id': userId.toString() }
      });
      const data = await response.json();
      setHasUsedTrial(data.hasUsedTrial);
    }
    checkTrial();
  }, [userId]);

  if (hasUsedTrial === null) return <LoadingSpinner />;
  if (hasUsedTrial) return null; // Don't show button if trial already used

  const startTrial = async () => {
    setLoading(true);
    try {
      const response = await fetch('/subscriptions/user/trial', {
        method: 'POST',
        headers: { 'X-Auto-Adda-Id': userId.toString() }
      });

      if (response.ok) {
        window.location.reload(); // Refresh to show dashboard
      } else {
        const error = await response.json();
        alert(error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={startTrial}
      disabled={loading}
      className="bg-green-500 hover:bg-green-600 text-white px-6 py-3 rounded-lg font-bold"
    >
      {loading ? 'Starting...' : 'Start 1-Month Free Trial'}
    </button>
  );
}
```

### Trial Banner (Updated for 1 Month)

```tsx
function TrialBanner({ status }: { status: SubscriptionStatusResponse }) {
  if (!status.isTrial) return null;

  if (status.isExpiringSoon) {
    return (
      <div className="bg-orange-100 border-l-4 border-orange-500 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <ClockIcon className="text-orange-500 w-6 h-6" />
            <div className="ml-3">
              <p className="font-bold text-orange-700">Trial Ending Soon!</p>
              <p className="text-orange-600">
                Your 1-month free trial expires in {status.daysRemaining} day(s).
                Subscribe now to continue accessing all dashboards.
              </p>
            </div>
          </div>
          <button className="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded">
            Subscribe Now
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-blue-100 text-blue-700 px-4 py-2 flex items-center justify-between">
      <div className="flex items-center">
        <GiftIcon className="w-5 h-5 mr-2" />
        <span>Free Trial Active: {status.daysRemaining} days remaining</span>
      </div>
      <a href="/pricing" className="underline">View Plans</a>
    </div>
  );
}
```

### Subscription Status Messages

| Status | isTrial | Message Example |
|--------|---------|-----------------|
| ACTIVE | false | "Your subscription is active. 349 day(s) remaining." |
| ACTIVE | true | "Your free trial is active. 28 day(s) remaining." |
| ACTIVE (expiring) | false | "Your subscription expires in 5 day(s). Please renew soon." |
| ACTIVE (expiring) | true | "Your free trial expires in 5 day(s). Subscribe now to continue." |
| EXPIRED | false | "Your subscription has expired. Please renew to continue using the dashboard." |
| EXPIRED | true | "Your free trial has expired. Please subscribe to access the dashboard." |
| PENDING | - | "Your subscription is pending activation. Please complete payment." |
| CANCELLED | - | "Your subscription has been cancelled." |
| NO_SUBSCRIPTION | - | "No subscription found. Please subscribe to access the dashboard." |

---

## Endpoint Migration Reference

### User Endpoints (NEW - Use These)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/subscriptions/user/status` | Get user's subscription status |
| GET | `/subscriptions/user/check` | Check if user has active subscription |
| GET | `/subscriptions/user/access` | Check dashboard access |
| POST | `/subscriptions/user/trial` | Start 1-month free trial |
| GET | `/subscriptions/user/trial/check` | Check if user has used trial |
| POST | `/subscriptions/user/create` | Create subscription (PENDING) |

### Deprecated Endpoints (Still Work - Avoid for New Code)

| Method | Old Endpoint | New Equivalent |
|--------|--------------|----------------|
| GET | `/subscriptions/status?entityType=X&entityId=Y` | `/subscriptions/user/status` |
| GET | `/subscriptions/check?entityType=X&entityId=Y` | `/subscriptions/user/check` |
| GET | `/subscriptions/access?entityType=X&entityId=Y` | `/subscriptions/user/access` |
| POST | `/subscriptions/create` (with entityId) | `/subscriptions/user/create` |

### Admin Endpoints (Unchanged)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/subscriptions/plans` | List all plans |
| GET | `/subscriptions/plans/active` | List active plans only |
| POST | `/subscriptions/admin/create` | Create subscription (Admin) |
| POST | `/subscriptions/admin/{id}/activate` | Activate subscription |
| PUT | `/subscriptions/admin/{id}/cancel` | Cancel subscription |
| PUT | `/subscriptions/admin/{id}/extend` | Extend subscription |
| GET | `/subscriptions/admin` | List all subscriptions |
| GET | `/subscriptions/admin/expiring` | Get expiring subscriptions |
| GET | `/subscriptions/admin/expired` | Get expired subscriptions |
| POST | `/subscriptions/admin/trial/{entityType}/{entityId}` | Grant trial (entity-based, still works) |

---

## Key Differences Summary

1. **One subscription = All access**
   - Old: Check subscription per garage/entity
   - New: Check once per user, access everything

2. **Simpler API calls**
   - Old: `?entityType=GARAGE&entityId=123`
   - New: Just `X-Auto-Adda-Id` header

3. **Trial duration**
   - Old: 30 days (fixed)
   - New: 1 calendar month (28-31 days)

4. **Fixed plans**
   - Old: Admin could create any plan
   - New: 3 standard plans (Monthly, Semi-Annual, Annual)

5. **Response structure**
   - Old: `garageId`, `garageName`, `entityType`
   - New: `userId` (entity fields deprecated/nullable)
