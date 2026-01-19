# Quick Start Guide - Generic AG-UI Frontend

## What is This?

A **completely generic** Angular frontend that implements:
- ✅ **AG-UI Protocol** - Standard agent communication protocol
- ✅ **A2UI Rendering** - Dynamic UI component rendering from metadata
- ✅ **Domain Agnostic** - Zero knowledge of hotels, bookings, or any specific domain

## 5-Minute Setup

### 1. Configure Your Application

Edit `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  wsUrl: 'ws://localhost:8080/agent',  // Your WebSocket endpoint
  apiUrl: 'http://localhost:8080/api', // Your REST API endpoint
  appId: 'my-app',                      // Your app identifier
  appTitle: 'My Agent',                 // Title shown in UI
  emptyStateHint: 'Ask me anything!'    // Initial prompt
};
```

### 2. Install and Run

```bash
cd angular-frontend
npm install
ng serve
```

Open http://localhost:4000

### 3. That's It!

The frontend will:
- Connect to your backend via WebSocket
- Handle AG-UI protocol messages
- Render A2UI components automatically
- Display markdown text as fallback

## Backend Requirements

Your backend must:

1. **Accept WebSocket connections** at the configured URL
2. **Implement AG-UI protocol** - Respond to these message types:
   ```json
   {
     "type": "chat",
     "message": {
       "sessionId": "...",
       "content": "User's message"
     }
   }
   ```

3. **Send AG-UI responses**:
   ```json
   {
     "type": "chat",
     "message": {
       "content": "Response text"
     }
   }
   ```

4. **(Optional) Include A2UI metadata** for rich UI:
   ```json
   {
     "type": "chat",
     "message": {
       "content": "Fallback text"
     },
     "data": {
       "format": "a2ui",
       "version": "1.0",
       "components": [
         {
           "type": "card",
           "title": "Result",
           "content": "Details here..."
         }
       ]
     }
   }
   ```

## Supported A2UI Components

### Basic Usage

```java
// Backend example (Java/Spring)
A2UIMetadata metadata = new A2UIMetadata();

// Add a heading
metadata.addComponent(new HeadingComponent("Results"));

// Add a card
metadata.addComponent(new CardComponent(
  "Item Title",
  "Subtitle",
  "Content goes here"
));

// Add a button
metadata.addComponent(new ButtonComponent(
  "Click Me",
  "action_name",
  "primary"
));

return metadata;
```

### Component Types

| Component | Purpose | Example |
|-----------|---------|---------|
| `heading` | Title text | Search Results |
| `card` | Content box | Item details |
| `text` | Paragraph | Description text |
| `button` | Action | Book Now, Learn More |
| `list` | Bullet/numbered list | Features, steps |
| `grid` | Multi-column layout | Product grid |
| `table` | Tabular data | Pricing, specs |
| `textfield` | Input field | User input |
| `image` | Display image | Product photo |
| `status` | Status indicator | Success, Error |
| `divider` | Separator line | Visual break |
| `json` | JSON display | API response |

## AG-UI Protocol Events

The frontend handles these events automatically:

- `run_started` - Agent begins processing
- `run` - Agent execution update
- `chat` - Message exchange
- `error` - Error occurred
- `complete` - Processing complete

## Example Integrations

### E-Commerce Backend

```typescript
// environment.ts
{
  appTitle: 'Shopping Assistant',
  emptyStateHint: 'What product are you looking for?'
}
```

### Support Chatbot

```typescript
// environment.ts
{
  appTitle: 'Customer Support',
  emptyStateHint: 'How can we help you today?'
}
```

### Data Analysis Tool

```typescript
// environment.ts
{
  appTitle: 'Data Explorer',
  emptyStateHint: 'Ask a question about your data...'
}
```

## Customization

### Change Colors

Edit `src/styles.scss`:

```scss
:root {
  --primary-color: #your-color;
  --accent-color: #your-accent;
}
```

### Add Custom CSS

Edit `src/app/components/chat/chat.component.scss`:

```scss
.chat-container {
  // Your custom styles
}
```

### Add Custom A2UI Component

1. Add rendering logic in `src/app/services/a2ui-renderer.service.ts`:
   ```typescript
   case 'my-component':
     return this.renderMyComponent(component);
   ```

2. Add render method:
   ```typescript
   private renderMyComponent(component: A2UIComponent): string {
     return `<div class="my-component">${component.content}</div>`;
   }
   ```

3. Add styles in `chat.component.scss`:
   ```scss
   .my-component {
     // Styles here
   }
   ```

## Testing

### Test WebSocket Connection

Open browser console:
```javascript
// Should see:
"WebSocket connected"
"Session initialized: session-xxxxx"
```

### Test Message Flow

1. Type a message and send
2. Check console for:
   - Outgoing message
   - Incoming response
   - A2UI rendering (if applicable)

### Test A2UI Rendering

Send a message that triggers A2UI response from backend. Components should render automatically.

## Troubleshooting

### Connection Issues

**Problem**: WebSocket won't connect

**Solution**: 
- Verify `wsUrl` in environment.ts
- Check backend is running
- Check CORS settings
- Use browser DevTools Network tab

### A2UI Not Rendering

**Problem**: Components not showing

**Solution**:
- Verify backend sends `data.format = 'a2ui'`
- Check `data.components` array exists
- Look for console errors
- Verify component types are supported

### Styling Issues

**Problem**: Components look wrong

**Solution**:
- Clear browser cache
- Rebuild: `ng build`
- Check CSS class names
- Verify styles loaded in DevTools

## Production Deployment

### Build

```bash
ng build --configuration production
```

Output: `dist/frontend/`

### Docker

```bash
docker build -t my-agent-frontend .
docker run -p 80:80 my-agent-frontend
```

### Environment Variables

For production, edit `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  wsUrl: 'wss://your-domain.com/agent',
  apiUrl: 'https://your-domain.com/api',
  appId: 'prod-app',
  appTitle: 'Your App',
  emptyStateHint: 'Your hint...'
};
```

## Documentation

- [AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md) - Complete protocol reference
- [CONFIGURATION_GUIDE.md](./CONFIGURATION_GUIDE.md) - Detailed configuration
- [REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md) - Technical changes

## Need Help?

1. Check documentation files above
2. Review browser console for errors
3. Verify backend AG-UI implementation
4. Test WebSocket connection separately

## Key Takeaways

✅ Frontend is **100% generic** - No domain knowledge  
✅ **Zero code changes** needed for different domains  
✅ **Configure via environment** files only  
✅ **Backend drives** all business logic and UI  
✅ **Protocol-compliant** with AG-UI standard  
✅ **A2UI rendering** is automatic and generic  

---

**Happy Building!** 🚀

This frontend is ready to use with any AG-UI compliant backend, for any domain, without modification.
