import { Injectable } from '@angular/core';
import { WebSocketService } from './websocket.service';
import { BehaviorSubject, Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface AgentMessage {
  sessionId: string;
  appId: string;
  userId: string;
  messageId: string;
  timestamp: string;
  content: string;
  type: 'user' | 'agent';
}

export interface AgentEvent {
  type: 'run_started' | 'run' | 'chat' | 'error' | 'complete';
  data: any;
  sessionId?: string;
  appId?: string;
  userId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AgUiService {
  private readonly APP_ID = 'hotel-booking-app';
  private readonly USER_ID = 'user-' + uuidv4().substring(0, 8);
  private sessionId: string = '';
  
  private messagesSubject = new BehaviorSubject<AgentMessage[]>([]);
  private eventsSubject = new BehaviorSubject<AgentEvent | null>(null);
  
  constructor(private wsService: WebSocketService) {
    this.initializeSession();
    this.subscribeToMessages();
  }

  private initializeSession(): void {
    this.sessionId = 'session-' + uuidv4();
    console.log('Session initialized:', this.sessionId);
  }

  private subscribeToMessages(): void {
    this.wsService.getMessages().subscribe(
      (message) => this.handleIncomingMessage(message),
      (error) => console.error('Error receiving message:', error)
    );
  }

  sendMessage(content: string): void {
    const message: AgentMessage = {
      sessionId: this.sessionId,
      appId: this.APP_ID,
      userId: this.USER_ID,
      messageId: uuidv4(),
      timestamp: new Date().toISOString(),
      content: content,
      type: 'user'
    };

    // Add to local messages
    const currentMessages = this.messagesSubject.value;
    this.messagesSubject.next([...currentMessages, message]);

    // Send via WebSocket
    this.wsService.send({
      type: 'chat',
      message: message
    });
  }

  private handleIncomingMessage(data: any): void {
    console.log('Received data:', data);

    // Handle different event types
    switch (data.type) {
      case 'run_started':
        this.handleRunStarted(data);
        break;
      case 'run':
        this.handleRun(data);
        break;
      case 'chat':
        this.handleChat(data);
        break;
      case 'error':
        this.handleError(data);
        break;
      case 'complete':
        this.handleComplete(data);
        break;
      default:
        console.warn('Unknown message type:', data.type);
    }

    // Emit event
    this.eventsSubject.next(data as AgentEvent);
  }

  private handleRunStarted(data: any): void {
    console.log('Agent run started:', data);
  }

  private handleRun(data: any): void {
    console.log('Agent run event:', data);
  }

  private handleChat(data: any): void {
    if (data.message) {
      const agentMessage: AgentMessage = {
        sessionId: data.message.sessionId || this.sessionId,
        appId: data.message.appId || this.APP_ID,
        userId: data.message.userId || this.USER_ID,
        messageId: data.message.messageId || uuidv4(),
        timestamp: data.message.timestamp || new Date().toISOString(),
        content: data.message.content || '',
        type: 'agent'
      };

      // Preserve A2UI metadata if present
      if (data.data) {
        (agentMessage as any).data = data.data;
      }

      const currentMessages = this.messagesSubject.value;
      this.messagesSubject.next([...currentMessages, agentMessage]);
    }
  }

  private handleError(data: any): void {
    console.error('Agent error:', data);
    const errorMessage: AgentMessage = {
      sessionId: this.sessionId,
      appId: this.APP_ID,
      userId: this.USER_ID,
      messageId: uuidv4(),
      timestamp: new Date().toISOString(),
      content: data.error || 'An error occurred',
      type: 'agent'
    };

    const currentMessages = this.messagesSubject.value;
    this.messagesSubject.next([...currentMessages, errorMessage]);
  }

  private handleComplete(data: any): void {
    console.log('Agent run complete:', data);
  }

  getMessages(): Observable<AgentMessage[]> {
    return this.messagesSubject.asObservable();
  }

  getEvents(): Observable<AgentEvent | null> {
    return this.eventsSubject.asObservable();
  }

  clearMessages(): void {
    this.messagesSubject.next([]);
  }

  resetSession(): void {
    this.initializeSession();
    this.clearMessages();
  }

  getSessionInfo(): { sessionId: string; appId: string; userId: string } {
    return {
      sessionId: this.sessionId,
      appId: this.APP_ID,
      userId: this.USER_ID
    };
  }
}
