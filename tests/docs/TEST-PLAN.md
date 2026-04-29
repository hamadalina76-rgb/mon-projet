# SpeedLine -- Master Test Plan

| Field              | Value                                                        |
|--------------------|--------------------------------------------------------------|
| **Document ID**    | SL-TP-2026-001                                               |
| **Version**        | 1.0                                                          |
| **Date**           | 2026-04-29                                                   |
| **Project**        | SpeedLine -- Automated Testing Robot                         |
| **Institution**    | Institut Superieur des Etudes Technologiques (ISET)          |
| **Type**           | PFE (Projet de Fin d'Etudes) Deliverable                     |
| **Classification** | Internal / Academic                                          |
| **Author(s)**      | Wassim Djobbi                                                |
| **Supervisor(s)**  | _[Academic Supervisor Name]_ / _[Company Supervisor Name]_   |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Scope](#2-scope)
3. [Test Strategy](#3-test-strategy)
4. [Critical Test Scenarios](#4-critical-test-scenarios)
5. [Environment Requirements](#5-environment-requirements)
6. [Entry and Exit Criteria](#6-entry-and-exit-criteria)
7. [Risk Matrix](#7-risk-matrix)
8. [Test Schedule](#8-test-schedule)
9. [Tools and Frameworks](#9-tools-and-frameworks)
10. [Deliverables Checklist](#10-deliverables-checklist)
11. [Approval](#11-approval)
12. [Revision History](#12-revision-history)

---

## 1. Executive Summary

SpeedLine is a cloud-native food delivery platform composed of 12 Spring Boot microservices, 2 Angular web applications, and 2 Flutter mobile applications, all deployed on Google Cloud Platform. The platform serves four distinct user roles -- customers, couriers, partners (restaurant owners), and administrators -- each interacting through dedicated frontends.

This document defines the **Master Test Plan** for the SpeedLine Automated Testing Robot, a comprehensive quality assurance framework that implements the **testing pyramid** across four layers: unit tests, integration/API tests, end-to-end tests, and performance tests, supplemented by dedicated security tests.

The testing robot is designed to:

- Provide repeatable, automated regression coverage for all critical business flows.
- Integrate with the GitLab CI/CD pipeline to enforce quality gates before every deployment.
- Generate Allure-based rich reporting for test traceability and stakeholder visibility.
- Reduce manual QA effort by at least 80% for regression cycles.
- Serve as the cornerstone deliverable for the PFE internship program.

The plan covers a **4-month internship timeline** (February 2026 -- May 2026) and targets a minimum of **90% automated coverage** of critical paths and **95% pass rate** on the stable test suite before the final defense.

---

## 2. Scope

### 2.1 In Scope

| Area                        | Details                                                                 |
|-----------------------------|-------------------------------------------------------------------------|
| **Backend Microservices**   | auth-service, order-service, delivery-service, payment-service, partner-service, promotion-service, review-service, notification-service, location-service, support-service, user-service, analytics-service |
| **Infrastructure Services** | API Gateway (Spring Cloud Gateway), Eureka Service Registry, Config Server |
| **Web Applications**        | Admin Panel (Angular), Partner Dashboard (Angular)                      |
| **Mobile Applications**     | Customer App (Flutter), Courier App (Flutter)                           |
| **API Testing**             | All REST endpoints exposed through the API Gateway                      |
| **E2E Testing**             | Critical user journeys across web (Playwright) and mobile (Patrol/integration_test) |
| **Performance Testing**     | Load, stress, and spike tests for high-traffic endpoints (k6)           |
| **Security Testing**        | Authentication, authorization, input validation, OWASP Top 10 coverage  |
| **CI/CD Integration**       | GitLab CI pipeline with automated test execution and quality gates       |
| **Test Reporting**          | Allure reports, k6 dashboards, coverage summaries                       |

### 2.2 Out of Scope

| Area                              | Rationale                                                        |
|-----------------------------------|------------------------------------------------------------------|
| Third-party payment gateway internals | Tested via mocks; real payment flows are the provider's responsibility |
| Google Cloud infrastructure SLA   | Platform reliability is Google's responsibility                  |
| SMS/push notification delivery    | Only API dispatch is tested; delivery is the carrier's domain    |
| Manual/exploratory testing        | This plan covers automated testing only                          |
| Database migration testing        | Handled by Flyway/Liquibase; not part of the robot               |
| Penetration testing               | Requires specialized tools and scope beyond PFE                  |
| Accessibility (WCAG) testing      | Deferred to future iterations                                    |
| Legacy browser support            | Testing targets Chrome (latest) as the primary browser           |

---

## 3. Test Strategy

The testing robot follows the **testing pyramid** model, emphasizing a large base of fast, isolated unit tests, a middle layer of integration/API tests, a thinner layer of end-to-end tests, and targeted performance and security tests at the top.

```
                    /\
                   /  \         Performance & Security Tests
                  /    \        (k6, Playwright security suite)
                 /------\
                /        \      E2E Tests
               /          \     (Playwright web, Patrol mobile)
              /------------\
             /              \   Integration / API Tests
            /                \  (Playwright API testing)
           /------------------\
          /                    \ Unit Tests
         /                      \(JUnit 5 + Mockito, Jasmine + Karma)
        /________________________\
```

### 3.1 Layer 1 -- Unit Tests

| Attribute         | Details                                                          |
|-------------------|------------------------------------------------------------------|
| **Objective**     | Validate individual classes, methods, and components in isolation |
| **Backend**       | JUnit 5 with Mockito for mocking dependencies                   |
| **Frontend Web**  | Jasmine with Karma for Angular component/service testing         |
| **Frontend Mobile** | Flutter `test` package with `mockito` (Dart)                  |
| **Coverage Target** | Minimum 80% line coverage on service and controller layers    |
| **Execution**     | On every commit via GitLab CI; locally via `mvn test`, `ng test`, `flutter test` |
| **Reporting**     | JaCoCo (Java), Istanbul/lcov (Angular), lcov (Flutter)           |

**Strategy:** Each microservice contains its own `src/test/java` directory with tests organized by layer (controller, service, repository). Angular apps have `.spec.ts` co-located with components. Flutter apps have a `test/` directory mirroring `lib/`.

### 3.2 Layer 2 -- Integration / API Tests

| Attribute         | Details                                                          |
|-------------------|------------------------------------------------------------------|
| **Objective**     | Validate API contracts, request/response schemas, inter-service communication |
| **Framework**     | Playwright Test (API testing mode, no browser required)          |
| **Test Directory**| `tests/playwright/api/`                                          |
| **Coverage**      | All 14 API spec files covering every microservice                |
| **Auth Strategy** | `ApiClient` helper class handles JWT login and token management  |
| **Execution**     | GitLab CI `api-tests` project; runs headlessly via `npx playwright test --project=api-tests` |
| **Reporting**     | Allure Playwright reporter + HTML report                         |

**API test files:**

| File                          | Service Covered           |
|-------------------------------|---------------------------|
| `auth.api.spec.ts`           | Auth Service              |
| `orders.api.spec.ts`         | Order Service             |
| `delivery.api.spec.ts`       | Delivery Service          |
| `payments.api.spec.ts`       | Payment Service           |
| `partners.api.spec.ts`       | Partner Service           |
| `users.api.spec.ts`          | User Service              |
| `promotions.api.spec.ts`     | Promotion Service         |
| `reviews.api.spec.ts`        | Review Service            |
| `support.api.spec.ts`        | Support Service           |
| `notifications.api.spec.ts`  | Notification Service      |
| `location.api.spec.ts`       | Location Service          |
| `analytics.api.spec.ts`      | Analytics Service          |
| `security.api.spec.ts`       | Cross-cutting security    |
| `e2e-order-flow.api.spec.ts` | Full order lifecycle (API-level E2E) |

### 3.3 Layer 3 -- End-to-End Tests

| Attribute         | Details                                                          |
|-------------------|------------------------------------------------------------------|
| **Objective**     | Validate complete user journeys through the actual UI            |
| **Web Framework** | Playwright Test with Chromium                                    |
| **Mobile Framework** | Flutter `integration_test` + Patrol for native interactions   |
| **Auth Setup**    | Dedicated setup projects (`admin-auth-setup`, `partner-auth-setup`) store authenticated state in `fixtures/.auth/` |
| **Execution**     | GitLab CI with retries (2 on CI); locally via `npx playwright test --project=admin-panel` |
| **Configuration** | 60s test timeout, 10s expect timeout, 15s action timeout, 30s navigation timeout |
| **Artifacts**     | Screenshots on failure, video on first retry, trace on first retry |

**Web E2E test suites:**

| Suite                        | Application         | Scenarios                                 |
|------------------------------|---------------------|-------------------------------------------|
| `admin-panel/auth.spec.ts`   | Admin Panel        | Login, logout, session management          |
| `admin-panel/dashboard.spec.ts` | Admin Panel     | Dashboard widgets, metrics, charts         |
| `admin-panel/orders.spec.ts` | Admin Panel        | Order list, details, status management     |
| `admin-panel/users.spec.ts`  | Admin Panel        | User CRUD, role management                 |
| `admin-panel/partners.spec.ts` | Admin Panel      | Partner list, approval, details            |
| `partner-dashboard/auth.spec.ts` | Partner Dashboard | Partner login, session                  |
| `partner-dashboard/dashboard.spec.ts` | Partner Dashboard | Revenue, order stats            |
| `partner-dashboard/orders.spec.ts` | Partner Dashboard | Incoming orders, accept/reject       |
| `partner-dashboard/menu.spec.ts` | Partner Dashboard | Menu item CRUD, categories            |

**Mobile E2E test suites:**

| Suite                     | Application   | Scenarios                                    |
|---------------------------|---------------|----------------------------------------------|
| Customer App integration  | Customer App  | Browse, order, pay, track, review            |
| Courier App integration   | Courier App   | Accept delivery, navigate, complete delivery |

### 3.4 Layer 4 -- Performance Tests

| Attribute         | Details                                                          |
|-------------------|------------------------------------------------------------------|
| **Objective**     | Validate system behavior under load, stress, and spike conditions |
| **Framework**     | k6 by Grafana Labs                                               |
| **Test Types**    | Load test (gradual ramp), stress test (beyond capacity), spike test (sudden burst) |
| **Key Endpoints** | Auth login, order creation, order listing, delivery tracking, partner menu retrieval |
| **Thresholds**    | p95 response time < 500ms, error rate < 1%, throughput > 100 RPS  |
| **Execution**     | Scheduled CI pipeline (nightly) or manual trigger                 |
| **Reporting**     | k6 Cloud or InfluxDB + Grafana dashboards                        |

### 3.5 Security Tests

| Attribute         | Details                                                          |
|-------------------|------------------------------------------------------------------|
| **Objective**     | Verify authentication, authorization, and input validation       |
| **Framework**     | Playwright API tests (`security.api.spec.ts`)                    |
| **Coverage**      | Unauthenticated access rejection, invalid/expired JWT handling, role-based access control, SQL injection prevention, XSS prevention, CORS policy enforcement, rate limiting |
| **Execution**     | Runs as part of the `api-tests` project in every CI pipeline     |

---

## 4. Critical Test Scenarios

The following table catalogs all critical test scenarios across the platform. Each scenario is assigned a unique identifier, priority level, test type, and current implementation status.

**Priority Legend:** P0 = Blocker (must pass for release), P1 = Critical, P2 = Major, P3 = Minor

**Type Legend:** UNIT = Unit Test, API = Integration/API Test, E2E = End-to-End Test, PERF = Performance Test, SEC = Security Test

**Status Legend:** IMPL = Implemented, PEND = Pending, PROG = In Progress

### 4.1 Authentication Service

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| AUTH-001 | Login with valid credentials returns JWT             | P0       | API  | IMPL   |
| AUTH-002 | Login with invalid credentials returns 401           | P0       | API  | IMPL   |
| AUTH-003 | Login with empty email is rejected                   | P1       | API  | IMPL   |
| AUTH-004 | Login with empty password is rejected                | P1       | API  | IMPL   |
| AUTH-005 | Customer registration with valid data                | P0       | API  | IMPL   |
| AUTH-006 | Registration with duplicate email is rejected        | P1       | API  | IMPL   |
| AUTH-007 | Registration with weak password is rejected          | P1       | API  | IMPL   |
| AUTH-008 | OTP request for phone verification                   | P0       | API  | PEND   |
| AUTH-009 | OTP validation with correct code                     | P0       | API  | PEND   |
| AUTH-010 | OTP validation with expired/incorrect code           | P1       | API  | PEND   |
| AUTH-011 | Password reset request via email                     | P1       | API  | IMPL   |
| AUTH-012 | Password reset with valid token                      | P1       | API  | IMPL   |
| AUTH-013 | Password reset with expired token is rejected        | P2       | API  | PEND   |
| AUTH-014 | Token refresh with valid refresh token               | P0       | API  | IMPL   |
| AUTH-015 | Token refresh with expired refresh token             | P1       | API  | IMPL   |
| AUTH-016 | Logout invalidates session/token                     | P0       | API  | IMPL   |
| AUTH-017 | Admin Panel login E2E flow                           | P0       | E2E  | IMPL   |
| AUTH-018 | Partner Dashboard login E2E flow                     | P0       | E2E  | IMPL   |
| AUTH-019 | Admin Panel logout E2E flow                          | P1       | E2E  | IMPL   |
| AUTH-020 | Auth service unit tests (token generation, validation) | P0     | UNIT | IMPL   |

### 4.2 Order Lifecycle

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| ORD-001  | Customer creates a new order                         | P0       | API  | IMPL   |
| ORD-002  | Admin lists all orders with pagination               | P0       | API  | IMPL   |
| ORD-003  | Get order details by ID                              | P0       | API  | IMPL   |
| ORD-004  | Partner accepts incoming order                       | P0       | API  | IMPL   |
| ORD-005  | Partner rejects incoming order                       | P1       | API  | IMPL   |
| ORD-006  | Partner marks order as preparing                     | P1       | API  | PEND   |
| ORD-007  | Partner marks order as ready for pickup              | P0       | API  | PEND   |
| ORD-008  | Courier picks up the order                           | P0       | API  | PEND   |
| ORD-009  | Courier delivers the order                           | P0       | API  | PEND   |
| ORD-010  | Customer cancels an order (before acceptance)        | P1       | API  | IMPL   |
| ORD-011  | Customer cancels an order (after acceptance)         | P1       | API  | PEND   |
| ORD-012  | Refund initiated on cancelled order                  | P1       | API  | PEND   |
| ORD-013  | Order status transitions validation (state machine)  | P0       | UNIT | IMPL   |
| ORD-014  | Full order lifecycle E2E (API-level)                 | P0       | API  | IMPL   |
| ORD-015  | Admin Panel -- view and manage orders                | P0       | E2E  | IMPL   |
| ORD-016  | Partner Dashboard -- receive and process orders      | P0       | E2E  | IMPL   |
| ORD-017  | Order creation under load (100 concurrent users)     | P0       | PERF | PEND   |
| ORD-018  | Order listing under load (500 concurrent users)      | P1       | PERF | PEND   |
| ORD-019  | Invalid order data is rejected (missing items)       | P1       | API  | IMPL   |
| ORD-020  | Order total calculation accuracy                     | P0       | UNIT | IMPL   |

### 4.3 Payment Service

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| PAY-001  | Process payment for an order                         | P0       | API  | IMPL   |
| PAY-002  | Payment with insufficient funds fails gracefully     | P1       | API  | IMPL   |
| PAY-003  | Payment with invalid card data is rejected           | P1       | API  | IMPL   |
| PAY-004  | Wallet top-up                                        | P1       | API  | PEND   |
| PAY-005  | Wallet balance inquiry                               | P1       | API  | IMPL   |
| PAY-006  | Payment via wallet balance                           | P0       | API  | PEND   |
| PAY-007  | Refund processing                                    | P0       | API  | IMPL   |
| PAY-008  | Refund to wallet                                     | P1       | API  | PEND   |
| PAY-009  | Payment history retrieval                            | P2       | API  | IMPL   |
| PAY-010  | Duplicate payment prevention (idempotency)           | P0       | API  | PEND   |
| PAY-011  | Payment processing unit tests                        | P0       | UNIT | IMPL   |
| PAY-012  | Payment processing under load                        | P1       | PERF | PEND   |

### 4.4 Delivery Service

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| DEL-001  | Assign courier to an order                           | P0       | API  | IMPL   |
| DEL-002  | Auto-assignment based on proximity                   | P1       | API  | PEND   |
| DEL-003  | Courier accepts delivery assignment                  | P0       | API  | IMPL   |
| DEL-004  | Courier rejects delivery assignment                  | P1       | API  | IMPL   |
| DEL-005  | Real-time courier location tracking                  | P0       | API  | IMPL   |
| DEL-006  | Route calculation between courier and destination    | P1       | API  | PEND   |
| DEL-007  | Delivery completion confirmation                     | P0       | API  | IMPL   |
| DEL-008  | Delivery status updates stream                       | P1       | API  | PEND   |
| DEL-009  | Estimated time of arrival calculation                | P2       | API  | PEND   |
| DEL-010  | Delivery reassignment on courier cancellation        | P1       | API  | PEND   |
| DEL-011  | Delivery tracking under load (1000 concurrent)       | P1       | PERF | PEND   |
| DEL-012  | Delivery service unit tests                          | P0       | UNIT | IMPL   |

### 4.5 Partner Management

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| PRT-001  | Partner registration with business details           | P0       | API  | IMPL   |
| PRT-002  | Admin approves partner registration                  | P0       | API  | IMPL   |
| PRT-003  | Admin rejects partner registration                   | P1       | API  | IMPL   |
| PRT-004  | Partner profile update                               | P1       | API  | IMPL   |
| PRT-005  | Menu item creation                                   | P0       | API  | IMPL   |
| PRT-006  | Menu item update (price, description, image)         | P1       | API  | IMPL   |
| PRT-007  | Menu item deletion                                   | P1       | API  | IMPL   |
| PRT-008  | Menu category management                             | P2       | API  | IMPL   |
| PRT-009  | Partner order listing and management                 | P0       | API  | IMPL   |
| PRT-010  | Partner revenue and analytics                        | P2       | API  | PEND   |
| PRT-011  | Admin Panel -- partner list and approval E2E         | P0       | E2E  | IMPL   |
| PRT-012  | Partner Dashboard -- menu management E2E             | P0       | E2E  | IMPL   |
| PRT-013  | Partner Dashboard -- order processing E2E            | P0       | E2E  | IMPL   |
| PRT-014  | Partner management unit tests                        | P0       | UNIT | IMPL   |

### 4.6 User Management

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| USR-001  | Admin lists all customers with pagination            | P0       | API  | IMPL   |
| USR-002  | Admin views customer details                         | P1       | API  | IMPL   |
| USR-003  | Admin creates a new user                             | P1       | API  | IMPL   |
| USR-004  | Admin updates user profile                           | P1       | API  | IMPL   |
| USR-005  | Admin deactivates a user                             | P1       | API  | IMPL   |
| USR-006  | Admin assigns/changes user roles                     | P0       | API  | IMPL   |
| USR-007  | Admin lists all couriers                             | P1       | API  | IMPL   |
| USR-008  | Admin lists all admins                               | P2       | API  | IMPL   |
| USR-009  | User search and filtering                            | P2       | API  | IMPL   |
| USR-010  | Admin Panel -- user CRUD E2E                         | P0       | E2E  | IMPL   |
| USR-011  | User service unit tests                              | P0       | UNIT | IMPL   |

### 4.7 Promotions and Coupons

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| PRM-001  | Create a new promotion/coupon                        | P0       | API  | IMPL   |
| PRM-002  | Validate a coupon code at checkout                   | P0       | API  | IMPL   |
| PRM-003  | Apply valid coupon to order total                    | P0       | API  | IMPL   |
| PRM-004  | Reject expired coupon                                | P1       | API  | IMPL   |
| PRM-005  | Reject coupon exceeding usage limit                  | P1       | API  | IMPL   |
| PRM-006  | List active promotions                               | P2       | API  | IMPL   |
| PRM-007  | Deactivate a promotion                               | P2       | API  | IMPL   |
| PRM-008  | Promotion with minimum order value enforcement       | P1       | API  | PEND   |
| PRM-009  | Promotion discount calculation accuracy              | P0       | UNIT | IMPL   |
| PRM-010  | Promotion service unit tests                         | P0       | UNIT | IMPL   |

### 4.8 Reviews and Ratings

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| REV-001  | Customer submits a review for a completed order      | P0       | API  | IMPL   |
| REV-002  | Review with rating (1-5 stars)                       | P0       | API  | IMPL   |
| REV-003  | Review listing for a partner                         | P1       | API  | IMPL   |
| REV-004  | Average rating calculation for partner               | P1       | API  | IMPL   |
| REV-005  | Review moderation by admin                           | P2       | API  | PEND   |
| REV-006  | Prevent duplicate reviews for same order             | P1       | API  | PEND   |
| REV-007  | Review service unit tests                            | P0       | UNIT | IMPL   |

### 4.9 Support Tickets

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| SUP-001  | Customer creates a support ticket                    | P0       | API  | IMPL   |
| SUP-002  | Admin lists all support tickets                      | P1       | API  | IMPL   |
| SUP-003  | Admin responds to a support ticket                   | P1       | API  | IMPL   |
| SUP-004  | Ticket status transitions (open -> in progress -> resolved -> closed) | P1 | API | IMPL |
| SUP-005  | Ticket priority assignment                           | P2       | API  | IMPL   |
| SUP-006  | Ticket escalation                                    | P2       | API  | PEND   |
| SUP-007  | Support service unit tests                           | P0       | UNIT | IMPL   |

### 4.10 Analytics and Reporting

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| ANL-001  | Dashboard overview metrics (total orders, revenue)   | P0       | API  | IMPL   |
| ANL-002  | Orders by date range report                          | P1       | API  | IMPL   |
| ANL-003  | Revenue by partner report                            | P1       | API  | IMPL   |
| ANL-004  | Courier performance metrics                          | P2       | API  | IMPL   |
| ANL-005  | Customer retention metrics                           | P2       | API  | PEND   |
| ANL-006  | Real-time order tracking dashboard data              | P1       | API  | PEND   |
| ANL-007  | Admin Panel -- dashboard widgets E2E                 | P1       | E2E  | IMPL   |
| ANL-008  | Partner Dashboard -- revenue overview E2E            | P1       | E2E  | IMPL   |
| ANL-009  | Analytics service unit tests                         | P0       | UNIT | IMPL   |

### 4.11 Location and Zones

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| LOC-001  | Create a delivery zone                               | P0       | API  | IMPL   |
| LOC-002  | Update delivery zone boundaries                      | P1       | API  | IMPL   |
| LOC-003  | Check if address is within delivery zone             | P0       | API  | IMPL   |
| LOC-004  | List available zones                                 | P1       | API  | IMPL   |
| LOC-005  | Geocoding address to coordinates                     | P1       | API  | PEND   |
| LOC-006  | Nearest partner lookup by location                   | P1       | API  | PEND   |
| LOC-007  | Location service unit tests                          | P0       | UNIT | IMPL   |

### 4.12 Notifications

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| NTF-001  | Send push notification on order status change        | P0       | API  | IMPL   |
| NTF-002  | Send email notification                              | P1       | API  | IMPL   |
| NTF-003  | Notification preferences management                  | P2       | API  | IMPL   |
| NTF-004  | Notification history retrieval                       | P2       | API  | IMPL   |
| NTF-005  | Bulk notification dispatch                           | P2       | API  | PEND   |
| NTF-006  | Google Pub/Sub message publishing verification       | P1       | API  | PEND   |
| NTF-007  | Notification service unit tests                      | P0       | UNIT | IMPL   |

### 4.13 Security Scenarios

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| SEC-001  | Reject requests without authentication token         | P0       | SEC  | IMPL   |
| SEC-002  | Reject requests with expired JWT                     | P0       | SEC  | IMPL   |
| SEC-003  | Reject requests with malformed auth header           | P0       | SEC  | IMPL   |
| SEC-004  | Customer cannot access admin-only endpoints          | P0       | SEC  | IMPL   |
| SEC-005  | Partner cannot access other partner's data           | P0       | SEC  | IMPL   |
| SEC-006  | SQL injection attempt is blocked                     | P0       | SEC  | IMPL   |
| SEC-007  | XSS payload in input fields is sanitized             | P0       | SEC  | IMPL   |
| SEC-008  | CORS policy enforcement                              | P1       | SEC  | IMPL   |
| SEC-009  | Rate limiting on auth endpoints                      | P1       | SEC  | PEND   |
| SEC-010  | Sensitive data not exposed in API responses           | P1       | SEC  | PEND   |
| SEC-011  | HTTPS enforcement                                    | P1       | SEC  | PEND   |
| SEC-012  | API Gateway request size limits                      | P2       | SEC  | PEND   |
| SEC-013  | JWT token tampering detection                        | P0       | SEC  | IMPL   |
| SEC-014  | Brute force login protection                         | P1       | SEC  | PEND   |

### 4.14 Performance Scenarios

| ID       | Description                                          | Priority | Type | Status |
|----------|------------------------------------------------------|----------|------|--------|
| PRF-001  | Auth login endpoint -- 200 concurrent users          | P0       | PERF | PEND   |
| PRF-002  | Order creation -- 100 concurrent users               | P0       | PERF | PEND   |
| PRF-003  | Order listing -- 500 concurrent users                | P1       | PERF | PEND   |
| PRF-004  | Partner menu retrieval -- 300 concurrent users       | P1       | PERF | PEND   |
| PRF-005  | Delivery tracking -- 1000 concurrent connections     | P0       | PERF | PEND   |
| PRF-006  | Stress test -- ramp to 2x expected peak              | P1       | PERF | PEND   |
| PRF-007  | Spike test -- sudden 5x traffic burst                | P1       | PERF | PEND   |
| PRF-008  | Soak test -- steady load for 30 minutes              | P2       | PERF | PEND   |
| PRF-009  | API Gateway throughput under load                    | P1       | PERF | PEND   |
| PRF-010  | Database connection pool under load                  | P2       | PERF | PEND   |

### Scenario Summary

| Category           | Total | P0 | P1 | P2 | Implemented | Pending |
|--------------------|-------|----|----|----|-------------|---------|
| Authentication     | 20    | 8  | 9  | 1  | 16          | 4       |
| Order Lifecycle    | 20    | 8  | 7  | 0  | 12          | 8       |
| Payment            | 12    | 4  | 5  | 1  | 7           | 5       |
| Delivery           | 12    | 4  | 5  | 1  | 7           | 5       |
| Partner Management | 14    | 6  | 4  | 2  | 12          | 2       |
| User Management    | 11    | 4  | 5  | 2  | 11          | 0       |
| Promotions         | 10    | 4  | 3  | 2  | 8           | 2       |
| Reviews            | 7     | 3  | 2  | 1  | 5           | 2       |
| Support            | 7     | 2  | 3  | 2  | 6           | 1       |
| Analytics          | 9     | 2  | 3  | 2  | 6           | 3       |
| Location           | 7     | 3  | 3  | 0  | 5           | 2       |
| Notifications      | 7     | 2  | 2  | 3  | 5           | 2       |
| Security           | 14    | 6  | 5  | 1  | 9           | 5       |
| Performance        | 10    | 3  | 5  | 2  | 0           | 10      |
| **TOTAL**          | **160** | **59** | **61** | **20** | **109** | **51** |

---

## 5. Environment Requirements

### 5.1 Test Environments

| Environment   | Purpose                   | URL Pattern                        | Data Policy          |
|---------------|---------------------------|------------------------------------|----------------------|
| **Local**     | Development testing       | `localhost:8080` (API), `localhost:4200` (Admin), `localhost:4201` (Partner) | Developer-managed |
| **Staging**   | Integration/E2E/Perf testing | `staging-api.speedline.tn`       | Seeded test data, reset weekly |
| **CI**        | Automated pipeline testing | Docker Compose (ephemeral)        | Fresh seed on each run |

### 5.2 Infrastructure Requirements

| Component              | Specification                                              |
|------------------------|------------------------------------------------------------|
| **Container Runtime**  | Docker 24+ with Docker Compose v2                          |
| **CI Runner**          | GitLab Runner with Docker executor (4 vCPU, 8 GB RAM min) |
| **Databases**          | PostgreSQL 15+ (one per microservice)                      |
| **Cache**              | Redis 7+                                                   |
| **Message Broker**     | Google Pub/Sub (emulator for local/CI)                     |
| **Service Registry**   | Eureka Server                                              |
| **Config Server**      | Spring Cloud Config (Git-backed)                           |
| **API Gateway**        | Spring Cloud Gateway                                       |
| **Browser**            | Chromium (bundled with Playwright)                          |
| **Node.js**            | 18 LTS or later                                            |
| **Java**               | JDK 17 or later                                            |
| **Flutter**            | 3.x stable channel                                         |

### 5.3 Test Data Requirements

| Data Type          | Strategy                                                  |
|--------------------|-----------------------------------------------------------|
| **User Accounts**  | Pre-seeded test accounts (admin, customer, partner, courier) defined in `helpers/test-data.ts` |
| **Orders**         | Created dynamically per test; cleaned up in `afterAll`     |
| **Partners/Menus** | Seeded via database migration scripts                      |
| **Payments**       | Mock payment gateway in test environments                  |
| **Locations**      | Static zone definitions in seed data                       |

---

## 6. Entry and Exit Criteria

### 6.1 Entry Criteria

The following conditions must be met before test execution begins for each cycle:

| #  | Criterion                                                               | Verification          |
|----|-------------------------------------------------------------------------|-----------------------|
| 1  | All microservices compile and build successfully                        | CI build stage green  |
| 2  | Docker Compose stack starts without errors                              | Health check passing  |
| 3  | Database migrations applied successfully                                | Flyway/Liquibase logs |
| 4  | Test data seeded in the target environment                              | Seed script exit code |
| 5  | API Gateway routes are accessible                                       | Smoke test passing    |
| 6  | Angular apps build and serve without errors                             | `ng build` exit code  |
| 7  | Test framework dependencies installed (`npm ci`)                        | Package lock resolved |
| 8  | Environment variables configured (`.env` file or CI variables)          | Config validation     |

### 6.2 Exit Criteria

Testing is considered complete when the following conditions are satisfied:

| #  | Criterion                                                               | Target                |
|----|-------------------------------------------------------------------------|-----------------------|
| 1  | All P0 scenarios pass                                                   | 100% pass rate        |
| 2  | All P1 scenarios pass                                                   | >= 95% pass rate      |
| 3  | Overall test pass rate                                                  | >= 95%                |
| 4  | Unit test code coverage (backend services)                              | >= 80% line coverage  |
| 5  | No open P0/P1 defects                                                   | 0 unresolved          |
| 6  | Performance thresholds met (p95 < 500ms, error rate < 1%)              | All k6 checks pass    |
| 7  | Security tests pass (no critical/high vulnerabilities)                  | 0 critical/high       |
| 8  | Test reports generated and archived                                     | Allure + k6 reports   |
| 9  | All test artifacts committed to the repository                          | Git verification      |

---

## 7. Risk Matrix

| ID   | Risk Description                                              | Likelihood | Impact   | Severity | Mitigation Strategy                                                  |
|------|---------------------------------------------------------------|------------|----------|----------|----------------------------------------------------------------------|
| R-01 | Microservice APIs change without notice, breaking API tests   | High       | High     | **Critical** | Contract-first development; API versioning; Allure history comparison |
| R-02 | Test environment instability (Docker resource exhaustion)      | Medium     | High     | **High**     | Resource limits in Docker Compose; CI runner sizing; health checks    |
| R-03 | Flaky E2E tests due to timing/async issues                    | High       | Medium   | **High**     | Explicit waits; retry policy (2 retries on CI); trace-on-retry       |
| R-04 | Test data pollution between parallel test runs                | Medium     | High     | **High**     | Isolated test data per suite; `afterAll` cleanup; unique identifiers  |
| R-05 | Insufficient time to implement all 160 scenarios              | Medium     | Medium   | **Medium**   | Prioritize P0 first; iterative implementation; weekly tracking        |
| R-06 | Third-party service downtime (Google Maps, payment)           | Low        | Medium   | **Medium**   | Mock external services in test environment; circuit breaker patterns  |
| R-07 | Mobile test environment setup complexity (emulators)           | Medium     | Medium   | **Medium**   | Use Patrol's native testing; Docker-based Android emulators for CI    |
| R-08 | k6 performance tests yield inconsistent results               | Medium     | Low      | **Low**      | Run on dedicated infrastructure; multiple runs with statistical analysis |
| R-09 | Team member unavailability during critical phases             | Low        | High     | **Medium**   | Documented processes; knowledge sharing; pair programming sessions    |
| R-10 | Database schema changes break existing tests                  | Medium     | Medium   | **Medium**   | Database migration versioning; test data abstraction layer            |

---

## 8. Test Schedule

The following schedule maps to a **4-month PFE internship** (February -- May 2026).

### 8.1 Phase Timeline

```
February 2026          March 2026            April 2026            May 2026
|---- Phase 1 ---------|---- Phase 2 ---------|---- Phase 3 ---------|---- Phase 4 --------|
  Foundation & Setup      Core Implementation    Advanced & Mobile      Finalization & Defense
  Weeks 1-4               Weeks 5-8              Weeks 9-12             Weeks 13-16
```

### 8.2 Phase Details

#### Phase 1: Foundation and Setup (Weeks 1--4, February 2026)

| Week | Activities                                                                  | Deliverables                        |
|------|-----------------------------------------------------------------------------|-------------------------------------|
| 1    | Environment setup; Docker Compose for all services; CI pipeline skeleton    | Working local environment           |
| 2    | Playwright project structure; helper utilities (`ApiClient`, test data)     | Test framework scaffolding          |
| 3    | Unit test setup for 3 core services (auth, order, payment)                 | JUnit 5 + Mockito test suites       |
| 4    | First API tests (auth, orders); Allure reporter integration                | AUTH-001 to AUTH-007, ORD-001 to ORD-003 |

#### Phase 2: Core Implementation (Weeks 5--8, March 2026)

| Week | Activities                                                                  | Deliverables                        |
|------|-----------------------------------------------------------------------------|-------------------------------------|
| 5    | API tests for payment, delivery, partner, user services                    | PAY-*, DEL-*, PRT-*, USR-* API tests |
| 6    | E2E test setup (auth fixtures, page objects); Admin Panel E2E tests        | Admin Panel E2E suite               |
| 7    | Partner Dashboard E2E tests; promotion, review, support API tests          | Partner Dashboard E2E suite         |
| 8    | Security test suite; notification, location, analytics API tests           | SEC-* test suite; full API coverage |

#### Phase 3: Advanced Testing and Mobile (Weeks 9--12, April 2026)

| Week | Activities                                                                  | Deliverables                        |
|------|-----------------------------------------------------------------------------|-------------------------------------|
| 9    | k6 performance test scripts; load and stress test scenarios                | PRF-001 to PRF-005 scripts          |
| 10   | Flutter integration test setup; Customer App test suite                     | Mobile test framework               |
| 11   | Courier App tests; spike and soak test scenarios; remaining API gaps        | Full mobile coverage; PRF-006 to PRF-010 |
| 12   | CI/CD pipeline finalization; quality gates; parallel execution optimization | Production-ready CI pipeline        |

#### Phase 4: Finalization and Defense (Weeks 13--16, May 2026)

| Week | Activities                                                                  | Deliverables                        |
|------|-----------------------------------------------------------------------------|-------------------------------------|
| 13   | Fix remaining test failures; address flaky tests; coverage gap analysis     | >= 95% pass rate achieved           |
| 14   | Test report generation; documentation finalization; metrics collection      | Final Allure + k6 reports           |
| 15   | PFE report writing; presentation preparation                               | Written report + slides             |
| 16   | Defense rehearsal; final adjustments; PFE defense                           | Successful defense                  |

### 8.3 Milestones

| Milestone                        | Target Date     | Success Criteria                            |
|----------------------------------|-----------------|---------------------------------------------|
| M1: Framework Ready              | Feb 28, 2026    | CI pipeline runs; first tests passing        |
| M2: Core API Coverage            | Mar 31, 2026    | All 14 API spec files implemented            |
| M3: Full E2E + Security Suite    | Apr 15, 2026    | All E2E suites + security tests passing      |
| M4: Performance Suite Complete   | Apr 30, 2026    | All k6 scenarios implemented and baselined   |
| M5: Final Delivery               | May 15, 2026    | All exit criteria met; reports generated      |
| M6: PFE Defense                  | May 25-31, 2026 | Successful defense presentation              |

---

## 9. Tools and Frameworks

| Category               | Tool / Framework          | Version   | Purpose                                           |
|------------------------|---------------------------|-----------|---------------------------------------------------|
| **API/E2E Testing**    | Playwright Test           | Latest    | API testing, web E2E testing, test orchestration   |
| **Unit Testing (Java)**| JUnit 5                   | 5.10+     | Backend microservice unit tests                    |
| **Mocking (Java)**     | Mockito                   | 5.x       | Dependency mocking in unit tests                   |
| **Unit Testing (Angular)** | Jasmine + Karma       | Latest    | Angular component and service testing              |
| **Unit Testing (Flutter)** | Flutter Test + Mockito | Latest    | Dart/Flutter unit and widget testing               |
| **Mobile E2E**         | Patrol                    | Latest    | Flutter native integration testing                 |
| **Mobile E2E**         | Flutter integration_test  | Latest    | Flutter widget integration testing                 |
| **Performance**        | k6 (Grafana)              | Latest    | Load, stress, spike, and soak testing              |
| **Coverage (Java)**    | JaCoCo                    | 0.8+      | Java code coverage reporting                       |
| **Coverage (JS/TS)**   | Istanbul / lcov           | Latest    | Angular and Playwright coverage                    |
| **Test Reporting**     | Allure Framework          | Latest    | Rich test reports with history and trends          |
| **CI/CD**              | GitLab CI                 | SaaS      | Pipeline automation and quality gates              |
| **Containerization**   | Docker + Docker Compose   | 24+       | Test environment provisioning                      |
| **Cloud Platform**     | Google Cloud Platform     | N/A       | Production and staging infrastructure              |
| **Database**           | PostgreSQL                | 15+       | Persistent data storage for all services           |
| **Cache**              | Redis                     | 7+        | Session management, caching                        |
| **Message Queue**      | Google Pub/Sub            | N/A       | Async event-driven communication                   |
| **Service Discovery**  | Eureka                    | Latest    | Microservice registration and discovery            |
| **Configuration**      | Spring Cloud Config       | Latest    | Centralized configuration management               |
| **API Gateway**        | Spring Cloud Gateway      | Latest    | Request routing, load balancing, security          |
| **Version Control**    | Git + GitLab              | Latest    | Source control and collaboration                   |
| **IDE**                | IntelliJ IDEA / VS Code   | Latest    | Development and debugging                          |

---

## 10. Deliverables Checklist

| #  | Deliverable                                           | Format          | Status  |
|----|-------------------------------------------------------|-----------------|---------|
| 1  | Master Test Plan (this document)                      | Markdown / PDF  | DONE    |
| 2  | Playwright API test suite (14 spec files)             | TypeScript      | DONE    |
| 3  | Playwright E2E test suite (9 spec files)              | TypeScript      | DONE    |
| 4  | Playwright configuration and fixtures                 | TypeScript      | DONE    |
| 5  | Helper utilities (ApiClient, test data, page objects) | TypeScript      | DONE    |
| 6  | JUnit 5 unit test suites (per microservice)           | Java            | PROG    |
| 7  | Jasmine unit test suites (Angular apps)               | TypeScript      | PROG    |
| 8  | Flutter unit test suites (mobile apps)                | Dart            | PEND    |
| 9  | Flutter integration/Patrol test suites                | Dart            | PEND    |
| 10 | k6 performance test scripts                           | JavaScript      | PEND    |
| 11 | Security test suite                                   | TypeScript      | DONE    |
| 12 | GitLab CI pipeline configuration (`.gitlab-ci.yml`)   | YAML            | PROG    |
| 13 | Docker Compose test environment                       | YAML            | DONE    |
| 14 | Allure test reports                                   | HTML            | PROG    |
| 15 | k6 performance reports and dashboards                 | HTML / Grafana  | PEND    |
| 16 | Test coverage reports (JaCoCo, Istanbul, lcov)        | HTML            | PROG    |
| 17 | Defect log and resolution tracker                     | Spreadsheet     | PROG    |
| 18 | PFE final report (rapport de stage)                   | PDF             | PEND    |
| 19 | PFE defense presentation                              | PowerPoint/PDF  | PEND    |

---

## 11. Approval

This test plan is reviewed and approved by the following stakeholders.

| Role                     | Name                          | Signature      | Date       |
|--------------------------|-------------------------------|----------------|------------|
| **Test Lead / Author**   | Wassim Djobbi                 | ______________ | __________ |
| **Academic Supervisor**  | _[Name]_                      | ______________ | __________ |
| **Company Supervisor**   | _[Name]_                      | ______________ | __________ |
| **Project Manager**      | _[Name]_                      | ______________ | __________ |

---

## 12. Revision History

| Version | Date       | Author         | Changes                          |
|---------|------------|----------------|----------------------------------|
| 1.0     | 2026-04-29 | Wassim Djobbi  | Initial version of the test plan |

---

*This document is a deliverable of the SpeedLine PFE project at ISET. It is subject to revision as the project evolves. All changes must be documented in the Revision History table above.*
