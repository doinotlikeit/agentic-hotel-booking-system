import { Injectable } from '@angular/core';

export interface A2UIComponent {
  type: string;
  content?: string;
  [key: string]: any;
}

export interface A2UIMetadata {
  format: string;
  version: string;
  components: A2UIComponent[];
}

@Injectable({
  providedIn: 'root'
})
export class A2uiRendererService {

  constructor() { }

  /**
   * Check if message contains A2UI metadata
   */
  hasA2UIMetadata(message: any): boolean {
    return message &&
      message.data &&
      message.data.format === 'a2ui' &&
      Array.isArray(message.data.components);
  }

  /**
   * Extract A2UI metadata from message
   */
  extractA2UIMetadata(message: any): A2UIMetadata | null {
    if (this.hasA2UIMetadata(message)) {
      return message.data as A2UIMetadata;
    }
    return null;
  }

  /**
   * Render A2UI components to HTML
   */
  renderToHTML(metadata: A2UIMetadata): string {
    if (!metadata || !metadata.components) {
      return '';
    }

    return metadata.components
      .map(component => this.renderComponent(component))
      .join('\n');
  }

  private renderComponent(component: A2UIComponent): string {
    switch (component.type) {
      case 'heading':
        return `<h2 class="a2ui-heading">${this.escapeHtml(component['content'] || '')}</h2>`;

      case 'subheading':
        return `<h3 class="a2ui-subheading">${this.escapeHtml(component['content'] || '')}</h3>`;

      case 'body':
        return `<p class="a2ui-body">${this.escapeHtml(component['content'] || '')}</p>`;

      case 'caption':
        return `<p class="a2ui-caption">${this.escapeHtml(component['content'] || '')}</p>`;

      case 'text':
        return this.renderText(component);

      case 'card':
        return this.renderCard(component);

      case 'list':
        return this.renderList(component);

      case 'divider':
        return '<hr class="a2ui-divider">';

      case 'status':
        return this.renderStatus(component);

      case 'json':
        return this.renderJson(component);

      case 'button':
        return this.renderButton(component);

      case 'grid':
        return this.renderGrid(component);

      default:
        return `<div class="a2ui-unknown">${this.escapeHtml(JSON.stringify(component))}</div>`;
    }
  }

  private renderText(component: A2UIComponent): string {
    const variant = component['variant'] || 'body';
    const align = component['align'] || 'left';
    return `<p class="a2ui-text a2ui-${variant}" style="text-align: ${align}">${this.escapeHtml(component['content'] || '')}</p>`;
  }

  private renderCard(component: A2UIComponent): string {
    const title = component['title'] ? `<h3 class="a2ui-card-title">${this.escapeHtml(component['title'])}</h3>` : '';
    const subtitle = component['subtitle'] ? `<p class="a2ui-card-subtitle">${this.escapeHtml(component['subtitle'])}</p>` : '';
    const content = `<div class="a2ui-card-content">${this.escapeHtml(component['content'] || '')}</div>`;

    return `
      <div class="a2ui-card">
        ${title}
        ${subtitle}
        ${content}
      </div>
    `;
  }

  private renderList(component: A2UIComponent): string {
    const items = (component['items'] || [])
      .map((item: string) => `<li>${this.escapeHtml(item)}</li>`)
      .join('');

    const tag = component['ordered'] ? 'ol' : 'ul';
    return `<${tag} class="a2ui-list">${items}</${tag}>`;
  }

  private renderStatus(component: A2UIComponent): string {
    const status = component['status'] || 'info';
    const icon = this.getStatusIcon(status);
    return `
      <div class="a2ui-status a2ui-status-${status}">
        <span class="material-icons">${icon}</span>
        <span>${this.escapeHtml(component['message'])}</span>
      </div>
    `;
  }

  private renderJson(component: A2UIComponent): string {
    const label = component['label'] ? `<div class="a2ui-json-label">${this.escapeHtml(component['label'])}</div>` : '';
    const jsonStr = JSON.stringify(component['data'], null, 2);
    return `
      <div class="a2ui-json">
        ${label}
        <pre class="a2ui-json-content"><code>${this.escapeHtml(jsonStr)}</code></pre>
      </div>
    `;
  }

  private renderButton(component: A2UIComponent): string {
    const variant = component['variant'] || 'primary';
    return `
      <button class="a2ui-button a2ui-button-${variant}" data-action="${this.escapeHtml(component['action'])}">
        ${this.escapeHtml(component['label'])}
      </button>
    `;
  }

  private renderGrid(component: A2UIComponent): string {
    const columns = component['columns'] || 2;
    const items = (component['items'] || [])
      .map((item: any) => `<div class="a2ui-grid-item">${this.renderComponent(item)}</div>`)
      .join('');

    return `
      <div class="a2ui-grid" style="grid-template-columns: repeat(${columns}, 1fr)">
        ${items}
      </div>
    `;
  }

  private getStatusIcon(status: string): string {
    switch (status) {
      case 'success':
        return 'check_circle';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
      default:
        return 'info';
    }
  }

  private escapeHtml(text: string): string {
    if (typeof text !== 'string') {
      return String(text);
    }
    const map: { [key: string]: string } = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }
}
