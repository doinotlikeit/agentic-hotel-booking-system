# A2UI Components Visual Reference

## Table Component

### Features Demonstration

```
┌─────────────────────────────────────────────────────────────┐
│ Filter: [Search table...                         ] 🔍       │
├─────────────────────────────────────────────────────────────┤
│ ID ↕  │ Name ↕        │ Email ↕               │ Status ↕   │
├───────┼───────────────┼───────────────────────┼────────────┤
│ 1     │ John Doe      │ john@example.com      │ Active     │
│ 2     │ Jane Smith    │ jane@example.com      │ Active     │
│ 3     │ Bob Wilson    │ bob@example.com       │ Inactive   │
│ 4     │ Alice Johnson │ alice@example.com     │ Active     │
│ 5     │ Charlie Brown │ charlie@example.com   │ Pending    │
│ 6     │ Diana Prince  │ diana@example.com     │ Active     │
│ 7     │ Eve Anderson  │ eve@example.com       │ Active     │
│ 8     │ Frank Miller  │ frank@example.com     │ Inactive   │
│ 9     │ Grace Lee     │ grace@example.com     │ Active     │
│ 10    │ Henry Ford    │ henry@example.com     │ Active     │
├─────────────────────────────────────────────────────────────┤
│  ⏮ First  ◀ Prev   Page 1 of 5 (45 rows)  Next ▶  Last ⏭  │
└─────────────────────────────────────────────────────────────┘

Features:
✅ Click column headers (↕) to sort
✅ Type in filter box to search
✅ Navigate pages with buttons
✅ Hover over rows for highlight
```

### Without Pagination (Small Dataset)

```
┌─────────────────────────────────────────────────────┐
│ Metric           │ Value                            │
├──────────────────┼──────────────────────────────────┤
│ Total Records    │ 1,234                            │
│ Average          │ 567.89                           │
│ Median           │ 543.21                           │
│ Std Deviation    │ 123.45                           │
│ Min Value        │ 100.00                           │
│ Max Value        │ 999.99                           │
├─────────────────────────────────────────────────────┤
│                    6 rows                           │
└─────────────────────────────────────────────────────┘
```

## JSON Tree Component

### Tree View (Collapsed)

```
┌──────────────────────────────────────────────────────────┐
│ Response Data                                             │
├──────────────────────────────────────────────────────────┤
│ Tree View                                                 │
│                                                           │
│ ▼ { 3 keys                                               │
│   "user": ▶ { 3 keys                                     │
│   "orders": ▶ [ 5 items                                  │
│   "metadata": ▶ { 4 keys                                 │
│ }                                                         │
└──────────────────────────────────────────────────────────┘

Click ▶ to expand nested structures
```

### Tree View (Expanded)

```
┌──────────────────────────────────────────────────────────┐
│ API Response                                              │
├──────────────────────────────────────────────────────────┤
│ Tree View                                                 │
│                                                           │
│ ▼ {                                                       │
│   "user": ▼ {                                            │
│     "id": 123                                            │
│     "name": "John Doe"                                   │
│     "address": ▼ {                                       │
│       "street": "123 Main St"                            │
│       "city": "Boston"                                   │
│       "zip": "02101"                                     │
│     }                                                     │
│   }                                                       │
│   "orders": ▼ [                                          │
│     0: ▼ {                                               │
│       "id": 1                                            │
│       "total": 99.99                                     │
│       "status": "completed"                              │
│     }                                                     │
│     1: ▶ { 3 keys                                        │
│   ]                                                       │
│   "timestamp": "2026-01-18T10:30:00Z"                   │
│ }                                                         │
└──────────────────────────────────────────────────────────┘

Features:
✅ Color-coded by type (strings, numbers, booleans)
✅ Click ▼/▶ to toggle nodes
✅ Shows counts when collapsed
✅ Indented for hierarchy
```

### Both Modes (Raw + Tree)

```
┌──────────────────────────────────────────────────────────┐
│ Debug Information                                         │
├──────────────────────────────────────────────────────────┤
│ Raw JSON                                                  │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ {                                                    │ │
│ │   "status": "success",                               │ │
│ │   "data": {                                          │ │
│ │     "results": [1, 2, 3]                            │ │
│ │   }                                                  │ │
│ │ }                                                    │ │
│ └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│ Tree View                                                 │
│                                                           │
│ ▼ { 2 keys                                               │
│   "status": "success"                                    │
│   "data": ▼ { 1 keys                                     │
│     "results": ▼ [ 3 items                               │
│       0: 1                                               │
│       1: 2                                               │
│       2: 3                                               │
│     ]                                                     │
│   }                                                       │
│ }                                                         │
└──────────────────────────────────────────────────────────┘

Perfect for debugging and comparison!
```

## Color Coding (JSON Tree)

```
▼ {
  "name": "John"      ← String (green)
  "age": 30          ← Number (blue)
  "active": true     ← Boolean (red)
  "role": null       ← Null (purple)
  "data": { ... }    ← Object (expand to see)
  "items": [ ... ]   ← Array (expand to see)
}
```

## Complete Example: Search Results

```
┌────────────────────────────────────────────────────────────┐
│ 🔍 Search Results                                          │
│                                                            │
│ Found 23 users matching "developer"                       │
│ ────────────────────────────────────────────────────────  │
│                                                            │
│ Filter: [Type to search...                    ] 🔍        │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ID↕ │ Name ↕        │ Email ↕           │ Role ↕     │ │
│ ├─────┼───────────────┼───────────────────┼────────────┤ │
│ │ 101 │ Sarah Dev     │ sarah@dev.com     │ Senior Dev │ │
│ │ 102 │ Mike Coder    │ mike@dev.com      │ Developer  │ │
│ │ 103 │ Lisa Backend  │ lisa@dev.com      │ Backend    │ │
│ │ 104 │ Tom Frontend  │ tom@dev.com       │ Frontend   │ │
│ │ 105 │ Anna Fullstack│ anna@dev.com      │ Full Stack │ │
│ │ 106 │ Chris DevOps  │ chris@dev.com     │ DevOps     │ │
│ │ 107 │ Emma Mobile   │ emma@dev.com      │ Mobile Dev │ │
│ │ 108 │ Ryan API      │ ryan@dev.com      │ API Dev    │ │
│ │ 109 │ Nina Data     │ nina@dev.com      │ Data Eng   │ │
│ │ 110 │ Paul Cloud    │ paul@dev.com      │ Cloud Arch │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │  ⏮ First ◀ Prev  Page 1 of 3 (23 rows)  Next ▶ Last ⏭ │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ ────────────────────────────────────────────────────────  │
│                                                            │
│ Search Metadata                                           │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Tree View                                              │ │
│ │                                                        │ │
│ │ ▼ { 4 keys                                             │ │
│ │   "query": "developer"                                 │ │
│ │   "total": 23                                          │ │
│ │   "timestamp": "2026-01-18T10:30:00Z"                  │ │
│ │   "filters": ▼ { 2 keys                                │ │
│ │     "role": "any"                                      │ │
│ │     "status": "active"                                 │ │
│ │   }                                                    │ │
│ │ }                                                      │ │
│ └────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

## Interaction Examples

### Table Interactions

1. **Sorting**
   ```
   Click "Name ↕" → Sorts A-Z
   Click again    → Sorts Z-A
   Column shows:    "Name ▲" or "Name ▼"
   ```

2. **Filtering**
   ```
   Type: "john"
   → Shows only rows containing "john" in any column
   → Updates row count: "Page 1 of 1 (3 rows)"
   ```

3. **Pagination**
   ```
   Click "Next ▶"  → Go to page 2
   Click "Last ⏭"  → Go to last page
   Click "Prev ◀"  → Go back one page
   Click "First ⏮" → Return to page 1
   ```

### JSON Tree Interactions

1. **Expand Node**
   ```
   Before: "data": ▶ { 5 keys
   After:  "data": ▼ {
             "key1": "value1"
             "key2": "value2"
             ...
           }
   ```

2. **Collapse Node**
   ```
   Before: "items": ▼ [
             0: "item1"
             1: "item2"
           ]
   After:  "items": ▶ [ 2 items
   ```

3. **Expand All**
   ```
   Start with all nodes collapsed (▶)
   Click each ▶ to drill down
   See full structure
   ```

## Responsive Behavior

### Desktop View
- Full table with all columns visible
- Comfortable row height
- Hover effects on rows

### Tablet View
- Table scrolls horizontally if needed
- Pagination stays visible
- Filter input full width

### Mobile View
- Table becomes scrollable horizontally
- Sticky header on scroll
- Pagination buttons stack vertically

## Accessibility

Both components include:

✅ **Keyboard Navigation**
- Tab through pagination buttons
- Enter to activate buttons
- Arrow keys for navigation

✅ **Screen Reader Support**
- ARIA labels on controls
- Descriptive button text
- Table headers properly marked

✅ **High Contrast**
- Clear visual indicators
- Sufficient color contrast
- Focus outlines visible

## Performance

### Table Component
- **Frontend pagination** - All data loaded once
- **Recommended**: < 1000 rows
- **Handles**: Sorting, filtering client-side
- **Fast**: No server round-trips for pagination

### JSON Tree Component
- **Lazy rendering** - Collapsed nodes don't render children
- **Recommended**: < 100KB JSON
- **Handles**: Deep nesting efficiently
- **Fast**: Expand/collapse is instant

## Summary

These components provide professional-grade data display:

**Table** = Spreadsheet-like experience  
**JSON Tree** = Developer-friendly data inspector  

Both are:
- ✅ Fully generic (domain-agnostic)
- ✅ Interactive (click, sort, filter, expand)
- ✅ Responsive (mobile-friendly)
- ✅ Accessible (keyboard + screen readers)
- ✅ Professional (polished UI/UX)

Perfect for displaying search results, API responses, analytics, and debug information!
