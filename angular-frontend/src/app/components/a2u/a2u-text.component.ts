import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-a2u-text',
  template: `
    <div [class]="'a2u-text ' + variant" [style.text-align]="align">
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .a2u-text {
      margin: 4px 0;
    }

    .a2u-text.heading {
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.3;
    }

    .a2u-text.subheading {
      font-size: 1.125rem;
      font-weight: 500;
      color: var(--text-primary);
      line-height: 1.4;
    }

    .a2u-text.body {
      font-size: 1rem;
      font-weight: 400;
      color: var(--text-primary);
      line-height: 1.6;
    }

    .a2u-text.caption {
      font-size: 0.875rem;
      font-weight: 400;
      color: var(--text-secondary);
      line-height: 1.5;
    }

    .a2u-text.label {
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  `]
})
export class A2uTextComponent {
  @Input() variant: 'heading' | 'subheading' | 'body' | 'caption' | 'label' = 'body';
  @Input() align: 'left' | 'center' | 'right' = 'left';
}
