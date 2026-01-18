import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Components
import { AppComponent } from './app.component';
import { ChatComponent } from './components/chat/chat.component';
import { A2uCardComponent } from './components/a2u/a2u-card.component';
import { A2uTextComponent } from './components/a2u/a2u-text.component';
import { A2uGridComponent } from './components/a2u/a2u-grid.component';
import { A2uButtonComponent } from './components/a2u/a2u-button.component';

// Services
import { WebSocketService } from './services/websocket.service';
import { AgUiService } from './services/ag-ui.service';

@NgModule({
  declarations: [
    AppComponent,
    ChatComponent,
    A2uCardComponent,
    A2uTextComponent,
    A2uGridComponent,
    A2uButtonComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  providers: [
    WebSocketService,
    AgUiService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
