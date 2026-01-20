# Generic AG-UI Frontend

> A domain-agnostic Angular frontend implementing the AG-UI protocol and A2UI rendering system.

## 🎯 Purpose

This frontend is a **completely generic, reusable implementation** that can be used for any agent-based application. It has:

- ✅ **Zero domain knowledge** - No business logic
- ✅ **AG-UI protocol** - Standard agent communication
- ✅ **A2UI rendering** - Dynamic UI from metadata
- ✅ **Configuration-driven** - Customize without code changes

## 🚀 Quick Start

### For New Developers

Start here: **[QUICKSTART.md](./QUICKSTART.md)**

This guide will get you running in 5 minutes.

### For Detailed Configuration

See: **[CONFIGURATION_GUIDE.md](./CONFIGURATION_GUIDE.md)**

Learn how to customize for your specific domain.

### For Protocol Understanding

Read: **[AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md)**

Understand the architecture and protocol implementation.

### For Technical Details

Review: **[REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md)**

See what changed and why.

## 📋 What You Need to Know

### 1. This Frontend is Generic

It knows **nothing** about:
- ❌ Hotels or bookings
- ❌ E-commerce or products
- ❌ Healthcare or patients
- ❌ Any specific business domain

It **only** knows about:
- ✅ AG-UI protocol messages
- ✅ A2UI component rendering
- ✅ WebSocket communication

### 2. Backend Drives Everything

All business logic, domain knowledge, and AI capabilities live in your backend agent. The frontend is just a protocol-compliant UI renderer.

### 3. Configuration Over Code

Change your app's behavior through `environment.ts`:

```typescript
{
  appId: 'your-app',
  appTitle: 'Your App Name',
  wsUrl: 'ws://your-backend-url'
}
```

No code changes needed!

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         Frontend (Generic UI)           │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │   Chat Component (Generic)      │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │   AG-UI Service                 │  │
│  │   - Protocol implementation     │  │
│  │   - Event handling              │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │   A2UI Renderer Service         │  │
│  │   - Component rendering         │  │
│  │   - Metadata parsing            │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │   WebSocket Service             │  │
│  │   - Connection management       │  │
│  └─────────────────────────────────┘  │
└─────────────────────────────────────────┘
                    │
                    │ AG-UI Protocol
                    │ (WebSocket)
                    ▼
┌─────────────────────────────────────────┐
│         Backend (Your Agent)            │
│                                         │
│  - Business logic                       │
│  - Domain knowledge                     │
│  - AI/ML models                         │
│  - A2UI generation                      │
└─────────────────────────────────────────┘
```

## 🎨 Supported A2UI Components

The frontend can render these generic UI components:

| Component | Description | Features |
|-----------|-------------|----------|
| `heading` | Main titles (H2) | - |
| `subheading` | Sub-titles (H3) | - |
| `text` | Paragraphs with variants | - |
| `card` | Content containers | - |
| `button` | Interactive buttons | - |
| `grid` | Multi-column layouts | - |
| `list` | Ordered/unordered lists | - |
| `table` | Tabular data | **Pagination, Sorting, Filtering** |
| `textfield` | Input fields | - |
| `image` | Images with captions | - |
| `image-gallery` | Grid of multiple images | **Lazy loading, Grid layout** |
| `status` | Status indicators | - |
| `divider` | Horizontal separators | - |
| `json` | JSON data display | **Tree view with expand/collapse** |

### Advanced Components

📊 **Table Component** - Professional data tables with:
- Pagination (First/Prev/Next/Last)
- Column sorting (click headers)
- Global search/filter
- Configurable page size
- Row counts and navigation

🌲 **JSON Tree Component** - Developer-friendly JSON display with:
- Expandable/collapsible tree structure
- Raw JSON view
- Both modes simultaneously
- Syntax highlighting
- Node counts (keys/items)

> 📚 See [ADVANCED_COMPONENTS.md](./ADVANCED_COMPONENTS.md) for detailed usage  
> 👀 See [COMPONENTS_VISUAL_GUIDE.md](./COMPONENTS_VISUAL_GUIDE.md) for visual examples

## 📦 Installation

```bash
# Clone the repository
git clone <repo-url>

# Install dependencies
cd frontend
npm install

# Configure your app
# Edit src/environments/environment.ts

# Run development server
ng serve

# Build for production
ng build --configuration production
```

## 🔧 Configuration

Edit `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  
  // Required: WebSocket endpoint
  wsUrl: 'ws://localhost:8080/agent',
  
  // Optional: REST API endpoint
  apiUrl: 'http://localhost:8080/api',
  
  // Required: App identifier
  appId: 'generic-agent-app',
  
  // Required: Display title
  appTitle: 'AI Agent',
  
  // Required: Empty state hint
  emptyStateHint: 'Type your message to begin...'
};
```

## 🌐 Use Cases

This generic frontend works for:

- 💬 Customer service chatbots
- 🛒 E-commerce shopping assistants
- ✈️ Travel booking systems
- 🏥 Healthcare assistants
- 💰 Financial advisors
- 📚 Educational tutors
- 🔧 Technical support
- 📊 Data analysis tools
- 🏢 Administrative interfaces
- 🤖 Any agent-based application

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [QUICKSTART.md](./QUICKSTART.md) | Get started in 5 minutes |
| [CONFIGURATION_GUIDE.md](./CONFIGURATION_GUIDE.md) | Detailed configuration |
| [AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md) | Protocol specification |
| [REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md) | Technical changes |

## 🔍 Key Features

### 1. Domain Agnostic
Zero business logic in frontend. Works with any backend that implements AG-UI protocol.

### 2. Protocol Compliant
Fully implements AG-UI protocol events:
- `run_started`
- `run`
- `chat`
- `error`
- `complete`

### 3. Dynamic UI Rendering
Automatically renders A2UI components from metadata. No hardcoded UI.

### 4. Configuration Driven
All customization via environment files. Zero code changes needed.

### 5. Production Ready
- TypeScript for type safety
- Angular best practices
- Responsive design
- Error handling
- WebSocket reconnection

## 🧪 Testing

```bash
# Unit tests
ng test

# E2E tests
ng e2e

# Lint
ng lint
```

## 🐳 Docker Support

```bash
# Build image
docker build -t agent-frontend .

# Run container
docker run -p 80:80 agent-frontend
```

## 🤝 Contributing

This frontend is designed to be generic and reusable. When contributing:

1. ❌ Do NOT add domain-specific logic
2. ✅ Keep components generic
3. ✅ Follow AG-UI protocol
4. ✅ Add A2UI components that are reusable
5. ✅ Update documentation

## 📄 License

[Your License Here]

## 🆘 Support

Need help?

1. Check [QUICKSTART.md](./QUICKSTART.md)
2. Review [CONFIGURATION_GUIDE.md](./CONFIGURATION_GUIDE.md)
3. Read [AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md)
4. Open an issue

## ✨ Benefits

| Benefit | Description |
|---------|-------------|
| 🔄 **Reusable** | Use with any AG-UI backend |
| 🛠️ **Maintainable** | Clear separation of concerns |
| 📈 **Scalable** | Easy to extend with new components |
| ⚡ **Fast** | Configuration-based customization |
| 🎯 **Focused** | UI only, no business logic |
| 📖 **Documented** | Comprehensive guides |

---

**Ready to build?** Start with [QUICKSTART.md](./QUICKSTART.md)

**Need details?** See [CONFIGURATION_GUIDE.md](./CONFIGURATION_GUIDE.md)

**Want to understand?** Read [AG_UI_PROTOCOL.md](./AG_UI_PROTOCOL.md)
