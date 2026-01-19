import { Component, Input, OnChanges } from '@angular/core';
import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeNestedDataSource } from '@angular/material/tree';

interface JsonTreeNode {
  key: string;
  value: any;
  type: string;
  children?: JsonTreeNode[];
  isExpandable: boolean;
}

@Component({
  selector: 'app-a2u-json-tree',
  template: `
    <mat-tree [dataSource]="dataSource" [treeControl]="treeControl" class="json-tree">
      <mat-tree-node *matTreeNodeDef="let node" matTreeNodePadding>
        <button mat-icon-button disabled></button>
        <span class="tree-node-content">
          <span class="node-key" *ngIf="node.key">{{ node.key }}:</span>
          <span [ngClass]="'node-value-' + node.type">{{ formatValue(node) }}</span>
        </span>
      </mat-tree-node>

      <mat-nested-tree-node *matTreeNodeDef="let node; when: hasChild">
        <div class="mat-tree-node">
          <button mat-icon-button matTreeNodeToggle
                  [attr.aria-label]="'Toggle ' + node.key"
                  class="toggle-button">
            <span class="toggle-icon">
              {{ treeControl.isExpanded(node) ? '▼' : '▶' }}
            </span>
          </button>
          <span class="tree-node-content">
            <span class="node-key" *ngIf="node.key">{{ node.key }}:</span>
            <span [ngClass]="'node-value-' + node.type">
              {{ node.type === 'array' ? '[' : '{' }}
              <span class="item-count" *ngIf="!treeControl.isExpanded(node)">
                {{ node.children?.length }} {{ node.type === 'array' ? 'items' : 'keys' }}
              </span>
            </span>
          </span>
        </div>
        <div [class.tree-invisible]="!treeControl.isExpanded(node)"
             role="group">
          <ng-container matTreeNodeOutlet></ng-container>
        </div>
        <div class="closing-bracket" *ngIf="treeControl.isExpanded(node)">
          {{ node.type === 'array' ? ']' : '}' }}
        </div>
      </mat-nested-tree-node>
    </mat-tree>
  `,
  styles: [`
    .json-tree {
      font-family: 'Courier New', Consolas, monospace;
      font-size: 0.875rem;
      color: #1f2937;
      background: #ffffff;
      padding: 12px;
    }

    .mat-tree-node {
      min-height: 32px;
      display: flex;
      align-items: center;
    }

    .toggle-button {
      width: 28px;
      height: 28px;
      padding: 0;
      margin-right: 4px;
    }

    .toggle-icon {
      font-size: 0.7rem;
      color: #3b82f6;
      font-weight: bold;
      transition: transform 0.2s ease;
    }

    .tree-node-content {
      display: flex;
      align-items: center;
      line-height: 1.6;
    }

    .node-key {
      color: #0066cc;
      font-weight: 600;
      margin-right: 6px;
    }

    .node-value-string {
      color: #22863a;
    }

    .node-value-number {
      color: #005cc5;
      font-weight: 500;
    }

    .node-value-boolean {
      color: #d73a49;
      font-weight: 600;
    }

    .node-value-null,
    .node-value-undefined {
      color: #6f42c1;
      font-style: italic;
    }

    .node-value-object,
    .node-value-array {
      color: #24292e;
      font-weight: bold;
    }

    .item-count {
      color: #6a737d;
      font-size: 0.75rem;
      margin-left: 6px;
      font-style: italic;
      opacity: 0.8;
      font-weight: normal;
    }

    .closing-bracket {
      color: #24292e;
      font-weight: bold;
      margin-left: 28px;
    }

    .tree-invisible {
      display: none;
    }

    mat-nested-tree-node {
      padding-left: 0 !important;
    }

    mat-nested-tree-node > div[role="group"] {
      padding-left: 28px;
      border-left: 2px solid rgba(59, 130, 246, 0.15);
      margin-left: 14px;
    }
  `]
})
export class A2uJsonTreeComponent implements OnChanges {
  @Input() data: any;
  @Input() collapsed: boolean = false;

  treeControl = new NestedTreeControl<JsonTreeNode>(node => node.children);
  dataSource = new MatTreeNestedDataSource<JsonTreeNode>();

  ngOnChanges(): void {
    if (this.data) {
      const nodes = this.buildTree(this.data, '', 0);
      this.dataSource.data = nodes;
      
      // Expand root level by default unless collapsed is true
      if (!this.collapsed && nodes.length > 0) {
        this.treeControl.dataNodes = nodes;
        this.expandRootLevel(nodes);
      }
    }
  }

  private expandRootLevel(nodes: JsonTreeNode[]): void {
    nodes.forEach(node => {
      if (node.isExpandable) {
        this.treeControl.expand(node);
      }
    });
  }

  private buildTree(data: any, key: string, level: number): JsonTreeNode[] {
    if (data === null) {
      return [{ key, value: null, type: 'null', isExpandable: false }];
    }
    
    if (data === undefined) {
      return [{ key, value: undefined, type: 'undefined', isExpandable: false }];
    }

    const type = typeof data;

    if (type === 'string' || type === 'number' || type === 'boolean') {
      return [{ key, value: data, type, isExpandable: false }];
    }

    if (Array.isArray(data)) {
      if (data.length === 0) {
        return [{ key, value: '[]', type: 'array', isExpandable: false }];
      }

      const children: JsonTreeNode[] = [];
      data.forEach((item, index) => {
        const childNodes = this.buildTree(item, String(index), level + 1);
        children.push(...childNodes);
      });

      return [{ key, value: data, type: 'array', children, isExpandable: true }];
    }

    if (type === 'object') {
      const keys = Object.keys(data);
      if (keys.length === 0) {
        return [{ key, value: '{}', type: 'object', isExpandable: false }];
      }

      const children: JsonTreeNode[] = [];
      keys.forEach(objKey => {
        const childNodes = this.buildTree(data[objKey], objKey, level + 1);
        children.push(...childNodes);
      });

      return [{ key, value: data, type: 'object', children, isExpandable: true }];
    }

    return [{ key, value: String(data), type: 'unknown', isExpandable: false }];
  }

  hasChild = (_: number, node: JsonTreeNode) => node.isExpandable;

  formatValue(node: JsonTreeNode): string {
    if (node.type === 'string') {
      return `"${node.value}"`;
    }
    if (node.type === 'null') {
      return 'null';
    }
    if (node.type === 'undefined') {
      return 'undefined';
    }
    if (node.type === 'object' || node.type === 'array') {
      return '';
    }
    return String(node.value);
  }
}
