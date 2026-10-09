# Product Contracts - Visual Regression Checklist

## Confluence References
- Product Contract Overview: `pages/72155868`
- Product Contract Create: `pages/72155870`
- Contract Status Flow: `pages/72155872`

---

## List View Checks

### Table Display
- [ ] Table loads with data (not empty "No data" state)
- [ ] Column headers visible and correct order
- [ ] Data rows properly formatted
- [ ] Status badges/indicators with correct colors
- [ ] Pagination visible when applicable

### Expected Columns
1. Contract Number
2. Customer Name
3. Product
4. Start Date
5. End Date
6. Status
7. Actions (if applicable)

### Action Buttons
- [ ] Create/Add button visible
- [ ] Export button visible (if feature enabled)
- [ ] Search field visible
- [ ] Filter options available

---

## Detail View Checks

### Header Section
- [ ] Contract number displayed prominently
- [ ] Status badge with correct color
- [ ] Customer name/link visible
- [ ] Edit button (for editable statuses)
- [ ] Delete button (for Draft status)

### Tabs
- [ ] General tab (default active)
- [ ] PODs tab visible
- [ ] Documents tab visible
- [ ] History/Audit tab visible

### General Tab Content
- [ ] Contract dates (Start, End)
- [ ] Product information
- [ ] Customer details
- [ ] Contract terms

### PODs Tab Content
- [ ] POD list/table
- [ ] Add POD button (if contract is editable)
- [ ] POD status indicators
- [ ] POD count matches expected

---

## Create Form Checks

### Required Fields (marked with *)
- [ ] Customer - required
- [ ] Product - required
- [ ] Start Date - required

### Optional Fields (no *)
- [ ] End Date - optional
- [ ] Notes/Comments - optional

### Form Behavior
- [ ] Save button disabled when form is empty
- [ ] Save button enabled when required fields filled
- [ ] Cancel button always enabled
- [ ] Validation messages on invalid input

---

## Status-Based UI

### Draft Status
- Edit button: visible
- Delete button: visible
- Activate button: visible

### Active Status
- Edit button: may be visible (limited fields)
- Delete button: hidden
- Terminate button: may be visible

### Terminated Status
- Edit button: hidden
- Delete button: hidden
- All action buttons: minimal
