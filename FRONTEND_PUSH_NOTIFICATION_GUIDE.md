# 🔔 Frontend Push Notification Integration Guide - AutoAdda

## 📋 Table of Contents
1. [Overview](#overview)
2. [Multi-Role Subscription Logic](#multi-role-subscription-logic)
3. [Prerequisites](#prerequisites)
4. [Step-by-Step Implementation](#step-by-step-implementation)
5. [Code Examples](#code-examples)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## Overview

### What You're Building
A browser push notification system that:
- ✅ Works even when the user closes the browser
- ✅ Supports users with multiple roles (CUSTOMER + SERVICE_PROVIDER)
- ✅ Sends role-specific notifications
- ✅ Manages subscriptions across multiple devices

### How Multi-Role Works
```
User has roles: [CUSTOMER, SERVICE_PROVIDER]
         ↓
Subscribe SEPARATELY for EACH role
         ↓
Creates 2 subscriptions in database:
  1. (userId=123, role=CUSTOMER, fcmToken=abc)
  2. (userId=123, role=SERVICE_PROVIDER, fcmToken=abc)
         ↓
When backend sends to CUSTOMER → User receives it
When backend sends to SERVICE_PROVIDER → User receives it
```

---

## Multi-Role Subscription Logic

### ⚠️ CRITICAL CONCEPT

**Each role needs a separate subscription!**

```javascript
// ❌ WRONG - Only subscribes once
subscribeToNotifications(userId, roles[0]);

// ✅ CORRECT - Subscribes for each role
for (const role of userRoles) {
  await subscribeToNotifications(userId, role);
}
```

### Why?
- Backend stores subscriptions with `(userId, fcmToken, role)` as unique key
- Same device can have multiple subscriptions with different roles
- When backend sends notification to "CUSTOMER" role, it filters by role
- If you only subscribe as CUSTOMER, you won't get SERVICE_PROVIDER notifications

---

## Prerequisites

### 1. Firebase Project Setup

**Go to Firebase Console**: https://console.firebase.google.com

1. **Create Project** (or use existing)
   - Name: `AutoAdda` (or your choice)
   - Enable Google Analytics (optional)

2. **Add Web App**
   - Settings → General → Your apps → Web
   - Register app with a nickname (e.g., "AutoAdda Web")
   - Copy the Firebase config object

3. **Get VAPID Key**
   - Settings → Cloud Messaging → Web configuration
   - Click "Generate key pair" under "Web Push certificates"
   - Copy the VAPID key

4. **Download Service Account** (Backend already has this)
   - Settings → Service Accounts → Generate new private key
   - Save as `firebase-service-account.json`

### 2. Install Dependencies

**For React/Vue/Angular projects:**
```bash
npm install firebase
```

**For vanilla JavaScript:**
```html
<!-- Add to index.html -->
<script src="https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js"></script>
```

---

## Step-by-Step Implementation

### 📁 File Structure

```
your-project/
├── public/
│   └── firebase-messaging-sw.js  ← Service Worker (MUST be in public folder)
├── src/
│   ├── services/
│   │   ├── firebase.js            ← Firebase initialization
│   │   └── notificationService.js ← Subscription logic
│   └── components/
│       └── NotificationButton.jsx ← UI component
```

---

## Code Examples

### 1️⃣ Firebase Configuration (`src/services/firebase.js`)

```javascript
// For React/Vue/Angular projects
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

// Your Firebase config from Firebase Console
const firebaseConfig = {
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "autoadda-xxxxx.firebaseapp.com",
  projectId: "autoadda-xxxxx",
  storageBucket: "autoadda-xxxxx.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdef123456"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// VAPID key from Firebase Console
export const VAPID_KEY = 'BNxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';

export { messaging, getToken, onMessage };
```

---

### 2️⃣ Notification Service (`src/services/notificationService.js`)

```javascript
import { messaging, getToken, onMessage, VAPID_KEY } from './firebase';

// Backend API base URL
const API_BASE_URL = 'http://localhost:8080';

/**
 * 🔥 MAIN FUNCTION: Subscribe user for ALL their roles
 *
 * @param {string} userId - User ID from your auth system
 * @param {Array<string>} userRoles - Array of role strings ["CUSTOMER", "SERVICE_PROVIDER"]
 * @returns {Promise<boolean>} Success status
 */
export async function subscribeToNotifications(userId, userRoles) {
  try {
    console.log('🔔 Starting notification subscription for user:', userId);
    console.log('📋 User roles:', userRoles);

    // Step 1: Check if browser supports notifications
    if (!('Notification' in window)) {
      console.error('❌ This browser does not support notifications');
      alert('Your browser does not support notifications');
      return false;
    }

    // Step 2: Check if service worker is supported
    if (!('serviceWorker' in navigator)) {
      console.error('❌ Service Worker not supported');
      alert('Your browser does not support service workers');
      return false;
    }

    // Step 3: Request notification permission
    console.log('📱 Requesting notification permission...');
    const permission = await Notification.requestPermission();

    if (permission !== 'granted') {
      console.log('⚠️ Notification permission denied');
      alert('Please allow notifications to receive updates');
      return false;
    }

    console.log('✅ Notification permission granted');

    // Step 4: Register service worker
    console.log('🔧 Registering service worker...');
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
    console.log('✅ Service worker registered:', registration);

    // Step 5: Get FCM token from Firebase
    console.log('🔑 Getting FCM token...');
    const fcmToken = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration
    });

    if (!fcmToken) {
      console.error('❌ Failed to get FCM token');
      return false;
    }

    console.log('✅ FCM Token obtained:', fcmToken.substring(0, 20) + '...');

    // Step 6: Subscribe for EACH role separately
    console.log('📝 Subscribing for each role...');

    const subscriptionResults = [];

    for (const role of userRoles) {
      console.log(`⏳ Subscribing as ${role}...`);

      try {
        const response = await fetch(`${API_BASE_URL}/notifications/subscribe`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            userId: userId,
            role: role,
            fcmToken: fcmToken,
            deviceInfo: getBrowserInfo()
          })
        });

        const result = await response.json();

        if (result.success) {
          console.log(`✅ Subscribed as ${role}:`, result);
          subscriptionResults.push({ role, success: true });
        } else {
          console.error(`❌ Failed to subscribe as ${role}:`, result.message);
          subscriptionResults.push({ role, success: false, error: result.message });
        }
      } catch (error) {
        console.error(`❌ Error subscribing as ${role}:`, error);
        subscriptionResults.push({ role, success: false, error: error.message });
      }
    }

    // Step 7: Check if all subscriptions succeeded
    const allSuccessful = subscriptionResults.every(r => r.success);

    if (allSuccessful) {
      console.log('🎉 Successfully subscribed for all roles!');

      // Step 8: Setup foreground message listener
      setupForegroundListener();

      return true;
    } else {
      console.warn('⚠️ Some subscriptions failed:', subscriptionResults);
      return false;
    }

  } catch (error) {
    console.error('❌ Error in subscription process:', error);
    alert('Failed to enable notifications: ' + error.message);
    return false;
  }
}

/**
 * 🔔 Listen for notifications when app is open (foreground)
 */
function setupForegroundListener() {
  onMessage(messaging, (payload) => {
    console.log('📩 Foreground notification received:', payload);

    const { title, body } = payload.notification;
    const url = payload.data?.url || '/';

    // Option 1: Show browser notification
    if (Notification.permission === 'granted') {
      const notification = new Notification(title, {
        body: body,
        icon: '/logo192.png',
        badge: '/badge.png',
        data: { url: url },
        tag: 'autoadda-notification',
        requireInteraction: false
      });

      notification.onclick = () => {
        window.open(url, '_blank');
        notification.close();
      };
    }

    // Option 2: Show in-app toast/snackbar (implement your own)
    // showInAppNotification(title, body, url);
  });

  console.log('✅ Foreground notification listener setup complete');
}

/**
 * 🔓 Unsubscribe from ALL roles for this device
 */
export async function unsubscribeFromAllRoles(userId) {
  try {
    const fcmToken = await getToken(messaging, { vapidKey: VAPID_KEY });

    const response = await fetch(`${API_BASE_URL}/notifications/unsubscribe`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, fcmToken })
    });

    const result = await response.json();
    console.log('Unsubscribed from all roles:', result);
    return result.success;

  } catch (error) {
    console.error('Error unsubscribing:', error);
    return false;
  }
}

/**
 * 🔓 Unsubscribe from specific role
 */
export async function unsubscribeFromRole(userId, role) {
  try {
    const fcmToken = await getToken(messaging, { vapidKey: VAPID_KEY });

    const response = await fetch(`${API_BASE_URL}/notifications/unsubscribe-role`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, fcmToken, role })
    });

    const result = await response.json();
    console.log(`Unsubscribed from ${role}:`, result);
    return result.success;

  } catch (error) {
    console.error('Error unsubscribing from role:', error);
    return false;
  }
}

/**
 * 📊 Get browser information for device tracking
 */
function getBrowserInfo() {
  const ua = navigator.userAgent;
  let browserName = 'Unknown';

  if (ua.includes('Chrome') && !ua.includes('Edge')) browserName = 'Chrome';
  else if (ua.includes('Firefox')) browserName = 'Firefox';
  else if (ua.includes('Safari') && !ua.includes('Chrome')) browserName = 'Safari';
  else if (ua.includes('Edge')) browserName = 'Edge';
  else if (ua.includes('Opera')) browserName = 'Opera';

  return `${browserName} on ${navigator.platform}`;
}

/**
 * ✅ Check if user has already subscribed
 */
export async function checkNotificationStatus() {
  if (!('Notification' in window)) {
    return 'not-supported';
  }

  return Notification.permission; // 'granted', 'denied', or 'default'
}
```

---

### 3️⃣ Service Worker (`public/firebase-messaging-sw.js`)

**⚠️ IMPORTANT**: This file MUST be in the `public` folder and named exactly `firebase-messaging-sw.js`

```javascript
// Import Firebase scripts (use compat version for service workers)
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

// Initialize Firebase in service worker
// ⚠️ MUST use the same config as your app
firebase.initializeApp({
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "autoadda-xxxxx.firebaseapp.com",
  projectId: "autoadda-xxxxx",
  storageBucket: "autoadda-xxxxx.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdef123456"
});

const messaging = firebase.messaging();

// 🔔 Handle background notifications (when app is closed)
messaging.onBackgroundMessage((payload) => {
  console.log('[Service Worker] Background notification received:', payload);

  const notificationTitle = payload.notification?.title || 'AutoAdda Notification';
  const notificationOptions = {
    body: payload.notification?.body || 'You have a new notification',
    icon: '/logo192.png',        // App icon (192x192)
    badge: '/badge.png',          // Small monochrome icon (96x96)
    image: payload.notification?.image,  // Large image (optional)
    data: {
      url: payload.data?.url || '/',
      clickAction: payload.fcmOptions?.link || '/'
    },
    tag: 'autoadda-notification',  // Replaces old notifications with same tag
    requireInteraction: false,      // Auto-dismiss after few seconds
    vibrate: [200, 100, 200],      // Vibration pattern (mobile)
    actions: [
      {
        action: 'open',
        title: 'View',
        icon: '/icons/open.png'
      },
      {
        action: 'dismiss',
        title: 'Dismiss',
        icon: '/icons/close.png'
      }
    ]
  };

  // Show the notification
  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// 🖱️ Handle notification click
self.addEventListener('notificationclick', (event) => {
  console.log('[Service Worker] Notification clicked:', event);

  event.notification.close();

  // Handle action buttons
  if (event.action === 'dismiss') {
    return; // Just close, do nothing
  }

  // Get URL from notification data
  const urlToOpen = event.notification.data?.url || '/';
  const fullUrl = new URL(urlToOpen, self.location.origin).href;

  // Open URL in existing tab or new tab
  event.waitUntil(
    clients.matchAll({
      type: 'window',
      includeUncontrolled: true
    }).then((clientList) => {
      // Check if app is already open in a tab
      for (const client of clientList) {
        if (client.url === fullUrl && 'focus' in client) {
          return client.focus();
        }
      }

      // Open new tab if not already open
      if (clients.openWindow) {
        return clients.openWindow(fullUrl);
      }
    })
  );
});

// 📝 Log service worker lifecycle
self.addEventListener('install', (event) => {
  console.log('[Service Worker] Installing...');
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  console.log('[Service Worker] Activating...');
  event.waitUntil(clients.claim());
});
```

---

### 4️⃣ React Component Example (`src/components/NotificationButton.jsx`)

```jsx
import React, { useState, useEffect } from 'react';
import {
  subscribeToNotifications,
  unsubscribeFromAllRoles,
  checkNotificationStatus
} from '../services/notificationService';

function NotificationButton({ user }) {
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [notificationStatus, setNotificationStatus] = useState('default');

  useEffect(() => {
    // Check initial notification status
    checkNotificationStatus().then(status => {
      setNotificationStatus(status);
      setIsSubscribed(status === 'granted');
    });
  }, []);

  const handleEnableNotifications = async () => {
    if (!user || !user.id || !user.roles) {
      alert('User information not available');
      return;
    }

    setIsLoading(true);

    try {
      // Subscribe for all user roles
      const success = await subscribeToNotifications(user.id, user.roles);

      if (success) {
        setIsSubscribed(true);
        setNotificationStatus('granted');
        alert('✅ Notifications enabled for all your roles!');
      } else {
        alert('⚠️ Failed to enable notifications. Check console for details.');
      }
    } catch (error) {
      console.error('Error enabling notifications:', error);
      alert('❌ Error: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDisableNotifications = async () => {
    setIsLoading(true);

    try {
      const success = await unsubscribeFromAllRoles(user.id);

      if (success) {
        setIsSubscribed(false);
        alert('🔕 Notifications disabled');
      } else {
        alert('Failed to disable notifications');
      }
    } catch (error) {
      console.error('Error disabling notifications:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Don't show if browser doesn't support notifications
  if (notificationStatus === 'not-supported') {
    return null;
  }

  return (
    <div className="notification-button-container">
      {!isSubscribed ? (
        <button
          onClick={handleEnableNotifications}
          disabled={isLoading || notificationStatus === 'denied'}
          className="btn-enable-notifications"
        >
          {isLoading ? '⏳ Enabling...' : '🔔 Enable Notifications'}
        </button>
      ) : (
        <div>
          <p>✅ Notifications enabled for: {user.roles.join(', ')}</p>
          <button
            onClick={handleDisableNotifications}
            disabled={isLoading}
            className="btn-disable-notifications"
          >
            {isLoading ? '⏳ Disabling...' : '🔕 Disable Notifications'}
          </button>
        </div>
      )}

      {notificationStatus === 'denied' && (
        <p style={{ color: 'red' }}>
          ⚠️ Notifications blocked. Please enable in browser settings.
        </p>
      )}
    </div>
  );
}

export default NotificationButton;
```

---

### 5️⃣ Usage in Your App

```jsx
import React, { useEffect, useState } from 'react';
import NotificationButton from './components/NotificationButton';

function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Get user from your auth system
    // This is just an example
    const currentUser = {
      id: '123',
      name: 'John Doe',
      email: 'john@example.com',
      roles: ['CUSTOMER', 'SERVICE_PROVIDER'] // ⚠️ IMPORTANT: Array of roles
    };

    setUser(currentUser);
  }, []);

  return (
    <div className="App">
      <h1>AutoAdda Dashboard</h1>

      {user && (
        <>
          <p>Welcome, {user.name}!</p>
          <p>Your roles: {user.roles.join(', ')}</p>

          {/* Notification button */}
          <NotificationButton user={user} />
        </>
      )}
    </div>
  );
}

export default App;
```

---

## Testing

### Test Checklist

#### 1. **Browser DevTools Check**

Open DevTools → **Application Tab**:

- ✅ **Service Workers**: Should show `firebase-messaging-sw.js` as **activated and running**
- ✅ **Notifications**: Permission should be "Granted"
- ✅ **Console**: Should see logs like "✅ Subscribed as CUSTOMER"

#### 2. **Database Verification**

```sql
-- Check if subscriptions were created
SELECT * FROM push_subscription WHERE user_id = '123';

-- Expected result (if user has 2 roles):
-- | id | user_id | role             | fcm_token        | device_info      |
-- |----|---------|------------------|------------------|------------------|
-- | 1  | 123     | CUSTOMER         | cXYz123...       | Chrome on Win... |
-- | 2  | 123     | SERVICE_PROVIDER | cXYz123...       | Chrome on Win... |
```

#### 3. **Send Test Notification**

**Option A: Using Postman/curl**

```bash
# Test 1: Send to CUSTOMER role only
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123",
    "role": "CUSTOMER",
    "title": "Customer Test",
    "body": "This is a customer notification",
    "url": "https://autoadda.com/customer"
  }'

# Test 2: Send to ALL roles
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123",
    "title": "General Test",
    "body": "This goes to all your devices",
    "url": "https://autoadda.com"
  }'
```

**Option B: Using backend test endpoint**

```bash
curl -X POST http://localhost:8080/notifications/test \
  -H "Content-Type: application/json" \
  -d '{
    "role": "CUSTOMER",
    "title": "Test Notification",
    "body": "Testing push notifications",
    "url": "https://autoadda.com"
  }'
```

#### 4. **Expected Results**

| Test | User Roles | Subscription Count | Notifications Received |
|------|-----------|-------------------|----------------------|
| Test 1 (CUSTOMER role) | [CUSTOMER, SERVICE_PROVIDER] | 2 | 1 (only CUSTOMER) |
| Test 2 (All roles) | [CUSTOMER, SERVICE_PROVIDER] | 2 | 2 (both roles) |

---

## Troubleshooting

### Issue 1: "Service worker registration failed"

**Symptoms**: Console error: `Failed to register service worker`

**Solutions**:
1. Ensure `firebase-messaging-sw.js` is in the **public** folder (not src)
2. File must be named exactly `firebase-messaging-sw.js`
3. Must be served from root URL: `/firebase-messaging-sw.js`
4. Check if HTTPS is enabled (required in production)

```javascript
// ✅ CORRECT
navigator.serviceWorker.register('/firebase-messaging-sw.js');

// ❌ WRONG
navigator.serviceWorker.register('./firebase-messaging-sw.js');
navigator.serviceWorker.register('/sw.js');
```

---

### Issue 2: "Failed to get FCM token"

**Symptoms**: `getToken()` returns null or throws error

**Solutions**:
1. Check VAPID key is correct (from Firebase Console → Cloud Messaging)
2. Ensure Firebase config matches exactly
3. Clear browser cache and service workers
4. Check browser console for detailed error message

```javascript
// Debugging
try {
  const token = await getToken(messaging, { vapidKey: VAPID_KEY });
  console.log('Token:', token);
} catch (error) {
  console.error('Detailed error:', error);
  // Check error.code and error.message for specifics
}
```

---

### Issue 3: "Notifications not showing"

**Symptoms**: No notification appears when sent from backend

**Checklist**:
1. ✅ Permission granted? Check `Notification.permission`
2. ✅ Service worker active? Check DevTools → Application → Service Workers
3. ✅ Token sent to backend? Check database for subscription record
4. ✅ Backend sending successfully? Check backend logs
5. ✅ Firebase Cloud Messaging enabled? Check Firebase Console
6. ✅ Browser supports notifications? (Chrome, Firefox, Edge, Safari 16+)

**Debug in Service Worker**:
```javascript
// In firebase-messaging-sw.js
messaging.onBackgroundMessage((payload) => {
  console.log('🔍 DEBUG: Received payload:', payload);
  // Check what's being received
});
```

---

### Issue 4: "Only subscribed to one role"

**Symptoms**: User has 2 roles but only 1 subscription in database

**Solution**: Check your subscription loop

```javascript
// ❌ WRONG - Only subscribes to first role
await subscribeToNotifications(userId, userRoles[0]);

// ✅ CORRECT - Subscribes to all roles
for (const role of userRoles) {
  await subscribeToNotifications(userId, role);
}

// ✅ ALSO CORRECT - Using Promise.all
await Promise.all(
  userRoles.map(role =>
    fetch(`${API_BASE_URL}/notifications/subscribe`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, role, fcmToken, deviceInfo })
    })
  )
);
```

---

### Issue 5: "CORS error when calling subscribe API"

**Symptoms**: Console error: `Access to fetch blocked by CORS policy`

**Backend Fix** (already done in SecurityConfig):
```java
// Backend has CORS enabled for all origins
config.setAllowedOriginPatterns(Arrays.asList("*"));
```

**If still happening**:
1. Check if backend is running on correct port (8080)
2. Update `API_BASE_URL` in notificationService.js
3. Ensure backend CORS includes your frontend origin

---

## API Endpoints Reference

### 1. Subscribe to Notifications

```http
POST http://localhost:8080/notifications/subscribe
Content-Type: application/json

{
  "userId": "123",
  "role": "CUSTOMER",
  "fcmToken": "cXYz123ABCdef456...",
  "deviceInfo": "Chrome on Windows 10"
}
```

**Response:**
```json
{
  "success": true,
  "subscriptionId": 1,
  "message": "Subscription saved successfully",
  "role": "CUSTOMER"
}
```

---

### 2. Unsubscribe from All Roles

```http
POST http://localhost:8080/notifications/unsubscribe
Content-Type: application/json

{
  "userId": "123",
  "fcmToken": "cXYz123ABCdef456..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Unsubscribed from all roles successfully"
}
```

---

### 3. Unsubscribe from Specific Role

```http
POST http://localhost:8080/notifications/unsubscribe-role
Content-Type: application/json

{
  "userId": "123",
  "fcmToken": "cXYz123ABCdef456...",
  "role": "CUSTOMER"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Unsubscribed from role: CUSTOMER",
  "role": "CUSTOMER"
}
```

---

## Production Checklist

Before deploying to production:

- [ ] **HTTPS Enabled**: Service workers require HTTPS (except localhost)
- [ ] **Firebase Config Updated**: Use production Firebase project
- [ ] **VAPID Key Set**: Correct VAPID key from Firebase Console
- [ ] **API Base URL Updated**: Point to production backend URL
- [ ] **Service Worker Path**: Verify `/firebase-messaging-sw.js` is accessible
- [ ] **Icons Added**: Add notification icons (192x192, 96x96)
- [ ] **Error Handling**: Proper user feedback for errors
- [ ] **Browser Compatibility**: Test on Chrome, Firefox, Safari, Edge
- [ ] **Mobile Testing**: Test on iOS Safari 16.4+, Android Chrome
- [ ] **Permission Denied Flow**: Handle gracefully when user denies permission

---

## Quick Start Summary

### For developers who just want to copy-paste:

1. **Install Firebase**: `npm install firebase`

2. **Copy 3 files**:
   - `src/services/firebase.js` → Update Firebase config
   - `src/services/notificationService.js` → Update API_BASE_URL
   - `public/firebase-messaging-sw.js` → Update Firebase config

3. **In your login/auth callback**:
   ```javascript
   import { subscribeToNotifications } from './services/notificationService';

   // After user logs in
   const user = { id: '123', roles: ['CUSTOMER', 'SERVICE_PROVIDER'] };
   await subscribeToNotifications(user.id, user.roles);
   ```

4. **Test**:
   ```bash
   curl -X POST http://localhost:8080/notifications/send \
     -H "Content-Type: application/json" \
     -d '{"userId":"123","title":"Test","body":"Hello","url":"/"}'
   ```

---

## Support

**Questions?** Contact backend team with:
- Console errors (DevTools → Console)
- Network requests (DevTools → Network)
- Service worker status (DevTools → Application → Service Workers)
- User ID and roles being used

**Firebase Issues?** Check:
- Firebase Console → Cloud Messaging → Check delivery stats
- Firebase Console → Usage → Check quota limits
- Firebase docs: https://firebase.google.com/docs/cloud-messaging/js/client

---

**✅ You're all set!** Follow this guide and push notifications will work seamlessly with multi-role support.
