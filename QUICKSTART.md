# Quick Start Guide

## Prerequisites

Before starting, ensure you have:
- Java 17+
- Maven 3.6+
- Node.js 18+ and npm
- Google Cloud Project with VertexAI API enabled
- GCP Authentication configured

## GCP Setup (Required for ADK)

### 1. Enable VertexAI API
```bash
gcloud services enable aiplatform.googleapis.com
```

### 2. Set up Authentication
Choose one of these methods:

**Option A: Application Default Credentials (Recommended for local dev)**
```bash
gcloud auth application-default login
```

**Option B: Service Account**
```bash
# Create service account
gcloud iam service-accounts create hotel-booking-agent

# Grant VertexAI User role
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:hotel-booking-agent@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"

# Create and download key
gcloud iam service-accounts keys create credentials.json \
  --iam-account=hotel-booking-agent@YOUR_PROJECT_ID.iam.gserviceaccount.com

# Set environment variable
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### 3. Configure Application Properties

Edit `adk-root-agent/src/main/resources/application.properties`:

```properties
# Google Cloud Configuration
gcp.project.id=your-project-id
gcp.location=us-central1

# Vertex AI Model
gcp.vertex.model=gemini-2.0-flash-exp

# Agent Configuration
agent.name=hotel-booking-agent
agent.description=AI-powered hotel booking assistant
```

Or set environment variables:
```bash
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1
```

## Option 1: Local Development

### Start ADK Root Agent
```bash
cd adk-root-agent

# Set environment variables (REQUIRED)
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1

# Run
mvn spring-boot:run
```

### Start Angular Frontend (in new terminal)
```bash
cd angular-frontend
npm install
npm start
```

### Access Application
Open browser to: http://localhost:4200

## Option 2: Docker Compose

### Prerequisites
- Docker
- Docker Compose
- GCP credentials file

### Start Everything
```bash
# Set environment variables
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# Start services
docker-compose up --build
```

### Access Application
Open browser to: http://localhost:4200

## Testing the Application

### Example Interactions

1. **Search for Hotels**
   - Type: "Search for hotels in Paris"
   - Agent will present a plan and execute search
   - Results will be displayed as JSON

2. **Book a Hotel**
   - Type: "Book a hotel"
   - Agent will process the booking
   - You'll receive a booking confirmation

3. **Search and Book**
   - Type: "Find and book hotels in Tokyo"
   - Agent will execute both operations sequentially
   - You'll get a comprehensive summary

### Monitor Connection
- Check the connection status in the top-right corner
- Should show "Connected" with a green indicator
- If disconnected, the app will automatically retry

### View Session Info
- Session information is displayed below the header
- Shows: Session ID, App ID, and User ID
- These are echoed back by the agent in responses

### Message History
- Use ↑ (up arrow) to navigate to previous messages
- Use ↓ (down arrow) to navigate forward
- Press Enter to send messages
- Click "Clear" to clear message history

## Troubleshooting

### Agent Backend Not Starting
```bash
# Check if port 8080 is already in use
lsof -i :8080

# Kill the process if needed
kill -9 <PID>

# Or change the port in application.properties
server.port=8081
```

### Frontend Not Starting
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### WebSocket Connection Issues
1. Ensure agent backend is running
2. Check browser console for errors
3. Verify CORS settings allow localhost:4200
4. Try refreshing the page

## Configuration

### ADK Root Agent Configuration
Edit: `adk-root-agent/src/main/resources/application.properties`

```properties
# Change port
server.port=8080

# GCP settings (REQUIRED - set via environment variables)
gcp.project-id=${GCP_PROJECT_ID}
gcp.location=${GCP_LOCATION:us-central1}

# ADK/Gemini model configuration
adk.model=gemini-2.0-flash-exp
adk.temperature=0.7
adk.max-tokens=2048
```

**Important**: `GCP_PROJECT_ID` must be set as an environment variable. The ADK SDK uses this to initialize VertexAI.

### Angular Frontend Configuration
Edit: `angular-frontend/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  wsUrl: 'ws://localhost:8080/agent',
  apiUrl: 'http://localhost:8080/api'
};
```

## Health Checks

### Agent Backend Health
```bash
curl http://localhost:8080/actuator/health
```

### Check WebSocket
```bash
# Using wscat (install: npm install -g wscat)
wscat -c ws://localhost:8080/agent
```

## Next Steps

1. Customize the UI colors in `angular-frontend/src/styles.scss`
2. Add more agent tools in the adk-root-agent
3. Integrate with real MCP servers
4. Add authentication
5. Implement database persistence

## Getting Help

- Check the logs in the terminal where you started each service
- Agent backend logs show agent processing and events
- Frontend browser console shows WebSocket messages
- Review the main README.md for detailed documentation
