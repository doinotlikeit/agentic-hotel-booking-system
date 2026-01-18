import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-a2u-card',
  template: `
    <mat-card class="a2u-card" [class.elevated]="elevated">
      <mat-card-header *ngIf="title">
        <mat-card-title>{{ title }}</mat-card-title>
        <mat-card-subtitle *ngIf="subtitle">{{ subtitle }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <ng-content></ng-content>
      </mat-card-content>
      <mat-card-actions *ngIf="hasActions">
        <ng-content select="[actions]"></ng-content>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .a2u-card {
      margin: 8px 0;
      transition: all 0.3s ease;
    }
    
    .a2u-card.elevated {
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    }
    
    .a2u-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
    }

    mat-card-content {
      padding: 16px;
    }
  `]
})
export class A2uCardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() elevated: boolean = false;
  @Input() hasActions: boolean = false;
}
