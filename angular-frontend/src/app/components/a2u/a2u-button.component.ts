import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-a2u-button',
  template: `
    <button 
      mat-raised-button 
      [color]="color"
      [disabled]="disabled"
      [class]="'a2u-button ' + variant"
      (click)="handleClick()">
      <mat-icon *ngIf="icon">{{ icon }}</mat-icon>
      <span>{{ label }}</span>
    </button>
  `,
  styles: [`
    .a2u-button {
      margin: 4px;
      transition: all 0.2s ease;
    }

    .a2u-button mat-icon {
      margin-right: 8px;
    }

    .a2u-button.primary {
      background-color: var(--accent-color);
      color: white;
    }

    .a2u-button.secondary {
      background-color: var(--secondary-color);
      color: white;
    }

    .a2u-button.danger {
      background-color: var(--error-color);
      color: white;
    }

    .a2u-button:hover:not([disabled]) {
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    }

    .a2u-button[disabled] {
      opacity: 0.5;
      cursor: not-allowed;
    }
  `]
})
export class A2uButtonComponent {
  @Input() label: string = 'Button';
  @Input() icon?: string;
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';
  @Input() variant: 'primary' | 'secondary' | 'danger' = 'primary';
  @Input() disabled: boolean = false;
  @Output() clicked = new EventEmitter<void>();

  handleClick(): void {
    if (!this.disabled) {
      this.clicked.emit();
    }
  }
}
