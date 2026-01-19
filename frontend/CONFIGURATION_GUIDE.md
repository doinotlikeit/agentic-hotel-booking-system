# Frontend Configuration Guide

## Quick Start: Customizing for Your Domain

This frontend is completely generic. To customize it for your specific application, follow these steps:

## Step 1: Update Environment Configuration

Edit `src/environments/environment.ts` and `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: false, // or true for production
  
  // WebSocket URL - Point to your backend agent
  wsUrl: 'ws://localhost:8080/agent',
  
  // REST API URL (if needed)
  apiUrl: 'http://localhost:8080/api',
  
  // Application identifier - Use your app's unique ID
  appId: 'my-custom-app',
  
  // Application title - Shown in the UI header
  appTitle: 'My Custom Agent',
  
  // Empty state hint - Shown when no messages
  emptyStateHint: 'Ask me anything...'
};
```

### Examples for Different Domains

#### E-Commerce Assistant
```typescript
{
  appId: 'ecommerce-assistant',
  appTitle: 'Shopping Assistant',
  emptyStateHint: 'What can I help you find today?'
}
```

#### Technical Support
```typescript
{
  appId: 'tech-support-agent',
  appTitle: 'Technical Support',
  emptyStateHint: 'Describe your issue...'
}
```

#### Travel Booking
```typescript
{
  appId: 'travel-booking-agent',
  appTitle: 'Travel Assistant',
  emptyStateHint: 'Where would you like to go?'
}
```

#### Financial Advisor
```typescript
{
  appId: 'financial-advisor',
  appTitle: 'Financial Assistant',
  emptyStateHint: 'How can I help with your finances?'
}
```

## Step 2: Customize Styling (Optional)

### Update Colors
Edit `src/styles.scss` to match your brand:

```scss
:root {
  --primary-color: #your-color;
  --secondary-color: #your-color;
  --background-color: #your-color;
}
```

### Update Chat Component Styles
Edit `src/app/components/chat/chat.component.scss`:

```scss
.chat-header {
  background: linear-gradient(135deg, #your-primary #your-secondary);
}

.message-bubble {
  border-radius: 12px; // Adjust as needed
}
```

## Step 3: Update Page Title and Icon

### Update HTML Title
Edit `src/index.html`:

```html
<title>Your App Name</title>
```

### Update Favicon
Replace `src/favicon.ico` with your app's icon.

## Step 4: Backend Integration

Ensure your backend:

1. **Implements AG-UI Protocol**
   - Responds to `chat` events
   - Emits `run_started`, `run`, `chat`, `complete` events

2. **Generates A2UI Metadata** (for rich UI)
   ```java
   A2UIMetadata metadata = new A2UIMetadata();
   metadata.addComponent(new CardComponent("Title", "Subtitle", "Content"));
   ```

3. **WebSocket Endpoint** matches your configuration
   ```java
   @MessageMapping("/agent")
   public void handleMessage(AgentMessage message) {
       // Your logic here
   }
   ```

## Step 5: Build and Deploy

### Development
```bash
cd frontend
npm install
ng serve
```

### Production Build
```bash
ng build --configuration production
```

### Docker Build
```bash
docker build -t my-agent-frontend .
docker run -p 80:80 my-agent-frontend
```

## Advanced Customization

### Add Custom A2UI Components

1. **Create component class**
   ```typescript
   export class MyCustomComponent implements A2UIComponent {
     type = 'my-custom';
     // Add properties
   }
   ```

2. **Add to renderer**
   Edit `src/app/services/a2ui-renderer.service.ts`:
   ```typescript
   case 'my-custom':
     return this.renderMyCustom(component);
   ```

3. **Add renderer method**
   ```typescript
   private renderMyCustom(component: A2UIComponent): string {
     return `<div class="a2ui-my-custom">${component.content}</div>`;
   }
   ```

4. **Add styles**
   Add to `src/app/components/chat/chat.component.scss`:
   ```scss
   .a2ui-my-custom {
     // Your styles
   }
   ```

### Add Event Handlers

To handle custom AG-UI events, edit `src/app/services/ag-ui.service.ts`:

```typescript
case 'my-custom-event':
  this.handleMyCustomEvent(data);
  break;

private handleMyCustomEvent(data: any): void {
  // Handle the event
  console.log('Custom event:', data);
}
```

## No Code Changes Needed For...

✅ Different backend implementations  
✅ Different business domains  
✅ Different agent capabilities  
✅ Different A2UI component combinations  

Everything is driven by:
- Environment configuration
- AG-UI protocol messages
- A2UI metadata from backend

## Testing Your Configuration

1. **Verify environment variables are loaded**
   ```typescript
   console.log('Config:', environment);
   ```

2. **Test WebSocket connection**
   - Open browser console
   - Check for connection messages
   - Verify `ws://your-url` is correct

3. **Test message flow**
   - Send a test message
   - Check browser console for AG-UI events
   - Verify A2UI rendering if applicable

## Common Issues

### WebSocket Connection Fails
- Check `wsUrl` in environment configuration
- Verify backend is running and accessible
- Check CORS settings on backend

### A2UI Not Rendering
- Verify backend sends `data.format = 'a2ui'`
- Check `data.components` array exists
- Check browser console for errors

### Styles Not Applied
- Clear browser cache
- Rebuild with `ng build`
- Check CSS class names match

## Support

For issues or questions about the AG-UI protocol and A2UI rendering, refer to:
- [AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md)
- Backend documentation
- AG-UI specification
