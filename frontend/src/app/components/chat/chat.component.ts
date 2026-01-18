import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { WebSocketService, ConnectionStatus } from '../../services/websocket.service';
import { AgUiService, AgentMessage } from '../../services/ag-ui.service';
import { A2uiRendererService } from '../../services/a2ui-renderer.service';
import { MarkdownService } from '../../services/markdown.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy {
  @ViewChild('messageContainer') messageContainer!: ElementRef;
  @ViewChild('messageInput') messageInput!: ElementRef;

  messages: AgentMessage[] = [];
  currentMessage: string = '';
  connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;
  messageHistory: string[] = [];
  historyIndex: number = -1;
  
  private subscriptions: Subscription[] = [];
  private readonly WS_URL = 'ws://localhost:8080/agent';

  constructor(
    private wsService: WebSocketService,
    private agUiService: AgUiService,
    private a2uiRenderer: A2uiRendererService,
    private markdownService: MarkdownService
  ) {}

  ngOnInit(): void {
    // Connect to WebSocket
    this.wsService.connect(this.WS_URL);

    // Subscribe to connection status
    this.subscriptions.push(
      this.wsService.getStatus().subscribe(status => {
        this.connectionStatus = status;
      })
    );

    // Subscribe to messages
    this.subscriptions.push(
      this.agUiService.getMessages().subscribe(messages => {
        this.messages = messages;
        setTimeout(() => this.scrollToBottom(), 100);
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.wsService.disconnect();
  }

  sendMessage(): void {
    if (!this.currentMessage.trim()) {
      return;
    }

    // Add to history
    this.messageHistory.push(this.currentMessage);
    this.historyIndex = this.messageHistory.length;

    // Send message via AG-UI service
    this.agUiService.sendMessage(this.currentMessage);

    // Clear input
    this.currentMessage = '';
  }

  clearMessages(): void {
    this.agUiService.clearMessages();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.navigateHistory(-1);
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.navigateHistory(1);
    }
  }

  private navigateHistory(direction: number): void {
    const newIndex = this.historyIndex + direction;
    
    if (newIndex >= 0 && newIndex < this.messageHistory.length) {
      this.historyIndex = newIndex;
      this.currentMessage = this.messageHistory[this.historyIndex];
    } else if (newIndex === this.messageHistory.length) {
      this.historyIndex = newIndex;
      this.currentMessage = '';
    }
  }

  private scrollToBottom(): void {
    if (this.messageContainer) {
      const element = this.messageContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  getStatusColor(): string {
    switch (this.connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return 'success';
      case ConnectionStatus.CONNECTING:
        return 'warning';
      case ConnectionStatus.ERROR:
        return 'error';
      default:
        return 'secondary';
    }
  }

  getSessionInfo(): string {
    const info = this.agUiService.getSessionInfo();
    return `Session: ${info.sessionId.substring(0, 8)}... | App: ${info.appId} | User: ${info.userId}`;
  }

  formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit'
    });
  }

  /**
   * Check if message has A2UI metadata
   */
  hasA2UIContent(message: AgentMessage): boolean {
    return this.a2uiRenderer.hasA2UIMetadata(message);
  }

  /**
   * Render A2UI content to HTML
   */
  renderA2UIContent(message: AgentMessage): string {
    const metadata = this.a2uiRenderer.extractA2UIMetadata(message);
    if (metadata) {
      return this.a2uiRenderer.renderToHTML(metadata);
    }
    return message.content;
  }

  /**
   * Render message content with markdown support
   */
  renderMessageContent(message: AgentMessage): string {
    if (!message.content) {
      return '';
    }
    return this.markdownService.parseMarkdownUnsafe(message.content);
  }

  /**
   * Check if message content has markdown syntax
   */
  hasMarkdown(message: AgentMessage): boolean {
    return this.markdownService.hasMarkdownSyntax(message.content);
  }
}
