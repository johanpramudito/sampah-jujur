# Firebase Firestore Security Rules

This document provides recommended security rules for the Sampah Jujur application's Firestore database.

## Overview

These security rules ensure that:
- Users can only access data they're authorized to see
- Data integrity is maintained
- Authentication is properly enforced
- Role-based access control is implemented

## Security Rules Configuration

Copy the following rules to your Firebase Console under **Firestore Database → Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }

    function isHousehold() {
      return isAuthenticated() && getUserData().userType == 'household';
    }

    function isCollector() {
      return isAuthenticated() && getUserData().userType == 'collector';
    }

    function isValidUserType(type) {
      return type == 'household' || type == 'collector';
    }

    // Users collection
    match /users/{userId} {
      // Allow read if:
      // - User is reading their own document
      // - User is a collector reading a household's profile (for pickup requests)
      // - User is a household reading a collector's profile (for assigned collectors)
      allow read: if isOwner(userId) || isAuthenticated();

      // Allow create only during registration (first-time creation)
      allow create: if isAuthenticated()
                    && request.auth.uid == userId
                    && isValidUserType(request.resource.data.userType)
                    && request.resource.data.id == userId
                    && request.resource.data.fullName is string
                    && request.resource.data.phone is string;

      // Allow update only for own profile
      allow update: if isOwner(userId)
                    && request.resource.data.id == userId
                    && request.resource.data.userType == resource.data.userType; // Can't change user type

      // Prevent deletion of user documents
      allow delete: if false;
    }

    // Pickup Requests collection
    match /pickupRequests/{requestId} {
      // Allow read if:
      // - User is the household who created the request
      // - User is a collector (to see available requests)
      allow read: if isAuthenticated()
                  && (isOwner(resource.data.householdId) || isCollector());

      // Allow create only by households
      allow create: if isHousehold()
                    && request.auth.uid == request.resource.data.householdId
                    && request.resource.data.status == 'pending'
                    && request.resource.data.wasteItems is list
                    && request.resource.data.wasteItems.size() > 0
                    && request.resource.data.pickupLocation is map
                    && request.resource.data.pickupLocation.address is string;

      // Allow update if:
      // - Household owns the request and status is 'pending' (can cancel or modify)
      // - Collector is accepting/completing the request
      allow update: if (isOwner(resource.data.householdId)
                        && resource.data.status == 'pending')
                    || (isCollector()
                        && (request.resource.data.status == 'accepted'
                            || request.resource.data.status == 'in_progress'
                            || request.resource.data.status == 'completed')
                        && request.resource.data.collectorId == request.auth.uid);

      // Allow delete only by household owner and only if status is 'pending'
      allow delete: if isOwner(resource.data.householdId)
                    && resource.data.status == 'pending';
    }

    // Waste Items subcollection (if stored separately)
    match /pickupRequests/{requestId}/wasteItems/{itemId} {
      // Allow read if user can read the parent request
      allow read: if isAuthenticated()
                  && (get(/databases/$(database)/documents/pickupRequests/$(requestId)).data.householdId == request.auth.uid
                      || isCollector());

      // Allow write only by the household owner of the parent request
      allow write: if isAuthenticated()
                   && get(/databases/$(database)/documents/pickupRequests/$(requestId)).data.householdId == request.auth.uid;
    }

    // Transactions collection (for completed pickups)
    match /transactions/{transactionId} {
      // Allow read if user is involved in the transaction
      allow read: if isAuthenticated()
                  && (request.auth.uid == resource.data.householdId
                      || request.auth.uid == resource.data.collectorId);

      // Allow create only by collectors when completing a pickup
      allow create: if isCollector()
                    && request.auth.uid == request.resource.data.collectorId
                    && request.resource.data.status == 'completed'
                    && request.resource.data.totalAmount >= 0;

      // Prevent updates and deletes for transaction integrity
      allow update: if false;
      allow delete: if false;
    }

    // Notifications collection (optional)
    match /notifications/{notificationId} {
      // Users can only read their own notifications
      allow read: if isOwner(resource.data.userId);

      // Only system (via Cloud Functions) can create notifications
      // For now, prevent client-side creation
      allow create: if false;

      // Users can mark their notifications as read
      allow update: if isOwner(resource.data.userId)
                    && request.resource.data.keys().hasOnly(['read'])
                    && request.resource.data.read is bool;

      // Users can delete their own notifications
      allow delete: if isOwner(resource.data.userId);
    }

    // Deny all other access by default
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## Rule Breakdown

### User Collection Rules

**Read Access:**
- Users can read their own profile
- Authenticated users can read other profiles (needed for viewing collector/household info)

**Write Access:**
- Users can only create their own profile during registration
- Users can update their own profile but cannot change userType
- User deletion is disabled to maintain data integrity

**Validation:**
- userType must be either 'household' or 'collector'
- User ID must match authentication UID
- Required fields: id, fullName, phone, userType

### Pickup Requests Rules

**Read Access:**
- Households can read their own requests
- Collectors can read all requests (to find available pickups)

**Create Access:**
- Only households can create pickup requests
- Must include waste items and pickup location
- Initial status must be 'pending'

**Update Access:**
- Households can update their own pending requests
- Collectors can update status when accepting/completing
- Collector must be assigned to the request to update

**Delete Access:**
- Only households can delete their own requests
- Only pending requests can be deleted

### Transaction Rules

**Read Access:**
- Users can only read transactions they're involved in

**Create Access:**
- Only collectors can create transactions when completing pickups
- Must include valid collector ID, household ID, and amount

**Update/Delete Access:**
- Disabled for data integrity

## Testing Security Rules

### Test in Firebase Console

1. Go to Firebase Console → Firestore → Rules
2. Click on "Rules Playground"
3. Test various scenarios:

```javascript
// Test 1: Household reading own profile
Authenticated as: {userId}
Location: /users/{userId}
Operation: get
Expected: Allow

// Test 2: User trying to read another user's profile
Authenticated as: {userId1}
Location: /users/{userId2}
Operation: get
Expected: Allow (but in production, consider restricting)

// Test 3: Household creating a pickup request
Authenticated as: {householdId}
Location: /pickupRequests/{requestId}
Operation: create
Data: {
  householdId: {householdId},
  status: 'pending',
  wasteItems: [...],
  pickupLocation: {...}
}
Expected: Allow

// Test 4: Collector trying to delete a request
Authenticated as: {collectorId}
Location: /pickupRequests/{requestId}
Operation: delete
Expected: Deny
```

## Best Practices

### 1. **Principle of Least Privilege**
Only grant the minimum permissions necessary for each operation.

### 2. **Input Validation**
Always validate data on both client and server:
- Check required fields exist
- Verify data types
- Validate value ranges
- Sanitize string inputs

### 3. **Role Verification**
Always verify user roles before allowing role-specific actions.

### 4. **Audit Logging**
Consider enabling Cloud Firestore audit logs for production:
- Monitor suspicious access patterns
- Track failed permission attempts
- Review data modification history

### 5. **Rate Limiting**
Implement rate limiting for:
- User registration
- Request creation
- Profile updates

Consider using Firebase App Check for additional security.

## Security Checklist

Before deploying to production, ensure:

- [ ] Security rules are deployed
- [ ] Rules are tested in Rules Playground
- [ ] Client-side validation matches server-side rules
- [ ] Sensitive data (passwords, tokens) is not stored in Firestore
- [ ] User authentication is enforced on all protected routes
- [ ] Firebase App Check is enabled (optional but recommended)
- [ ] Cloud Firestore audit logs are enabled
- [ ] Rate limiting is implemented for sensitive operations
- [ ] API keys are restricted in Google Cloud Console
- [ ] Firebase project has proper IAM permissions

## Common Security Issues to Avoid

### ❌ Don't Do This:

```javascript
// INSECURE: Allows anyone to read all data
allow read: if true;

// INSECURE: Allows anyone to write without validation
allow write: if request.auth != null;

// INSECURE: No validation on user-provided data
allow create: if isAuthenticated();
```

### ✅ Do This Instead:

```javascript
// SECURE: Specific read permissions
allow read: if isOwner(userId) || hasPermission();

// SECURE: Write with validation
allow write: if isAuthenticated()
          && validateData(request.resource.data);

// SECURE: Create with full validation
allow create: if isAuthenticated()
           && request.resource.data.id == request.auth.uid
           && isValidUserType(request.resource.data.userType);
```

## Monitoring and Maintenance

### Regular Security Audits

1. **Monthly:** Review access logs for unusual patterns
2. **Quarterly:** Review and update security rules
3. **Yearly:** Complete security audit with penetration testing

### Firebase Console Monitoring

Monitor these metrics in Firebase Console:
- **Firestore Usage:** Unusual spikes in read/write operations
- **Authentication:** Failed login attempts
- **Security Rules:** Denied access attempts

## Additional Resources

- [Firebase Security Rules Documentation](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Security Checklist](https://firebase.google.com/support/guides/security-checklist)
- [Common Security Rules Pitfalls](https://firebase.google.com/docs/firestore/security/rules-conditions)

## Support

For security concerns or questions:
1. Review Firebase documentation
2. Check Stack Overflow with tag `firebase-security-rules`
3. Contact Firebase support for critical security issues

---

**Last Updated:** 2025-01-26
**Version:** 1.0
**Maintained By:** Development Team
