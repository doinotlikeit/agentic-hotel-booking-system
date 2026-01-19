import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, interval } from 'rxjs';

export enum ConnectionStatus {
  CONNECTED = 'Connected',
  DISCONNECTED = 'Disconnected',
  CONNECTING = 'Connecting...',
  ERROR = 'Error'
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket: WebSocket | null = null;
  private messageSubject = new Subject<any>();
  private statusSubject = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  private reconnectAttempts = 0;
  private reconnectInterval = 3000;
  private pingInterval: any;
  private pongTimeout: any;

  constructor() { }

  connect(url: string): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.statusSubject.next(ConnectionStatus.CONNECTING);

    try {
      this.socket = new WebSocket(url);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.statusSubject.next(ConnectionStatus.CONNECTED);
        this.reconnectAttempts = 0;
        this.startPingPong();
      };

      this.socket.onmessage = (event) => {
        try {
          console.log('🔵 WebSocket RAW message received:', event.data);
          const data = JSON.parse(event.data);
          console.log('🔵 Parsed message type:', data.type, 'messageId:', data.message?.messageId);

          // Handle pong messages
          if (data.type === 'pong') {
            this.clearPongTimeout();
            return;
          }

          this.messageSubject.next(data);
        } catch (error) {
          console.error('Error parsing message:', error);
        }
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.statusSubject.next(ConnectionStatus.ERROR);
      };

      this.socket.onclose = () => {
        console.log('WebSocket closed');
        this.statusSubject.next(ConnectionStatus.DISCONNECTED);
        this.stopPingPong();
        this.attemptReconnect(url);
      };
    } catch (error) {
      console.error('Error creating WebSocket:', error);
      this.statusSubject.next(ConnectionStatus.ERROR);
    }
  }

  send(message: any): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.error('WebSocket is not connected');
    }
  }

  disconnect(): void {
    this.stopPingPong();
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  getMessages(): Observable<any> {
    return this.messageSubject.asObservable();
  }

  getStatus(): Observable<ConnectionStatus> {
    return this.statusSubject.asObservable();
  }

  private startPingPong(): void {
    this.pingInterval = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping' });

        // Set timeout for pong response
        this.pongTimeout = setTimeout(() => {
          console.warn('Pong not received, connection may be dead');
          this.disconnect();
        }, 5000);
      }
    }, 30000); // Send ping every 30 seconds
  }

  private stopPingPong(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
      this.pingInterval = null;
    }
    this.clearPongTimeout();
  }

  private clearPongTimeout(): void {
    if (this.pongTimeout) {
      clearTimeout(this.pongTimeout);
      this.pongTimeout = null;
    }
  }

  private attemptReconnect(url: string): void {
    this.reconnectAttempts++;
    console.log(`Attempting to reconnect (attempt ${this.reconnectAttempts})...`);

    setTimeout(() => {
      this.connect(url);
    }, this.reconnectInterval);
  }
}
