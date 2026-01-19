import { Component } from '@angular/core';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  template: '<app-chat></app-chat>',
  styles: []
})
export class AppComponent {
  title = environment.appTitle || 'AI Agent';
}
