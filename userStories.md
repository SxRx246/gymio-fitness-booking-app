# Gymio – User Stories

## 1. User Registration and Authentication

### US-01: Register an Account

**As a** new user,  
**I want** to register for a Gymio account,  
**So that** I can access the application's fitness booking services.

**Acceptance Criteria:**
- Given that I provide valid registration details, when I submit the registration form, then my account should be created.
- Given that I provide invalid or incomplete details, when I submit the form, then the system should return a validation error.
- Given that I register successfully, when my account is created, then the system should send an email verification message.
- Given that an account already exists with the same email address, when I attempt to register, then the system should reject the duplicate registration.

### US-02: Verify an Email Address

**As a** registered user,  
**I want** to verify my email address,  
**So that** my account can be confirmed.

**Acceptance Criteria:**
- Given that I receive a valid verification link, when I open it, then the system should verify my email address.
- Given that the verification token is invalid or expired, when I attempt verification, then the system should reject the request.
- Given that my email is already verified, when I attempt verification again, then the system should handle the request appropriately without incorrectly changing my account.

### US-03: Resend Email Verification

**As a** registered user,  
**I want** to request another verification email,  
**So that** I can verify my account if I did not receive the original message.

**Acceptance Criteria:**
- Given that my account requires verification, when I request another verification email, then the system should process the request.
- Given that my email is already verified, when I request another verification email, then the system should not send an unnecessary verification message.
- Given that I make repeated requests, when the rate limit is exceeded, then the system should restrict further requests temporarily.

### US-04: Log In

**As a** registered user,  
**I want** to log in securely,  
**So that** I can access features permitted by my account role.

**Acceptance Criteria:**
- Given that I provide valid login credentials, when I log in, then the system should authenticate me and return the appropriate authentication response.
- Given that I provide invalid credentials, when I attempt to log in, then the system should reject the request without revealing sensitive authentication details.
- Given that I exceed the permitted login request rate, when I attempt another login, then the system should apply rate limiting.
- Given that I am not authorized to access a protected resource, when I request it, then the system should deny access.

### US-05: Reset a Forgotten Password

**As a** registered user,  
**I want** to reset my forgotten password,  
**So that** I can regain access to my account.

**Acceptance Criteria:**
- Given that I submit a password-reset request, when the request is processed, then the system should handle it without unnecessarily disclosing whether the email is registered.
- Given that I use a valid reset token and a valid new password, when I submit the reset request, then my password should be updated.
- Given that I use an invalid or expired reset token, when I submit the request, then the system should reject it.
- Given that I provide a password that does not meet the password requirements, when I submit the request, then the system should return a validation error.

### US-06: Change a Password

**As a** logged-in user,  
**I want** to change my password,  
**So that** I can maintain control over my account security.

**Acceptance Criteria:**
- Given that I provide the correct current password and a valid new password, when I submit the update, then the system should update my password.
- Given that I provide an incorrect current password, when I submit the update, then the system should reject the change.
- Given that my new password does not meet the password requirements, when I submit the update, then the system should reject it.

---

## 2. User Profiles

### US-07: Create a User Profile

**As a** registered user,  
**I want** to create my personal profile,  
**So that** I can maintain my personal information in Gymio.

**Acceptance Criteria:**
- Given that I am authenticated, when I submit valid profile information, then the system should create my profile.
- Given that I provide invalid profile information, when I submit the request, then the system should return a validation error.
- Given that I already have a profile, when I attempt to create a duplicate profile, then the system should handle the request according to the profile rules.

### US-08: View My Profile

**As a** registered user,  
**I want** to view my profile,  
**So that** I can review my saved information.

**Acceptance Criteria:**
- Given that I am authenticated and have a profile, when I request my profile, then the system should return my profile information.
- Given that I do not have a profile, when I request it, then the system should handle the missing profile appropriately.
- Given that I am unauthenticated, when I request a protected profile resource, then the system should deny access.

### US-09: Update My Profile

**As a** registered user,  
**I want** to update my profile information,  
**So that** my personal details remain accurate.

**Acceptance Criteria:**
- Given that I provide valid updated information, when I submit the update, then the system should save the changes.
- Given that I provide invalid information, when I submit the update, then the system should return a validation error.
- Given that I attempt to modify another user's private profile without authorization, when I submit the request, then the system should deny access.

### US-10: Upload a Profile Picture

**As a** registered user,  
**I want** to upload a profile picture,  
**So that** my profile can display an image associated with my account.

**Acceptance Criteria:**
- Given that I upload an accepted image file, when I submit the upload request, then the system should process the image and update my profile.
- Given that I upload an unsupported file or invalid content, when I submit the request, then the system should reject the upload.
- Given that I am unauthenticated, when I attempt to upload a profile picture, then the system should deny access.

---

## 3. Fitness Class Management

### US-11: Create a Fitness Class

**As a** trainer,  
**I want** to create a fitness class,  
**So that** customers can discover and book my classes.

**Acceptance Criteria:**
- Given that I am an authorized trainer and provide valid class details, when I submit the request, then the system should create the class.
- Given that I provide invalid class details, when I submit the request, then the system should return a validation error.
- Given that I am not authorized to create classes, when I submit the request, then the system should deny access.

### US-12: View Available Fitness Classes

**As a** customer,  
**I want** to view fitness classes,  
**So that** I can discover classes that match my interests.

**Acceptance Criteria:**
- Given that fitness classes exist, when I request the class list, then the system should return the relevant class information.
- Given that no classes match the request, when I request the class list, then the system should return an appropriate empty result.
- Given that I request a particular class by ID, when the class exists, then the system should return its details.
- Given that the requested class does not exist, when I request it, then the system should return an appropriate not-found response.

### US-13: Search and Filter Fitness Classes

**As a** customer,  
**I want** to search and filter fitness classes,  
**So that** I can find classes based on my preferences.

**Acceptance Criteria:**
- Given that I specify a class type, when I search, then the system should return matching classes.
- Given that I specify a fitness level or class status, when I search, then the system should apply the requested filters.
- Given that I combine supported filters, when I search, then the system should apply them together.
- Given that no classes match my criteria, when I search, then the system should return an empty result rather than unrelated classes.

### US-14: View Classes Assigned to a Trainer

**As a** trainer,  
**I want** to view the classes assigned to me,  
**So that** I can manage my fitness sessions.

**Acceptance Criteria:**
- Given that I am an authorized trainer, when I request my class list, then the system should return the classes associated with the requested trainer.
- Given that pagination parameters are provided, when I request my classes, then the system should return the appropriate page.
- Given that I attempt to perform a restricted operation, when I submit the request, then the system should enforce the required permissions.

### US-15: Update a Fitness Class

**As a** trainer,  
**I want** to update my fitness class details,  
**So that** customers can access accurate class information.

**Acceptance Criteria:**
- Given that I am authorized to update the class and provide valid details, when I submit the update, then the system should save the changes.
- Given that I provide invalid class information, when I submit the update, then the system should return a validation error.
- Given that I am not authorized to update the class, when I submit the request, then the system should deny access.

### US-16: Cancel a Fitness Class

**As a** trainer,  
**I want** to cancel a fitness class,  
**So that** customers are not able to rely on a session that will not take place.

**Acceptance Criteria:**
- Given that I am authorized to manage the class, when I cancel it, then the system should update its status appropriately.
- Given that I am not authorized to cancel the class, when I submit the request, then the system should deny access.
- Given that the class does not exist, when I attempt to cancel it, then the system should return an appropriate not-found response.

### US-17: Delete a Fitness Class

**As an** administrator,  
**I want** to delete a fitness class,  
**So that** I can manage inappropriate or unnecessary class records.

**Acceptance Criteria:**
- Given that I am an authorized administrator, when I delete an existing class, then the system should process the deletion.
- Given that I am not an administrator, when I attempt to delete a class, then the system should deny access.
- Given that the requested class does not exist, when I attempt to delete it, then the system should return an appropriate not-found response.

---

## 4. Fitness Class Bookings

### US-18: Book a Fitness Class

**As a** customer,  
**I want** to book a fitness class,  
**So that** I can reserve a place in a session.

**Acceptance Criteria:**
- Given that I am authenticated and a class has available capacity, when I submit a valid booking request, then the system should create my booking.
- Given that the class has reached its capacity, when I attempt to book it, then the system should reject the booking or follow the configured waitlist rules.
- Given that I already have a booking for the same class, when I attempt to create a duplicate booking, then the system should prevent the duplicate.
- Given that the class cannot be booked, when I submit the request, then the system should reject the booking appropriately.

### US-19: View My Bookings

**As a** customer,  
**I want** to view my bookings,  
**So that** I can keep track of my reserved fitness sessions.

**Acceptance Criteria:**
- Given that I am authenticated, when I request my bookings, then the system should return the bookings associated with my account.
- Given that I have no bookings, when I request my bookings, then the system should return an appropriate empty result.
- Given that another user has a booking, when I request my own bookings, then the system should not expose that user's private booking information.

### US-20: View Booking Details

**As a** customer,  
**I want** to view the details of a booking,  
**So that** I can check the information about my reservation.

**Acceptance Criteria:**
- Given that the booking belongs to me, when I request its details, then the system should return the booking information.
- Given that the booking does not exist, when I request it, then the system should return an appropriate not-found response.
- Given that the booking belongs to another user and I lack permission to view it, when I request it, then the system should deny access.

### US-21: Cancel a Booking

**As a** customer,  
**I want** to cancel my booking,  
**So that** I can release my reserved place when I can no longer attend.

**Acceptance Criteria:**
- Given that I own an eligible booking, when I request cancellation, then the system should update the booking status appropriately.
- Given that the booking belongs to another user and I lack permission, when I attempt to cancel it, then the system should deny access.
- Given that the booking cannot be cancelled under the applicable rules, when I submit the request, then the system should reject the cancellation appropriately.

### US-22: View Bookings for a Fitness Class

**As a** trainer,  
**I want** to view bookings for a class I manage,  
**So that** I can understand customer participation.

**Acceptance Criteria:**
- Given that I am authorized to view the class bookings, when I request them, then the system should return the relevant booking information.
- Given that the class has no bookings, when I request its bookings, then the system should return an appropriate empty result.
- Given that I am not authorized to view the requested bookings, when I submit the request, then the system should deny access.

### US-23: Manage Bookings as an Administrator

**As an** administrator,  
**I want** to view and manage bookings,  
**So that** I can oversee booking activity and resolve administrative issues.

**Acceptance Criteria:**
- Given that I am an authorized administrator, when I request the booking list, then the system should return the bookings permitted by the administrative interface.
- Given that I am authorized to delete a booking, when I submit a valid deletion request, then the system should process it.
- Given that I am not an administrator, when I attempt a restricted booking-management operation, then the system should deny access.

---

## 5. Membership Plans

### US-24: View Membership Plans

**As a** customer,  
**I want** to view available membership plans,  
**So that** I can understand the membership options offered by Gymio.

**Acceptance Criteria:**
- Given that membership plans are available, when I request the plan list, then the system should return the plans accessible to me.
- Given that I request a plan by ID, when the plan exists, then the system should return its details.
- Given that the requested plan does not exist, when I request it, then the system should return an appropriate not-found response.

### US-25: Create a Membership Plan

**As an** administrator,  
**I want** to create a membership plan,  
**So that** I can introduce new membership options.

**Acceptance Criteria:**
- Given that I am an authorized administrator and provide valid plan details, when I submit the request, then the system should create the plan.
- Given that I provide invalid plan details, when I submit the request, then the system should return a validation error.
- Given that I am not an administrator, when I attempt to create a plan, then the system should deny access.

### US-26: Update a Membership Plan

**As an** administrator,  
**I want** to update membership plan details,  
**So that** the available plans remain accurate.

**Acceptance Criteria:**
- Given that I am an authorized administrator and provide valid updated details, when I submit the request, then the system should update the plan.
- Given that the plan does not exist, when I attempt to update it, then the system should return an appropriate not-found response.
- Given that I lack administrative permission, when I submit the request, then the system should deny access.

### US-27: Deactivate or Reactivate a Membership Plan

**As an** administrator,  
**I want** to deactivate or reactivate a membership plan,  
**So that** I can control which plans are available.

**Acceptance Criteria:**
- Given that I am an authorized administrator, when I deactivate an existing plan, then the system should mark it as inactive.
- Given that I am an authorized administrator, when I reactivate an eligible plan, then the system should mark it as active.
- Given that I lack the required permission, when I attempt either operation, then the system should deny access.

---

## 6. Membership Payments

### US-28: Make a Membership Payment

**As a** customer,  
**I want** to make a payment for a membership,  
**So that** I can pay for the membership I choose.

**Acceptance Criteria:**
- Given that I am authenticated and provide valid payment information, when I submit a membership payment request, then the system should process the request according to the configured payment workflow.
- Given that the payment request is invalid, when I submit it, then the system should return an appropriate validation error.
- Given that I am not authenticated, when I attempt to make a payment, then the system should deny access.
- Given that payment processing fails, when the system handles the request, then it should not incorrectly represent the payment as successful.

### US-29: View My Payment History

**As a** customer,  
**I want** to view my payment history,  
**So that** I can review my previous membership payments.

**Acceptance Criteria:**
- Given that I am authenticated, when I request my payments, then the system should return payments associated with my account.
- Given that I have no payments, when I request my payment history, then the system should return an appropriate empty result.
- Given that another user has made payments, when I request my own history, then the system should not expose that user's private payment information.

---

## 7. Waitlist Management

### US-30: Join a Waitlist

**As a** customer,  
**I want** to join a waitlist for a fitness class,  
**So that** I can have an opportunity to attend when a place becomes available.

**Acceptance Criteria:**
- Given that I am authenticated and eligible to join the waitlist, when I submit a valid request, then the system should register my waitlist entry.
- Given that I am not eligible to join the waitlist, when I submit the request, then the system should reject it appropriately.
- Given that a duplicate waitlist entry is not permitted, when I attempt to join the same waitlist again, then the system should prevent the duplicate.

### US-31: View My Waitlist Entries

**As a** customer,  
**I want** to view my waitlist entries,  
**So that** I can track the classes for which I am waiting.

**Acceptance Criteria:**
- Given that I am authenticated, when I request my waitlist entries, then the system should return the entries associated with my account.
- Given that I have no waitlist entries, when I request them, then the system should return an appropriate empty result.
- Given that another user has waitlist entries, when I request my own entries, then the system should not expose that user's private information.

### US-32: Leave a Waitlist

**As a** customer,  
**I want** to leave a waitlist,  
**So that** I can withdraw my interest in a class.

**Acceptance Criteria:**
- Given that I own an eligible waitlist entry, when I request to leave the waitlist, then the system should update or remove the entry according to the implemented rules.
- Given that the entry does not exist or does not belong to me, when I submit the request, then the system should reject it appropriately.
- Given that I am unauthenticated, when I attempt to leave a waitlist, then the system should deny access.

### US-33: Accept a Waitlist Opportunity

**As a** customer,  
**I want** to accept a waitlist opportunity,  
**So that** I can proceed when a place becomes available.

**Acceptance Criteria:**
- Given that I have an eligible waitlist entry, when I accept the opportunity, then the system should update the entry according to the waitlist workflow.
- Given that the opportunity is no longer valid, when I attempt to accept it, then the system should reject the request appropriately.
- Given that the entry belongs to another user, when I attempt to accept it, then the system should deny the operation.

### US-34: Decline a Waitlist Opportunity

**As a** customer,  
**I want** to decline a waitlist opportunity,  
**So that** I can give up a place I no longer want.

**Acceptance Criteria:**
- Given that I have an eligible waitlist entry, when I decline the opportunity, then the system should update the entry according to the implemented workflow.
- Given that the opportunity is invalid or the entry does not exist, when I submit the request, then the system should reject it appropriately.
- Given that I am not authorized to modify the entry, when I submit the request, then the system should deny access.

---

## 8. Notifications

### US-35: Subscribe to Notifications

**As an** authenticated user,  
**I want** to subscribe to real-time notifications,  
**So that** I can receive relevant updates while using Gymio.

**Acceptance Criteria:**
- Given that I am authenticated, when I subscribe to the notification endpoint, then the system should establish an available Server-Sent Events (SSE) connection.
- Given that a relevant notification is generated, when my connection is active, then the system should deliver the notification through the connection.
- Given that I am unauthenticated, when I attempt to subscribe, then the system should deny access.
- Given that my connection closes, when the system detects the disconnection, then it should clean up the associated connection resources appropriately.

---

## 9. Administration and Security

### US-36: View Registered Users

**As an** administrator,  
**I want** to view registered users,  
**So that** I can oversee user accounts.

**Acceptance Criteria:**
- Given that I am an authorized administrator, when I request the user list, then the system should return the permitted user information.
- Given that I am not an administrator, when I request the administrative user list, then the system should deny access.
- Given that user information is sensitive, when the system returns user records, then it should avoid exposing unnecessary sensitive data.

### US-37: Update User Information as an Administrator

**As an** administrator,  
**I want** to update permitted user information,  
**So that** I can manage user accounts when necessary.

**Acceptance Criteria:**
- Given that I am an authorized administrator and provide valid changes, when I submit the update, then the system should save the permitted changes.
- Given that the target user does not exist, when I submit the update, then the system should return an appropriate not-found response.
- Given that I lack administrative permission, when I submit the request, then the system should deny access.

### US-38: Apply Rate Limiting

**As a** system administrator,  
**I want** the application to limit excessive requests,  
**So that** authentication and other protected operations are less vulnerable to abuse.

**Acceptance Criteria:**
- Given that a client exceeds the configured request limit, when it submits additional requests, then the system should restrict requests according to the rate-limiting policy.
- Given that a client remains within the configured limit, when it submits a valid request, then the system should process it normally.
- Given that a request is rate-limited, when the system responds, then it should return an appropriate status and error response.

### US-39: Record Audit Events

**As an** administrator,  
**I want** important application events to be recorded,  
**So that** relevant activities can be reviewed for accountability and troubleshooting.

**Acceptance Criteria:**
- Given that an operation is configured to generate an audit event, when the operation occurs, then the system should record the relevant audit information.
- Given that an audit record is stored, when an unauthorized user attempts to access it, then the system should deny access.
- Given that audit records contain sensitive information, when the system stores them, then it should avoid recording unnecessary secrets or credentials.

### US-40: Receive Consistent Error Responses

**As a** user,  
**I want** the application to return clear and consistent error responses,  
**So that** I can understand when a request fails without receiving unnecessary internal details.

**Acceptance Criteria:**
- Given that I submit invalid data, when validation fails, then the system should return an appropriate client-error response.
- Given that I request a resource that does not exist, when the request is processed, then the system should return an appropriate not-found response.
- Given that I attempt an unauthorized operation, when the request is processed, then the system should deny access.
- Given that an unexpected server error occurs, when the system responds, then it should not expose stack traces, credentials, or other internal implementation details.

---

## 10. Development and Quality Assurance

### US-41: Seed Development Data

**As a** developer,  
**I want** the development environment to contain sample users, classes, bookings, profiles, and audit records,  
**So that** I can test application functionality without creating every record manually.

**Acceptance Criteria:**
- Given that the development profile is enabled, when the application starts, then the configured sample data should be available.
- Given that the application runs outside the development profile, when it starts, then development-only sample data should not be loaded unintentionally.

### US-42: Test Business Logic

**As a** developer,  
**I want** automated tests for core business logic,  
**So that** I can identify regressions and maintain reliable functionality.

**Acceptance Criteria:**
- Given that a service has defined business rules, when its automated tests run, then the rules should be verified for valid and invalid scenarios.
- Given that a test fails, when the test suite completes, then the failure should be reported.
- Given that application behavior changes, when the test suite runs against the updated implementation, then relevant tests should help identify unintended changes.

---

## Summary

| Feature Area | Number of User Stories |
|---|---:|
| Registration and Authentication | 6 |
| User Profiles | 4 |
| Fitness Class Management | 7 |
| Fitness Class Bookings | 6 |
| Membership Plans | 4 |
| Membership Payments | 2 |
| Waitlist Management | 5 |
| Notifications | 1 |
| Administration and Security | 5 |
| Development and Quality Assurance | 2 |
| **Total** | **42** |