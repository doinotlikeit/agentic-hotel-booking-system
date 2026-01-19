# Advanced A2UI Components - Usage Guide

## Table Component with Pagination, Sorting, and Filtering

### Features
✅ **Pagination** - Navigate through large datasets  
✅ **Sorting** - Click column headers to sort  
✅ **Filtering** - Search/filter across all columns  
✅ **Responsive** - Adapts to different screen sizes  

### Backend Usage (Java)

#### Basic Table
```java
List<String> headers = Arrays.asList("ID", "Name", "Email", "Status");
List<List<Object>> rows = Arrays.asList(
    Arrays.asList(1, "John Doe", "john@example.com", "Active"),
    Arrays.asList(2, "Jane Smith", "jane@example.com", "Active"),
    Arrays.asList(3, "Bob Wilson", "bob@example.com", "Inactive")
);

A2UIBuilder builder = A2UIBuilder.create()
    .addTable(headers, rows);
```

#### Table with Custom Page Size
```java
// Show 25 rows per page
A2UIBuilder builder = A2UIBuilder.create()
    .addTable(headers, rows, 25);
```

#### Filterable Table
```java
// Enable search/filter functionality
A2UIBuilder builder = A2UIBuilder.create()
    .addFilterableTable(headers, rows, 10);
```

#### Advanced Table Configuration
```java
A2UIBuilder builder = A2UIBuilder.create()
    .addTable(
        headers,           // Column headers
        rows,              // Data rows
        20,                // Page size
        true,              // Sortable
        true,              // Filterable
        true               // Pagination enabled
    );
```

### Table Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `headers` | List<String> | [] | Column headers |
| `rows` | List<List<Object>> | [] | Table data (2D array) |
| `pageSize` | Integer | 10 | Rows per page |
| `sortable` | Boolean | true | Enable column sorting |
| `filterable` | Boolean | false | Enable search/filter |
| `pagination` | Boolean | auto | Enable pagination (auto if rows > pageSize) |

### Frontend Display

The table automatically includes:
- **First/Prev/Next/Last** navigation buttons
- **Current page indicator** (e.g., "Page 2 of 5")
- **Total rows counter** (e.g., "45 total rows")
- **Sort indicators** (▲/▼) when sorting
- **Filter input** (when filterable is enabled)
- **Hover effects** on rows
- **Sticky header** that stays visible when scrolling

## JSON Component with Tree View

### Features
✅ **Tree View** - Expandable/collapsible JSON structure  
✅ **Raw View** - Traditional JSON display  
✅ **Both Modes** - Show raw and tree side-by-side  
✅ **Syntax Highlighting** - Color-coded by type  
✅ **Node Counts** - Shows object keys/array length  

### Backend Usage (Java)

#### Basic JSON (Raw Display)
```java
Map<String, Object> data = new HashMap<>();
data.put("name", "John Doe");
data.put("age", 30);
data.put("email", "john@example.com");

A2UIBuilder builder = A2UIBuilder.create()
    .addJsonData("User Data", data);
```

#### JSON Tree View (Collapsible)
```java
// Complex nested data
Map<String, Object> complexData = new HashMap<>();
complexData.put("user", Map.of(
    "id", 123,
    "name", "John Doe",
    "address", Map.of(
        "street", "123 Main St",
        "city", "Boston",
        "zip", "02101"
    )
));
complexData.put("orders", Arrays.asList(
    Map.of("id", 1, "total", 99.99),
    Map.of("id", 2, "total", 149.99)
));

// Tree view with collapsed nodes
A2UIBuilder builder = A2UIBuilder.create()
    .addJsonTree("API Response", complexData);
```

#### Both Raw and Tree Views
```java
A2UIBuilder builder = A2UIBuilder.create()
    .addJsonTree("Response Data", data, "both", true);
```

#### Only Tree View (Expanded)
```java
A2UIBuilder builder = A2UIBuilder.create()
    .addJsonTree("Response Data", data, "tree", false);
```

### JSON Display Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `"raw"` | Traditional JSON text | Simple data, copy-paste needs |
| `"tree"` | Expandable tree structure | Complex nested data |
| `"both"` | Show both views | Best for debugging, comparison |

### Tree View Features

- **Click to expand/collapse** - Click ▼/▶ icons
- **Color coding**:
  - 🔵 Keys (blue)
  - 🟢 Strings (green)
  - 🔵 Numbers (blue)
  - 🔴 Booleans (red)
  - 🟣 Null (purple)
- **Node counts** - Shows "5 keys" or "10 items"
- **Nested indentation** - Visual hierarchy

## Complete Example

### Backend: Search Results with Table and JSON

```java
@Service
public class SearchService {
    
    public Map<String, Object> searchUsers(String query) {
        // Get search results
        List<User> users = userRepository.search(query);
        
        // Build table data
        List<String> headers = Arrays.asList(
            "ID", "Name", "Email", "Role", "Status", "Last Login"
        );
        
        List<List<Object>> rows = users.stream()
            .map(u -> Arrays.asList(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.getStatus(),
                u.getLastLogin()
            ))
            .collect(Collectors.toList());
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("total", users.size());
        metadata.put("timestamp", Instant.now());
        
        // Build A2UI response
        A2UIBuilder builder = A2UIBuilder.create()
            .addHeading("Search Results")
            .addText(String.format("Found %d users matching '%s'", 
                users.size(), query), "body", "left")
            .addDivider()
            
            // Add filterable table
            .addFilterableTable(headers, rows, 15)
            
            // Add JSON metadata (tree view)
            .addJsonTree("Search Metadata", metadata, "both", true);
        
        return builder.build();
    }
}
```

### Backend: API Response with Detailed JSON

```java
public Map<String, Object> getOrderDetails(Long orderId) {
    Order order = orderRepository.findById(orderId);
    
    // Build detailed order object
    Map<String, Object> orderData = Map.of(
        "id", order.getId(),
        "customer", Map.of(
            "id", order.getCustomer().getId(),
            "name", order.getCustomer().getName(),
            "email", order.getCustomer().getEmail()
        ),
        "items", order.getItems().stream()
            .map(item -> Map.of(
                "id", item.getId(),
                "name", item.getName(),
                "quantity", item.getQuantity(),
                "price", item.getPrice()
            ))
            .collect(Collectors.toList()),
        "totals", Map.of(
            "subtotal", order.getSubtotal(),
            "tax", order.getTax(),
            "shipping", order.getShipping(),
            "total", order.getTotal()
        ),
        "status", order.getStatus(),
        "createdAt", order.getCreatedAt()
    );
    
    return A2UIBuilder.create()
        .addHeading("Order #" + orderId)
        .addStatus("Order is " + order.getStatus(), 
            order.getStatus().equals("completed") ? "success" : "info")
        
        // Show order details in tree view
        .addJsonTree("Order Details", orderData, "both", true)
        
        .build();
}
```

### Backend: Data Analysis Results

```java
public Map<String, Object> analyzeData(String dataset) {
    // Perform analysis
    AnalysisResult result = analysisService.analyze(dataset);
    
    // Summary statistics table
    List<String> statsHeaders = Arrays.asList("Metric", "Value");
    List<List<Object>> statsRows = Arrays.asList(
        Arrays.asList("Total Records", result.getTotalRecords()),
        Arrays.asList("Average", result.getAverage()),
        Arrays.asList("Median", result.getMedian()),
        Arrays.asList("Std Deviation", result.getStdDev()),
        Arrays.asList("Min Value", result.getMin()),
        Arrays.asList("Max Value", result.getMax())
    );
    
    // Detailed results table
    List<String> detailHeaders = Arrays.asList(
        "Category", "Count", "Percentage", "Min", "Max", "Avg"
    );
    List<List<Object>> detailRows = result.getCategoryStats().stream()
        .map(stat -> Arrays.asList(
            stat.getCategory(),
            stat.getCount(),
            String.format("%.1f%%", stat.getPercentage()),
            stat.getMin(),
            stat.getMax(),
            stat.getAvg()
        ))
        .collect(Collectors.toList());
    
    return A2UIBuilder.create()
        .addHeading("Analysis Results: " + dataset)
        
        // Summary stats (no pagination needed)
        .addText("Summary Statistics", "subheading", "left")
        .addTable(statsHeaders, statsRows, 10, false, false, false)
        
        .addDivider()
        
        // Detailed breakdown (with pagination and sorting)
        .addText("Detailed Breakdown", "subheading", "left")
        .addFilterableTable(detailHeaders, detailRows, 20)
        
        .addDivider()
        
        // Raw data (tree view for inspection)
        .addJsonTree("Raw Analysis Data", result, "tree", true)
        
        .build();
}
```

## Best Practices

### When to Use Tables

✅ **Use tables for:**
- Tabular data with clear columns
- Lists of items with multiple properties
- Data comparisons
- Structured results (search, reports, logs)
- When users need to sort/filter

❌ **Avoid tables for:**
- Single values or simple lists
- Deeply nested data
- Unstructured content

### When to Use JSON Tree View

✅ **Use JSON tree for:**
- Complex nested objects
- API responses
- Configuration data
- Debug information
- When structure matters

❌ **Avoid JSON tree for:**
- Simple key-value pairs (use cards instead)
- Large arrays of primitives (use tables)
- Data that needs formatting

### Performance Considerations

**Tables:**
- Frontend handles pagination (all data sent once)
- Recommended max: 1000 rows
- For larger datasets, implement backend pagination

**JSON Tree:**
- Trees start collapsed to improve performance
- Large objects (>100KB) may be slow
- Consider summarizing very large responses

## Styling Customization

### Table Styling

Edit `chat.component.scss`:

```scss
.a2ui-table {
  // Custom table styles
  font-size: 0.9rem;
  
  th {
    background: #your-color;
  }
  
  tbody tr:hover {
    background: #your-hover-color;
  }
}
```

### JSON Tree Styling

```scss
.a2ui-json-tree {
  // Custom tree styles
  
  .json-string {
    color: #your-color;
  }
  
  .json-number {
    color: #your-color;
  }
}
```

## Frontend Integration

The components render automatically when A2UI metadata is received. No frontend code changes needed!

The frontend includes:
- Inline JavaScript for pagination logic
- Event handlers for sorting and filtering
- Responsive layout adjustments
- Accessibility features (keyboard navigation)

## Testing

### Test Table Component

```java
@Test
public void testTableComponent() {
    List<String> headers = Arrays.asList("Col1", "Col2");
    List<List<Object>> rows = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
        rows.add(Arrays.asList("Value" + i, i));
    }
    
    Map<String, Object> result = A2UIBuilder.create()
        .addFilterableTable(headers, rows, 10)
        .build();
    
    // Verify structure
    assertTrue(result.containsKey("components"));
    // ...
}
```

### Test JSON Tree Component

```java
@Test
public void testJsonTreeComponent() {
    Map<String, Object> data = Map.of(
        "nested", Map.of("key", "value"),
        "array", Arrays.asList(1, 2, 3)
    );
    
    Map<String, Object> result = A2UIBuilder.create()
        .addJsonTree("Test", data, "both", true)
        .build();
    
    // Verify structure
    assertTrue(result.containsKey("components"));
    // ...
}
```

## Summary

These advanced components provide:

**Table Component:**
- ✅ Pagination (First/Prev/Next/Last)
- ✅ Column sorting (click headers)
- ✅ Global search/filter
- ✅ Row counts and page info
- ✅ Responsive design
- ✅ Hover effects

**JSON Tree Component:**
- ✅ Expandable/collapsible nodes
- ✅ Raw JSON view
- ✅ Dual view mode (raw + tree)
- ✅ Syntax highlighting
- ✅ Node counts
- ✅ Deep nesting support

Both components are fully generic and work with any data!
