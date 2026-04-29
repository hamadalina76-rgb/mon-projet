# SpeedLine - Manual Test Scenarios

**Project:** SpeedLine Food Delivery Platform
**Document Type:** PFE Deliverable - Manual QA Test Plan
**Version:** 1.0
**Date:** 2026-04-29
**Author:** QA Team

---

## Purpose

This document defines manual test scenarios that complement the automated test suite. These scenarios cover areas that require human judgment, visual verification, real device behavior, or complex cross-application interactions that are impractical to automate reliably.

**Automated coverage already exists for:** Authentication flows, CRUD operations, API endpoint validation, order lifecycle (API level), security injection tests, pagination, and basic navigation. See `/tests/playwright/` for details.

---

## Legend

| Priority | Description |
|----------|-------------|
| **Critical** | Blocks release. Must pass before any deployment. |
| **High** | Core user experience. Must pass before production release. |
| **Medium** | Important but has workarounds. Can ship with known issues if documented. |
| **Low** | Nice to have. Polish and edge cases. |

| Status | Description |
|--------|-------------|
| **Pass** | Scenario verified successfully. |
| **Fail** | Scenario failed. Bug filed. |
| **Not Tested** | Not yet executed in current cycle. |

---

## 1. Admin Panel (Angular Web App)

### 1.1 Visual and UI Verification

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-001 | Dashboard layout renders correctly | Admin logged in. Desktop viewport (1920x1080). | 1. Navigate to dashboard. 2. Verify sidebar, header, KPI cards, and charts are visible. 3. Check that no elements overlap or overflow. 4. Verify consistent font sizes and colors match the design system. | All dashboard components render without visual defects. KPI cards align in a grid. Charts display with proper labels. | High | Not Tested |
| MTC-002 | Responsive layout on tablet | Admin logged in. Tablet viewport (768x1024). | 1. Resize browser to 768px width. 2. Navigate through dashboard, orders, partners, and users pages. 3. Verify sidebar collapses to hamburger menu. 4. Verify tables switch to card layout or scroll horizontally. | Layout adapts gracefully. No horizontal scrollbar on main content. Hamburger menu opens/closes sidebar. All content remains accessible. | Medium | Not Tested |
| MTC-003 | Responsive layout on mobile | Admin logged in. Mobile viewport (375x812). | 1. Resize browser to 375px width. 2. Navigate through all main sections. 3. Attempt to use filters and search. 4. Verify modals and dialogs fit the viewport. | All sections are usable on mobile. Modals do not overflow. Buttons and inputs are tap-friendly (minimum 44px touch target). | Medium | Not Tested |
| MTC-004 | Dark/light theme toggle | Admin logged in. Theme toggle available. | 1. Switch from light to dark theme. 2. Navigate to all major pages (dashboard, orders, partners, users, analytics, zones). 3. Check readability of text, charts, and maps. 4. Switch back to light theme. | Theme applies consistently across all pages. No illegible text or invisible elements. Charts and maps adapt colors appropriately. | Low | Not Tested |
| MTC-005 | RTL language support | Admin logged in. Arabic or RTL locale available. | 1. Switch to RTL language setting. 2. Navigate through main pages. 3. Verify sidebar mirrors to right side. 4. Verify form labels and inputs align correctly. | Layout mirrors completely. Text direction is correct. No overlapping elements. | Low | Not Tested |

### 1.2 Complex Form Validation

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-006 | Partner approval with document review | Admin logged in. Pending partner application exists with uploaded documents. | 1. Navigate to Partners > Pending. 2. Open a pending partner application. 3. View each uploaded document (ID, business license, tax certificate). 4. Zoom into documents and verify readability. 5. Click Approve and add approval notes. 6. Confirm the approval. | Documents render correctly (PDF and images). Zoom works without distortion. After approval, partner status changes to Active. Approval notes are saved. Partner receives notification. | Critical | Not Tested |
| MTC-007 | Partner rejection with reason | Admin logged in. Pending partner application exists. | 1. Open a pending partner application. 2. Review documents. 3. Click Reject. 4. Enter rejection reason (required field). 5. Attempt to submit without a reason. 6. Enter reason and submit. | Rejection without reason shows validation error. After rejection with reason, partner status changes to Rejected. Rejection reason is stored and visible in partner history. | High | Not Tested |
| MTC-008 | Create delivery zone with boundary validation | Admin logged in. Zones management accessible. | 1. Navigate to Zones. 2. Click Create Zone. 3. Enter zone name and delivery fee. 4. Attempt to save without drawing boundaries. 5. Draw zone on map. 6. Save zone. | Cannot save zone without boundaries. Drawn polygon is valid (no self-intersecting lines). Zone appears in the list after save. | High | Not Tested |
| MTC-009 | Bulk user status update | Admin logged in. Multiple users exist. | 1. Navigate to Users. 2. Select multiple users via checkboxes. 3. Choose "Suspend" from bulk actions. 4. Confirm the action. 5. Verify status changed for all selected users. | All selected users are suspended. Confirmation dialog shows count. Status updates reflect immediately in the list. | Medium | Not Tested |
| MTC-010 | Create promotion with complex rules | Admin logged in. | 1. Navigate to Promotions > Create. 2. Set promo code, discount type (percentage), value (25%), max discount cap (50 TND). 3. Set date range (future start, future end). 4. Set minimum order amount (20 TND). 5. Limit to specific zone. 6. Set maximum usage count (100). 7. Save and verify. | Promotion is created with all rules intact. Rules display correctly in promotion details. Validation prevents invalid combinations (e.g., percentage > 100). | High | Not Tested |

### 1.3 Map Interactions

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-011 | Draw delivery zone polygon on map | Admin logged in. Zone editor open. | 1. Open zone editor. 2. Click on map to start drawing polygon. 3. Add at least 5 points to create an irregular zone. 4. Close the polygon. 5. Edit a point by dragging. 6. Delete a point. 7. Save the zone. | Polygon draws smoothly. Points snap correctly. Editing and deleting points updates the shape in real time. Saved zone matches the drawn shape. | High | Not Tested |
| MTC-012 | Real-time courier tracking on map | Admin logged in. At least one active courier with GPS enabled. | 1. Navigate to courier tracking map. 2. Verify courier markers appear on the map. 3. Wait 30 seconds and observe marker movement. 4. Click a courier marker. 5. Verify courier info popup (name, current order, speed). | Courier markers update positions in near real-time (within 5 seconds). Popup shows accurate courier info. Multiple couriers display without marker overlap issues. | Critical | Not Tested |
| MTC-013 | Zone overlap detection | Admin logged in. Existing zone drawn. | 1. Create a new zone. 2. Draw boundaries that overlap with an existing zone. 3. Attempt to save. | System warns about zone overlap. Admin can choose to proceed or adjust boundaries. Overlap is visually highlighted on the map. | Medium | Not Tested |
| MTC-014 | Map zoom and pan performance | Admin logged in. Map with zones and couriers loaded. | 1. Zoom in to street level. 2. Zoom out to city level. 3. Pan across the entire coverage area. 4. Rapidly zoom in and out. | Map renders smoothly at all zoom levels. No tile loading delays beyond 2 seconds. Zone polygons redraw correctly at different zoom levels. No JavaScript errors in console. | Medium | Not Tested |

### 1.4 Report Export

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-015 | Export orders report as PDF | Admin logged in. Orders exist for the selected date range. | 1. Navigate to Analytics > Reports. 2. Select date range (last 30 days). 3. Click Export PDF. 4. Open the downloaded file. | PDF downloads successfully. Contains correct date range header. Data matches what is shown on screen. Tables are formatted correctly. SpeedLine branding is present. File size is reasonable. | High | Not Tested |
| MTC-016 | Export orders report as CSV | Admin logged in. Orders exist for the selected date range. | 1. Navigate to Analytics > Reports. 2. Select date range (last 30 days). 3. Click Export CSV. 4. Open the downloaded file in a spreadsheet application. | CSV downloads successfully. All columns have headers. Data matches the on-screen report. Special characters (accented names) are preserved. Dates format consistently. | High | Not Tested |
| MTC-017 | Export revenue report with filters | Admin logged in. Revenue data exists. | 1. Navigate to Analytics > Revenue. 2. Apply filters: specific partner, specific zone, date range. 3. Export as PDF. 4. Verify exported data respects applied filters. | Exported report only includes data matching the active filters. Filter criteria are listed in the report header. | Medium | Not Tested |
| MTC-018 | Large dataset export | Admin logged in. More than 10,000 orders exist. | 1. Select a wide date range that includes 10,000+ records. 2. Click Export CSV. 3. Monitor browser performance during export. | Export completes without browser freeze or crash. Progress indicator is shown. File contains all records. No truncation or data loss. | Medium | Not Tested |

### 1.5 Real-Time Dashboard Updates

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-019 | Dashboard KPIs update on new order | Admin logged in to dashboard. A customer account ready to place an order. | 1. Note current "Orders Today" count on dashboard. 2. Place a new order from customer app or API. 3. Observe the dashboard without refreshing. | "Orders Today" counter increments within 5 seconds. No page refresh needed. Counter animation is smooth. | Critical | Not Tested |
| MTC-020 | Live order feed | Admin logged in to orders page. | 1. Open the orders list. 2. Place a new order from another device/session. 3. Observe the orders list. | New order appears at the top of the list within 5 seconds. Order shows "New" badge or highlight. Optional: sound notification plays. | High | Not Tested |
| MTC-021 | Dashboard after network interruption | Admin logged in to dashboard. | 1. Note current dashboard state. 2. Disable network (airplane mode or disconnect). 3. Wait 10 seconds. 4. Re-enable network. 5. Observe dashboard behavior. | Dashboard shows a disconnection indicator when offline. After reconnection, data refreshes automatically. No stale data displayed. | High | Not Tested |

### 1.6 Multi-Tab Behavior

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-022 | Same admin in multiple tabs | Admin logged in. | 1. Open admin panel in Tab A. 2. Open admin panel in Tab B. 3. In Tab A, update an order status. 4. Switch to Tab B and observe. | Tab B reflects the updated order status (on next interaction or auto-refresh). No conflicts or errors. Session remains valid in both tabs. | Medium | Not Tested |
| MTC-023 | Logout in one tab | Admin logged in in two tabs. | 1. In Tab A, click Logout. 2. Switch to Tab B. 3. Try to perform any action. | Tab B redirects to login page or shows a session expired message. No unauthorized actions are possible after logout. | High | Not Tested |
| MTC-024 | Concurrent edit conflict | Two admin sessions open (different tabs or browsers). | 1. Both admins open the same partner profile for editing. 2. Admin A changes the partner name and saves. 3. Admin B changes the partner phone and saves. | Either optimistic concurrency shows a conflict warning to Admin B, or the last save wins with both changes preserved (not overwriting Admin A's name change). | Medium | Not Tested |

### 1.7 Browser Compatibility

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-025 | Full workflow on Chrome (latest) | Chrome latest version. Admin credentials. | 1. Login. 2. Navigate all main sections. 3. Create a zone on map. 4. Export a report. 5. Review partner application. | All features work as expected. No console errors. Consistent styling. | Critical | Not Tested |
| MTC-026 | Full workflow on Firefox (latest) | Firefox latest version. Admin credentials. | 1. Repeat MTC-025 steps on Firefox. | All features work as expected. No browser-specific rendering issues. | High | Not Tested |
| MTC-027 | Full workflow on Safari (latest) | Safari latest on macOS. Admin credentials. | 1. Repeat MTC-025 steps on Safari. | All features work as expected. Date pickers and file uploads function correctly (Safari has known quirks). | High | Not Tested |
| MTC-028 | Full workflow on Edge (latest) | Edge latest version. Admin credentials. | 1. Repeat MTC-025 steps on Edge. | All features work as expected. No Chromium-Edge-specific issues. | Medium | Not Tested |

---

## 2. Partner Dashboard (Angular Web App)

### 2.1 Menu Management with Image Upload

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-029 | Add menu item with image | Partner logged in. Menu section accessible. | 1. Navigate to Menu. 2. Click Add Item. 3. Fill in name, description, price, and category. 4. Upload a product image (JPEG, 2MB). 5. Preview the image. 6. Save the item. | Image uploads with a progress indicator. Preview shows the image correctly. After save, the item appears in the menu with thumbnail. Image is optimized/resized server-side. | Critical | Not Tested |
| MTC-030 | Upload oversized image | Partner logged in. | 1. Add a new menu item. 2. Attempt to upload an image larger than 10MB. | Upload is rejected with a clear error message indicating the maximum file size. Form does not submit. | Medium | Not Tested |
| MTC-031 | Upload unsupported file format | Partner logged in. | 1. Add a new menu item. 2. Attempt to upload a .bmp or .tiff file. | Upload is rejected with an error message listing supported formats (JPEG, PNG, WebP). | Medium | Not Tested |
| MTC-032 | Reorder menu categories | Partner logged in. Multiple menu categories exist. | 1. Navigate to Menu. 2. Drag a category to reorder it. 3. Save the new order. 4. Refresh the page. | Drag-and-drop is smooth. New order persists after refresh. Customer app reflects the updated category order. | Medium | Not Tested |
| MTC-033 | Bulk toggle item availability | Partner logged in. Multiple items exist. | 1. Select multiple items. 2. Toggle availability to "Unavailable." 3. Verify items show as unavailable. 4. Toggle back to "Available." | Bulk toggle updates all selected items. Visual indicator (grayed out, strikethrough, or badge) shows unavailable items. Changes reflect on customer app. | High | Not Tested |

### 2.2 Order Sound Notifications

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-034 | New order sound alert | Partner logged in. Dashboard open. Browser audio permissions granted. | 1. Keep the partner dashboard open. 2. Place a new order targeting this partner from a customer account. 3. Listen for sound notification. | A distinct alert sound plays when a new order arrives. Sound is audible and not too loud. Notification also appears visually. | Critical | Not Tested |
| MTC-035 | Sound alert when tab is in background | Partner logged in. Dashboard tab not in focus. | 1. Open partner dashboard. 2. Switch to a different browser tab. 3. Place a new order from another device. 4. Listen for sound. | Sound notification plays even when the tab is in the background (browser permitting). Visual notification badge appears on the tab title (e.g., "(1) New Order"). | High | Not Tested |
| MTC-036 | Mute/unmute notifications | Partner logged in. Sound settings available. | 1. Locate the sound toggle in settings or header. 2. Mute sounds. 3. Place a new order. 4. Verify no sound plays. 5. Unmute sounds. 6. Place another order. 7. Verify sound plays. | Mute setting persists across page refreshes. Visual notifications still appear when sound is muted. | Medium | Not Tested |

### 2.3 Print Kitchen Tickets

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-037 | Print order ticket | Partner logged in. An accepted order exists. Printer connected. | 1. Open an accepted order. 2. Click "Print Ticket." 3. Verify the print preview. 4. Print to a physical or PDF printer. | Print preview shows: order number, items with quantities, special instructions, customer name, delivery address, and timestamp. Layout fits standard thermal paper width (80mm). No cut-off content. | Critical | Not Tested |
| MTC-038 | Auto-print on order acceptance | Partner logged in. Auto-print enabled in settings. Printer connected. | 1. Enable auto-print in settings. 2. Accept a new incoming order. 3. Observe printer behavior. | Ticket prints automatically upon acceptance without manual intervention. Print dialog may or may not appear depending on browser settings. | High | Not Tested |
| MTC-039 | Print ticket with special characters | An order with special characters in items or instructions (Arabic text, accented characters). | 1. Accept an order with special characters. 2. Print the ticket. | All special characters render correctly on the printed ticket. No mojibake or missing glyphs. | Medium | Not Tested |

### 2.4 Real-Time Order Status Updates

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-040 | Order status changes reflect instantly | Partner logged in. An active order exists. | 1. View the active order on partner dashboard. 2. From the courier app, mark the order as "Picked Up." 3. Observe the partner dashboard. | Order status updates to "Picked Up" within 5 seconds without page refresh. Status badge color changes accordingly. | High | Not Tested |
| MTC-041 | Multiple simultaneous orders | Partner logged in. Peak simulation: 5+ orders incoming within 1 minute. | 1. Place 5 orders targeting this partner in quick succession. 2. Observe the dashboard. 3. Accept orders one by one. | All orders appear in the incoming queue. No orders are lost or duplicated. Accept action works reliably for each order. Sound plays for each new order. | Critical | Not Tested |

### 2.5 Profile Completion Flow

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-042 | Complete partner profile with documents | New partner account with incomplete profile. | 1. Login as a new partner. 2. Observe the profile completion wizard/banner. 3. Upload business license (PDF). 4. Upload ID document (JPEG). 5. Fill in bank details. 6. Set business hours. 7. Add menu items. 8. Submit for review. | Wizard guides partner step by step. Progress indicator shows completion percentage. Each uploaded document shows a preview. Submission triggers admin review notification. Partner sees "Pending Review" status. | Critical | Not Tested |
| MTC-043 | Resume incomplete profile | Partner with partially completed profile. | 1. Login. 2. Close the browser. 3. Login again later. | Previously completed steps are saved. Partner resumes from where they left off. No data loss. | High | Not Tested |

---

## 3. Customer App (Flutter Mobile)

### 3.1 GPS and Location Permission Flows

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-044 | First-time location permission request | Fresh app install. Location permission not yet granted. | 1. Open the app for the first time. 2. Observe the location permission dialog. 3. Grant "While Using the App" permission. | App explains why location is needed before the system dialog. After granting, the map centers on the user's location. Nearby restaurants load based on the position. | Critical | Not Tested |
| MTC-045 | Location permission denied | Fresh app install. | 1. Open the app. 2. Deny location permission. 3. Observe app behavior. | App shows a fallback: manual address entry or city selection. A non-intrusive banner suggests enabling location for a better experience. App does not crash. Core browsing functionality still works. | Critical | Not Tested |
| MTC-046 | Location permission revoked mid-session | App open with location granted. | 1. While the app is open, go to device Settings. 2. Revoke location permission for SpeedLine. 3. Return to the app. 4. Try to place an order. | App detects the permission change. Shows a prompt to re-enable location or enter address manually. Does not crash or show a blank map. | High | Not Tested |
| MTC-047 | GPS accuracy in dense urban area | Device in a dense urban environment. | 1. Open the app. 2. Check the delivery pin position on the map. 3. Manually adjust if inaccurate. 4. Compare with actual GPS coordinates. | Pin placement is within 50 meters of actual location. Manual adjustment via drag is smooth. Adjusted address updates the delivery fee if zone changes. | High | Not Tested |

### 3.2 Push Notification Delivery

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-048 | Order status push notifications | Customer with push notifications enabled. Active order placed. | 1. Place an order. 2. Lock the phone. 3. Wait for partner to accept the order. 4. Check notification shade. | Push notification arrives within 30 seconds of status change. Notification shows: order status, partner name, and estimated time. Tapping the notification opens the order tracking screen. | Critical | Not Tested |
| MTC-049 | Push notification with app killed | Active order exists. App is force-closed. | 1. Place an order. 2. Force close the app. 3. Wait for order status change. | Push notification still arrives (via FCM/APNs). Tapping notification re-opens the app at the order tracking screen. | Critical | Not Tested |
| MTC-050 | Notification permission denied | Customer denies push notification permission. | 1. Deny notification permission at install. 2. Place an order. 3. Check for in-app status updates. | App shows in-app banners or status updates as a fallback. A settings prompt suggests enabling notifications. Order tracking page shows real-time status without push. | High | Not Tested |
| MTC-051 | Promotional push notification | Customer has marketing notifications enabled. | 1. Send a promotional push from the admin panel. 2. Observe delivery on the customer device. 3. Tap the notification. | Notification arrives with correct promotional content and image. Tapping opens the relevant promotion or restaurant page via deep link. | Medium | Not Tested |

### 3.3 Payment Gateway Flow (Stripe)

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-052 | Successful card payment | Customer logged in. Items in cart. Valid test card available. | 1. Proceed to checkout. 2. Select card payment. 3. Enter Stripe test card (4242 4242 4242 4242). 4. Enter valid expiry and CVV. 5. Confirm payment. | Payment processes successfully. Order confirmation screen appears. Receipt is available. Payment status shows "Paid" in admin panel. | Critical | Not Tested |
| MTC-053 | Declined card payment | Customer at checkout. | 1. Proceed to checkout. 2. Enter Stripe test declined card (4000 0000 0000 0002). 3. Attempt payment. | Payment is declined with a user-friendly message ("Your card was declined. Please try another payment method."). Order is not created. Customer can retry with a different card. | Critical | Not Tested |
| MTC-054 | 3D Secure authentication | Customer at checkout. | 1. Enter Stripe test 3DS card (4000 0025 0000 3155). 2. Confirm payment. 3. Complete 3DS challenge in the popup/redirect. | 3DS authentication screen appears. After successful authentication, payment completes. Order is confirmed. | High | Not Tested |
| MTC-055 | Payment with saved card | Customer with a previously saved card. | 1. Proceed to checkout. 2. Select saved card. 3. Confirm payment. | Payment processes without re-entering card details. Only CVV or biometric confirmation required. Transaction completes successfully. | High | Not Tested |
| MTC-056 | Cash on delivery option | Customer at checkout. | 1. Proceed to checkout. 2. Select "Cash on Delivery." 3. Confirm order. | Order is created with payment method "COD." No payment charge is made. Courier sees "Collect cash" instruction. | High | Not Tested |
| MTC-057 | Apply promo code at checkout | Customer at checkout. Valid promo code exists. | 1. Add items to cart. 2. Enter a valid promo code. 3. Observe price update. 4. Complete payment. | Discount is applied and visible in the breakdown. Final charge reflects the discounted amount. Promo usage count increments in admin panel. | High | Not Tested |

### 3.4 Map Navigation Accuracy

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-058 | Live courier tracking on map | Order in "Out for Delivery" status. | 1. Open order tracking screen. 2. Observe courier marker on the map. 3. Watch the marker for 2 minutes. | Courier marker moves smoothly along roads. Estimated arrival time updates as courier approaches. Map auto-pans to keep courier visible. Route line updates if courier deviates. | Critical | Not Tested |
| MTC-059 | Restaurant location accuracy | Browsing restaurants on the map view. | 1. Switch to map view for restaurant browsing. 2. Tap restaurant markers. 3. Verify the address shown matches the pin location. | Restaurant pins are placed at correct addresses. Tapping shows restaurant name, rating, delivery time, and distance. | Medium | Not Tested |

### 3.5 App Background/Foreground Behavior

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-060 | App resume after backgrounding | Active order in progress. | 1. Open order tracking. 2. Press Home to background the app. 3. Wait 5 minutes. 4. Re-open the app. | App resumes on the tracking screen. Order status is current (not stale from 5 minutes ago). Map position and courier location are updated. No login prompt. | Critical | Not Tested |
| MTC-061 | App resume after long background | App was backgrounded for 30+ minutes. | 1. Open the app. 2. Background it for 30 minutes. 3. Re-open. | App refreshes data automatically. If the session token expired, silently refreshes it (or shows login). No crash or blank screen. | High | Not Tested |
| MTC-062 | Incoming call during checkout | Customer in the middle of checkout. | 1. Start checkout process. 2. Receive a phone call. 3. End the call. 4. Return to the app. | App returns to the checkout screen with all data preserved (cart, address, payment selection). No duplicate order risk. | High | Not Tested |

### 3.6 Deep Link Handling

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-063 | Deep link to restaurant | App installed. Deep link URL shared via messaging. | 1. Tap a deep link like `speedline://restaurant/123`. 2. If app is installed, observe behavior. 3. If not installed, observe fallback. | App opens directly to the restaurant page. If not installed, redirects to app store. If user is not logged in, shows restaurant page after login. | High | Not Tested |
| MTC-064 | Deep link to promotion | App installed. Promo deep link received via push. | 1. Tap a promotional deep link. 2. Observe which screen opens. | App opens to the promotion detail page or the relevant restaurant list with the promo applied. Promo code is auto-filled at checkout. | Medium | Not Tested |
| MTC-065 | Deep link to order tracking | App installed. Active order exists. | 1. Tap the tracking deep link from a notification. | App opens directly to the order tracking screen for the correct order. | High | Not Tested |

### 3.7 Offline Mode Behavior

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-066 | Browse while offline | App previously loaded with data. Device offline. | 1. Open the app with data loaded. 2. Turn off Wi-Fi and cellular data. 3. Attempt to browse restaurants. | Previously loaded restaurants/data remain visible (cached). New data requests show a "No connection" banner. Pull-to-refresh shows offline message. | Medium | Not Tested |
| MTC-067 | Place order while offline | Cart filled. Device goes offline before checkout. | 1. Add items to cart. 2. Disable network. 3. Attempt to checkout. | Clear error message: "No internet connection. Please check your connection and try again." Cart contents are preserved. Order is not lost. | High | Not Tested |
| MTC-068 | Network recovery during use | App showing offline state. | 1. While offline banner is shown, re-enable network. 2. Observe app behavior. | App automatically detects reconnection. Offline banner disappears. Data refreshes automatically. Pending actions (if any) complete. | High | Not Tested |

### 3.8 App Store Review Prompt

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-069 | Review prompt after successful delivery | Customer completes their 3rd successful order. | 1. Complete an order delivery. 2. Rate the order (4 or 5 stars). 3. Observe for an app store review prompt. | In-app review prompt appears at an appropriate time (not immediately, not blocking). Dismissing the prompt does not re-trigger for at least 30 days. Prompt follows Apple/Google guidelines. | Low | Not Tested |

---

## 4. Courier App (Flutter Mobile)

### 4.1 Background GPS Tracking Accuracy

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-070 | GPS tracking while delivering | Courier logged in with active delivery. Background location permission granted. | 1. Accept a delivery mission. 2. Navigate to the restaurant. 3. Lock the phone screen. 4. Continue walking/driving for 5 minutes. 5. Check admin panel for tracking data. | Courier position updates in admin panel every 5-10 seconds. Track follows the actual route (not jumping between points). Position accuracy within 20 meters. | Critical | Not Tested |
| MTC-071 | GPS tracking through tunnels/parking | Courier on an active delivery passing through a GPS-dead zone. | 1. During active delivery, enter a tunnel or underground parking. 2. Exit after 2 minutes. 3. Check position updates. | Position freezes gracefully during dead zone (no wild jumps). Tracking resumes within 15 seconds of re-acquiring GPS signal. Customer sees last known position, not an error. | High | Not Tested |
| MTC-072 | GPS tracking battery impact | Courier app with active tracking. Full battery. | 1. Start a delivery shift with full battery. 2. Track battery consumption over 1 hour of active use. 3. Compare with baseline (same use without SpeedLine). | Battery drain from SpeedLine tracking does not exceed 10% per hour above baseline. Battery optimization tips are available in the app. | High | Not Tested |

### 4.2 Battery Optimization Handling

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-073 | Battery saver mode impact | Android device with battery saver enabled. | 1. Enable battery saver mode. 2. Accept a delivery. 3. Navigate with the app backgrounded. 4. Check tracking accuracy in admin panel. | App warns the courier that battery saver may affect tracking. If tracking degrades, app shows a persistent notification asking to disable battery saver. Tracking remains functional at reduced frequency. | High | Not Tested |
| MTC-074 | Doze mode handling (Android) | Android device. App backgrounded for extended period between deliveries. | 1. Complete a delivery. 2. Wait 15 minutes without new missions (phone idle). 3. Receive a new delivery notification. | Push notification arrives despite Doze mode. App wakes up and shows the mission assignment. Response time is not significantly delayed. | Critical | Not Tested |
| MTC-075 | iOS background app refresh | iOS device. Background App Refresh enabled for SpeedLine. | 1. Accept a delivery. 2. Switch to another app (Maps for navigation). 3. Check tracking data in admin panel. | GPS tracking continues while the SpeedLine app is in the background. Location updates are sent to the server. No significant gap in tracking data. | Critical | Not Tested |

### 4.3 Navigation to Pickup/Delivery

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-076 | Navigate to restaurant for pickup | Courier with accepted mission. External maps app installed. | 1. Accept a delivery mission. 2. Tap "Navigate to Restaurant." 3. Observe which navigation app opens. | External navigation app (Google Maps, Apple Maps, or Waze) opens with the restaurant address as destination. Route is correct. Return to SpeedLine is easy (back button or notification). | Critical | Not Tested |
| MTC-077 | Navigate to customer for delivery | Courier has picked up the order. | 1. Confirm pickup at restaurant. 2. Tap "Navigate to Customer." 3. Follow navigation. | Navigation opens with the customer's delivery address. If the address includes apartment/floor details, they are shown in SpeedLine (not in the navigation app). | Critical | Not Tested |
| MTC-078 | Handle incorrect address | Courier arrives at delivery location but address is wrong. | 1. Arrive at the GPS pin location. 2. Customer is not there. 3. Use the in-app "Contact Customer" option. 4. If unreachable, use "Report Issue." | Contact options (call, chat) are easily accessible. If customer is unreachable after a timeout, courier can mark the delivery as "Failed - Wrong Address." Admin is notified. | High | Not Tested |

### 4.4 Photo Proof of Delivery

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-079 | Take delivery photo | Courier at delivery location. Order to be left at door. | 1. Arrive at delivery location. 2. Leave the order at the door. 3. Tap "Take Photo." 4. Camera opens. 5. Take a photo. 6. Review and confirm. | Camera opens within the app (not external camera app). Photo is clear and correctly oriented. Photo uploads with the delivery confirmation. Photo is visible to admin and customer. | Critical | Not Tested |
| MTC-080 | Delivery photo in low light | Delivery at night or in a dark hallway. | 1. Attempt to take a delivery photo in low light. 2. Flash activates (if available). 3. Review photo quality. | Flash activates automatically or can be toggled. Photo is usable for verification purposes. If quality is too low, a retake option is available. | Medium | Not Tested |
| MTC-081 | Photo upload on slow connection | Courier on a slow network (3G or poor signal). | 1. Take a delivery photo. 2. Confirm delivery. 3. Observe upload behavior. | Photo upload retries automatically on failure. Delivery confirmation is not blocked by photo upload. Photo uploads in background when connection improves. Status shows "Photo uploading..." | High | Not Tested |

### 4.5 Signature Capture

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-082 | Capture customer signature | Courier at delivery location. Signature required for this order. | 1. Tap "Collect Signature." 2. Hand the phone to the customer. 3. Customer signs on screen with finger. 4. Tap Confirm. | Signature pad is responsive to touch. Line is smooth (not jagged). Clear/redo option is available. Signature saves and is visible in order details on admin panel. | High | Not Tested |
| MTC-083 | Signature on wet/gloved finger | Customer has wet hands or is wearing gloves. | 1. Attempt to sign with wet fingers or thin gloves. 2. Assess responsiveness. | Signature pad has sufficient touch sensitivity. If unusable, courier can skip signature with a note and photo proof instead. | Low | Not Tested |

### 4.6 Sound Alerts for New Missions

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-084 | New delivery mission alert | Courier online and available. Phone in pocket (screen off). | 1. Set courier status to "Available." 2. Assign a delivery to this courier. 3. Listen for alert sound. | Loud, distinct alert sound plays. Sound overrides silent mode (or vibration pattern is strong). Push notification appears. Alert repeats or escalates if not acknowledged within 30 seconds. | Critical | Not Tested |
| MTC-085 | Mission alert while navigating | Courier using external navigation app for current delivery. | 1. Be navigating with Google Maps/Waze during an active delivery. 2. Receive a new stacked mission. 3. Observe notification behavior. | Alert sound plays over the navigation audio. Notification is visible as an overlay or heads-up notification. Courier can accept/reject without leaving the navigation app. | High | Not Tested |
| MTC-086 | Mission timeout | Courier receives a mission but does not respond. | 1. Send a mission to the courier. 2. Do not accept or reject for 60 seconds. | Mission times out and is reassigned to another courier. Courier receives a "Mission expired" notification. No penalty UX is shown (first timeout). | High | Not Tested |

### 4.7 Network Reconnection Handling

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-087 | Lose connection during delivery | Courier on active delivery. | 1. Disable network during delivery. 2. Continue driving for 5 minutes. 3. Re-enable network. | App shows an offline indicator. GPS positions are cached locally. On reconnection, all cached positions are uploaded to the server. Delivery status can be updated. | Critical | Not Tested |
| MTC-088 | Confirm delivery while offline | Courier at delivery location with no signal. | 1. Arrive at delivery location. 2. Lose network connection. 3. Take photo and confirm delivery. | Delivery confirmation is queued locally. Photo is saved locally. When connection restores, confirmation and photo are synced. Customer and admin see the update. | Critical | Not Tested |
| MTC-089 | Frequent network switching | Courier alternating between Wi-Fi and cellular. | 1. During an active delivery, move between Wi-Fi and cellular zones frequently. 2. Observe app behavior. | App handles network switches without disconnecting WebSocket. No duplicate position updates. No delivery status errors. Transition is seamless to the user. | Medium | Not Tested |

### 4.8 Multi-Delivery (Bundle) Workflow

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-090 | Accept bundled deliveries | Two orders from the same restaurant going to nearby addresses. | 1. Receive a bundled mission (2 orders). 2. Review both orders. 3. Accept the bundle. 4. Navigate to restaurant. 5. Pick up both orders. 6. Deliver first order. 7. Deliver second order. | Bundle shows both orders with addresses and sequence. Navigation optimizes the route. Each delivery is confirmed independently. Both customers see tracking. Earnings show bundle bonus. | Critical | Not Tested |
| MTC-091 | Partial delivery failure in bundle | Bundled delivery accepted. First delivery succeeds. | 1. Complete the first delivery in the bundle. 2. Arrive at second address but customer is unreachable. 3. Report the issue. | First delivery completes normally. Failed delivery triggers the return/support flow. Courier is not penalized for the partial failure. Each order has independent status. | High | Not Tested |
| MTC-092 | Bundle with different restaurants | Two orders from different restaurants in proximity. | 1. Receive a multi-pickup bundle. 2. Navigate to first restaurant. 3. Pick up order 1. 4. Navigate to second restaurant. 5. Pick up order 2. 6. Deliver both orders. | Route optimization considers both pickups. Estimated times account for multi-stop. Each restaurant sees only their order. Customers see updated ETAs based on multi-stop. | High | Not Tested |

---

## 5. Cross-Application Flows

### 5.1 Full Order Lifecycle

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-093 | Complete order lifecycle: customer to delivery | Customer app, partner dashboard, courier app, and admin panel all accessible. | 1. **Customer:** Browse restaurants, add items to cart, apply promo code, proceed to checkout, pay with card, confirm order. 2. **Partner:** Receive order notification with sound, review order details, accept order, print kitchen ticket, mark order as "Ready for Pickup." 3. **Courier:** Receive mission notification, accept mission, navigate to restaurant, confirm pickup, navigate to customer, take delivery photo, confirm delivery. 4. **Customer:** Rate the order and courier. 5. **Admin:** Verify the complete order timeline and payment reconciliation. | Every transition happens in real time. No step is lost. All parties see consistent order status. Payment is captured after delivery. Rating is associated with the correct order, partner, and courier. Admin timeline shows all events with accurate timestamps. | Critical | Not Tested |
| MTC-094 | Order cancellation by customer before acceptance | Customer with a just-placed order. | 1. Customer places an order. 2. Before partner accepts, customer cancels. 3. Observe behavior across all platforms. | Order status changes to "Cancelled" across all platforms. Payment is refunded (if pre-paid). Partner stops seeing the order. No courier is assigned. Admin sees cancellation reason and refund status. | Critical | Not Tested |
| MTC-095 | Order cancellation by partner | Customer order pending at partner. | 1. Customer places an order. 2. Partner rejects the order with reason ("out of stock"). 3. Observe customer app and admin panel. | Customer receives notification of rejection with reason. Payment is refunded. Customer is prompted to reorder from another restaurant. Admin logs the rejection reason. | High | Not Tested |
| MTC-096 | Order modification after placement | Customer has placed an order. Partner has not yet started preparing. | 1. Customer requests to add an item (via support or in-app). 2. Partner reviews and accepts the modification. 3. Price difference is charged/refunded. | Modified order shows updated items and price. Payment difference is handled correctly. Kitchen ticket can be reprinted with modifications highlighted. | Medium | Not Tested |

### 5.2 Real-Time Tracking Accuracy

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-097 | Customer sees courier moving in real time | Order in "Out for Delivery" status. Both customer and courier apps active. | 1. Customer opens tracking screen. 2. Courier begins driving toward customer. 3. Customer observes courier marker for 5 minutes. 4. Compare courier's actual route with what the customer sees. | Courier marker moves smoothly (no teleporting). Position delay is under 10 seconds. Route line updates if courier takes a different path. ETA adjusts dynamically. Courier icon rotates to show direction of travel. | Critical | Not Tested |
| MTC-098 | Tracking accuracy during peak hours | Multiple active deliveries in the system simultaneously (10+). | 1. Place multiple orders. 2. Assign different couriers. 3. Multiple customers track their deliveries simultaneously. | Each customer sees only their courier. No cross-contamination of tracking data. Server handles concurrent WebSocket connections without performance degradation. ETAs remain accurate. | Critical | Not Tested |
| MTC-099 | Tracking with poor GPS | Courier in an area with poor GPS (urban canyon, indoor). | 1. Courier passes through a low-GPS area. 2. Observe tracking on customer side. | Marker does not jump erratically. System smooths GPS data. ETA shows "Estimating..." if position is uncertain. When GPS recovers, tracking resumes normally. | High | Not Tested |

### 5.3 Notification Chain

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-100 | Full notification chain for an order | All apps set up with push notifications enabled. | 1. **Customer** places an order. 2. **Partner** receives "New Order" notification. 3. Partner accepts. **Customer** receives "Order Accepted" notification. 4. Partner marks ready. **System** assigns courier. **Courier** receives "New Mission" notification. 5. Courier picks up. **Customer** receives "Order Picked Up" notification. 6. Courier delivers. **Customer** receives "Order Delivered" notification. | Every notification arrives within 30 seconds of the triggering event. No notification is missing. Notification content is accurate and matches the current state. Notifications stack correctly (not replacing each other). | Critical | Not Tested |
| MTC-101 | Notification when app is in foreground | Customer app is open and in foreground. | 1. Trigger a status change while customer has the app open. | In-app notification banner appears (not just a push). Status updates on the current screen. No duplicate alert (push + in-app). | Medium | Not Tested |
| MTC-102 | Notification localization | User has app set to French/Arabic. | 1. Trigger various notifications. 2. Check notification text language. | All notifications appear in the user's selected language. No mixed-language content. Proper grammar and formatting. | Medium | Not Tested |

### 5.4 Payment Reconciliation

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-103 | End-of-day payment reconciliation | Multiple completed orders with various payment methods. | 1. Complete 5+ orders (mix of card and COD). 2. Navigate to admin payment dashboard. 3. Verify totals match. 4. Check partner payouts. 5. Check courier earnings. | Total revenue matches sum of individual orders. Card payments match Stripe dashboard. COD amounts are flagged for collection. Partner commission is calculated correctly. Courier delivery fees are accurate. Promo discounts are accounted for (SpeedLine absorbs, not partner). | Critical | Not Tested |
| MTC-104 | Refund processing | A completed order needs a refund (customer complaint). | 1. Admin initiates a full refund from the order page. 2. Check Stripe for refund status. 3. Check customer notification. 4. Verify partner payout adjustment. | Refund is processed in Stripe. Customer receives a "Refund Initiated" notification with the amount. Partner payout is adjusted. Refund appears in financial reports. | High | Not Tested |
| MTC-105 | Partial refund | Customer received a wrong item in a multi-item order. | 1. Admin initiates a partial refund for the wrong item. 2. Enter the refund amount. 3. Process the refund. | Only the specified amount is refunded. Order status remains "Delivered" (not cancelled). Refund line item appears in the order details. Financial reports reflect the partial refund. | High | Not Tested |

### 5.5 Promotion Codes Across Platforms

| ID | Scenario | Preconditions | Steps | Expected Result | Priority | Status |
|----|----------|---------------|-------|-----------------|----------|--------|
| MTC-106 | Create and use promotion end-to-end | Admin has access. Customer app ready. | 1. **Admin:** Create a new promo code (20% off, max 10 TND, valid for 7 days, first order only). 2. **Customer:** Enter the promo code at checkout. 3. Verify discount is applied correctly. 4. Complete the order. 5. **Admin:** Check promotion usage stats. | Discount applies: 20% of subtotal, capped at 10 TND. Promo usage count increments. Second use by the same customer is rejected with "Already used" message. Expired code is rejected after 7 days. | Critical | Not Tested |
| MTC-107 | Promo code zone restriction | Promo code valid only for Zone A. | 1. Customer in Zone A enters the code. 2. Customer in Zone B enters the same code. | Zone A customer gets the discount. Zone B customer sees "This promotion is not available in your area." | Medium | Not Tested |
| MTC-108 | Promo code maximum usage reached | Promo code with max 5 total uses. Already used 4 times. | 1. 5th customer uses the code (should work). 2. 6th customer tries to use the code. | 5th use succeeds. 6th attempt shows "This promotion has expired" or "No longer available." Admin sees usage count at max. | Medium | Not Tested |
| MTC-109 | Concurrent promo code usage | Two customers attempt to use the last available use simultaneously. | 1. Promo has 1 use remaining. 2. Two customers add it to checkout at the same time. 3. Both attempt to complete the order. | Only one customer gets the discount. The other receives an error message. No race condition allows over-redemption. | High | Not Tested |

---

## Execution Summary

| Section | Total Scenarios | Critical | High | Medium | Low |
|---------|----------------|----------|------|--------|-----|
| Admin Panel | 28 | 3 | 13 | 10 | 2 |
| Partner Dashboard | 15 | 4 | 6 | 4 | 1 |
| Customer App | 26 | 8 | 11 | 6 | 1 |
| Courier App | 23 | 8 | 10 | 4 | 1 |
| Cross-Application | 17 | 7 | 6 | 4 | 0 |
| **Total** | **109** | **30** | **46** | **28** | **5** |

---

## Test Environment Requirements

| Requirement | Details |
|-------------|---------|
| **Admin Panel** | Chrome 120+, Firefox 120+, Safari 17+, Edge 120+. Desktop and tablet viewports. |
| **Partner Dashboard** | Chrome 120+ (primary). Thermal receipt printer for ticket tests. |
| **Customer App (Android)** | Physical device, Android 12+. Google Play Services enabled. Test Stripe keys configured. |
| **Customer App (iOS)** | Physical device, iOS 16+. APNs configured for push notifications. |
| **Courier App (Android)** | Physical device, Android 12+. Background location permission. Battery saver testable. |
| **Courier App (iOS)** | Physical device, iOS 16+. Background App Refresh enabled. |
| **Network** | Ability to simulate offline, 3G, and network switching conditions. |
| **Accounts** | Admin, partner, customer, and courier test accounts pre-created and seeded. |
| **Stripe** | Test mode with test card numbers configured. |

---

## Appendix: Relationship to Automated Tests

This manual test plan is designed to complement the automated suite located in `/tests/playwright/`. The following areas are covered by automation and are **excluded** from this document:

- API endpoint request/response validation (`/tests/playwright/api/`)
- Authentication login/logout flows (`auth.spec.ts`)
- Basic CRUD navigation (`dashboard.spec.ts`, `orders.spec.ts`, `users.spec.ts`, `partners.spec.ts`)
- Menu item creation (basic flow) (`menu.spec.ts`)
- SQL injection and security header tests (`security.api.spec.ts`)
- API-level order lifecycle (`e2e-order-flow.api.spec.ts`)

This manual plan focuses on what automation cannot reliably cover: visual rendering, real device behavior, hardware interactions (GPS, camera, printer, sound), payment gateway UX, real-time WebSocket updates perceived by users, and end-to-end multi-application workflows.
