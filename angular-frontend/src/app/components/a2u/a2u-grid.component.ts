import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-a2u-grid',
  template: `
    <div class="a2u-grid" [style.grid-template-columns]="getGridColumns()" [style.gap]="gap">
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .a2u-grid {
      display: grid;
      width: 100%;
    }
  `]
})
export class A2uGridComponent {
  @Input() columns: number = 2;
  @Input() gap: string = '16px';

  getGridColumns(): string {
    return `repeat(${this.columns}, 1fr)`;
  }
}
