# Markdown Support in Chat UI

## Overview
The chat UI now supports full markdown rendering for agent responses, providing rich text formatting capabilities.

## Features

### Supported Markdown Syntax

1. **Headers** (H1-H6)
   ```markdown
   # Header 1
   ## Header 2
   ### Header 3
   ```

2. **Text Formatting**
   - **Bold**: `**text**` or `__text__`
   - *Italic*: `*text*` or `_text_`
   - ~~Strikethrough~~: `~~text~~`

3. **Links**
   ```markdown
   [Link text](https://example.com)
   ```

4. **Lists**
   - Unordered: `- item` or `* item`
   - Ordered: `1. item`

5. **Code**
   - Inline: `` `code` ``
   - Block:
   ````markdown
   ```language
   code block
   ```
   ````

6. **Blockquotes**
   ```markdown
   > Quote text
   ```

7. **Tables**
   ```markdown
   | Column 1 | Column 2 |
   |----------|----------|
   | Data 1   | Data 2   |
   ```

8. **Horizontal Rules**
   ```markdown
   ---
   ```

9. **Images**
   ```markdown
   ![Alt text](image-url)
   ```

## Implementation Details

### Components Added

1. **MarkdownService** (`src/app/services/markdown.service.ts`)
   - Parses markdown text using the `marked` library
   - Sanitizes HTML output for security
   - Provides methods to detect markdown syntax
   - Configures custom rendering options

2. **Updated ChatComponent**
   - Integrated MarkdownService
   - Updated template to render markdown content
   - Falls back to plain text if no markdown detected

### Styling

Comprehensive CSS styles added for:
- Headers with visual hierarchy
- Code blocks with syntax highlighting structure
- Tables with borders and alternating rows
- Blockquotes with left border accent
- Lists with proper indentation
- Links with hover effects
- Responsive design for all elements

### Security

- All markdown content is sanitized before rendering
- XSS protection via Angular's DomSanitizer
- External links open in new tab with `rel="noopener noreferrer"`

## Usage

### For Agent Developers

The agent can now return markdown-formatted responses:

```java
String response = """
    ## Search Results
    
    I found 3 hotels in Paris:
    
    1. **Grand Hotel Paris** ⭐ 4.5
       - Price: $250/night
       - *Luxury hotel near the Louvre*
    
    2. **Eiffel View Hotel** ⭐ 4.0
       - Price: $180/night
       - *Stunning views of the Eiffel Tower*
    
    Would you like to `book` one of these hotels?
    """;
```

### For Frontend Developers

The markdown rendering is automatic. No changes needed to existing code:

```typescript
// Messages are automatically rendered with markdown support
this.agUiService.sendMessage(userMessage);
```

## Dependencies

- **marked**: `^11.0.0` - Fast markdown parser
- **@types/marked**: TypeScript definitions

## Testing

To test markdown rendering:

1. Start the application
2. Send a message with markdown syntax
3. Observe formatted output in agent responses

Example test messages:
- "Show me hotels with **pricing** in a table"
- "Give me a list of amenities"
- "Format the results with headers and bullets"

## Browser Support

Works in all modern browsers that support:
- ES6+
- CSS Grid
- Flexbox
- CSS Variables

## Performance

- Markdown parsing is done on-demand
- Minimal overhead for plain text messages
- Efficient caching of parsed content
- No impact on WebSocket performance
