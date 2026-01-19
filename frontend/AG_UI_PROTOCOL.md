# AG-UI Protocol Implementation

## Overview

This Angular frontend is a **generic, domain-agnostic implementation** of the AG-UI protocol and A2UI rendering system. It has **no knowledge** of any specific business domain (hotels, bookings, e-commerce, etc.) and focuses solely on:

1. **AG-UI Protocol** - Managing agent communication via WebSocket
2. **A2UI Rendering** - Interpreting and displaying UI metadata

## Architecture Principles

### 1. Complete Domain Independence
The frontend is 100% generic and reusable. All domain-specific logic resides in the backend agent.

### 2. AG-UI Protocol Events
The frontend handles these standard AG-UI events:

- `run_started` - Agent execution begins
- `run` - Agent execution event
- `chat` - Chat message exchange
- `error` - Error occurred
- `complete` - Agent execution complete

### 3. A2UI Component Rendering
The frontend renders generic UI components from metadata:

#### Text Components
- `heading` / `header` - Main headings (H2)
- `subheading` - Sub-headings (H3)
- `body` - Body text (paragraph)
- `caption` / `footer` - Caption/footer text
- `text` - Generic text with variants

#### Container Components
- `card` - Card container with title, subtitle, content
- `grid` - Grid layout with configurable columns

#### List Components
- `list` - Ordered or unordered lists

#### Interactive Components
- `button` - Clickable button with action
- `textfield` / `input` - Text input field

#### Visual Components
- `divider` - Horizontal divider
- `status` - Status indicator (success/error/warning/info)
- `image` - Image with caption

#### Data Components
- `list` - Ordered or unordered lists
- `json` - JSON data display with **tree view** (expandable/collapsible)
  - Mode options: `raw`, `tree`, or `both`
  - Syntax highlighting with color-coded types
  - Click to expand/collapse nested structures
  - Shows node counts (keys/items) when collapsed
- `table` - Tabular data with **pagination**, **sorting**, and **filtering**
  - First/Prev/Next/Last pagination controls
  - Click column headers to sort
  - Search/filter across all columns
  - Configurable rows per page
  - Displays current page and total row counts

> 📚 See [ADVANCED_COMPONENTS.md](./ADVANCED_COMPONENTS.md) for detailed usage examples

## Configuration

All domain-specific settings are configured via environment files:

### `environment.ts` / `environment.prod.ts`

```typescript
export const environment = {
  production: false,
  wsUrl: 'ws://localhost:8080/agent',     // WebSocket endpoint
  apiUrl: 'http://localhost:8080/api',    // REST API endpoint
  appId: 'generic-agent-app',              // Application identifier
  appTitle: 'AI Agent',                    // Display title
  emptyStateHint: 'Type your message...'   // Empty state hint
};
```

## Services

### AgUiService
**Purpose**: Handle AG-UI protocol communication

**Responsibilities**:
- Establish WebSocket connection
- Send/receive AG-UI messages
- Handle AG-UI events (run_started, run, chat, error, complete)
- Manage session state
- NO domain-specific logic

### A2uiRendererService
**Purpose**: Render A2UI metadata to HTML

**Responsibilities**:
- Parse A2UI metadata from messages
- Render generic UI components
- Convert A2UI to displayable HTML
- NO domain-specific logic

### WebSocketService
**Purpose**: Low-level WebSocket communication

**Responsibilities**:
- Manage WebSocket connection lifecycle
- Handle connection status
- Send/receive raw messages

### MarkdownService
**Purpose**: Parse markdown content

**Responsibilities**:
- Convert markdown to HTML
- Sanitize HTML output

## Component Structure

### ChatComponent
The main UI component that:
- Displays AG-UI messages
- Renders A2UI components when present
- Falls back to markdown rendering for text
- Provides message input
- Shows connection status
- **Contains NO domain-specific logic**

## A2UI Message Format

Messages with A2UI metadata follow this structure:

```json
{
  "type": "chat",
  "message": {
    "sessionId": "session-uuid",
    "messageId": "message-uuid",
    "content": "Text fallback",
    "type": "agent"
  },
  "data": {
    "format": "a2ui",
    "version": "1.0",
    "components": [
      {
        "type": "heading",
        "content": "Results"
      },
      {
        "type": "card",
        "title": "Item Title",
        "subtitle": "Item Subtitle",
        "content": "Item details..."
      },
      {
        "type": "button",
        "label": "Action",
        "action": "perform_action",
        "variant": "primary"
      }
    ]
  }
}
```

## Customization

To adapt this frontend for a specific application:

1. **Update Environment Variables**
   ```typescript
   appTitle: 'Your App Name'
   appId: 'your-app-id'
   emptyStateHint: 'Your custom hint...'
   ```

2. **Customize Styles** (optional)
   - Modify `chat.component.scss` for colors/fonts
   - Update `styles.scss` for global styles
   - Add custom A2UI component styles

3. **Backend Agent Implementation**
   - Implement your domain logic in the backend
   - Generate A2UI metadata for rich UI
   - Use AG-UI protocol events

## Testing Domain Agnosticism

To verify the frontend is truly generic:

1. Change `appTitle` and `appId` in environment
2. Connect to a different backend agent
3. Frontend should work without code changes

## Benefits

✅ **Reusable** - Works with any AG-UI compliant backend  
✅ **Maintainable** - Clear separation of concerns  
✅ **Scalable** - Add new A2UI components easily  
✅ **Flexible** - Configure via environment variables  
✅ **Protocol-Driven** - Follows AG-UI standard

## Example Use Cases

This generic frontend can be used for:
- Customer service chatbots
- E-commerce assistants
- Booking systems (hotels, flights, restaurants)
- Technical support agents
- Data analysis tools
- Administrative interfaces
- Any agent-based application

Simply implement your backend agent with the AG-UI protocol and A2UI metadata generation!
