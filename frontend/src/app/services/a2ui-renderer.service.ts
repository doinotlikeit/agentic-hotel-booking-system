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
   * This is a generic check that works with any AG-UI message
   */
  hasA2UIMetadata(message: any): boolean {
    return message &&
      message.data &&
      message.data.format === 'a2ui' &&
      Array.isArray(message.data.components);
  }

  /**
   * Extract A2UI metadata from AG-UI message
   * Returns null if no A2UI metadata is found
   */
  extractA2UIMetadata(message: any): A2UIMetadata | null {
    if (this.hasA2UIMetadata(message)) {
      return message.data as A2UIMetadata;
    }
    return null;
  }

  /**
   * Render A2UI metadata to HTML
   * This method converts generic A2UI components to displayable HTML
   */
  renderToHTML(metadata: A2UIMetadata): string {
    if (!metadata || !metadata.components) {
      return '';
    }

    return metadata.components
      .map(component => this.renderComponent(component))
      .join('\n');
  }

  /**
   * Render individual A2UI component to HTML
   * Supports all generic A2UI component types
   */
  private renderComponent(component: A2UIComponent): string {
    switch (component.type) {
      // Text components
      case 'heading':
      case 'header':
        return `<h2 class="a2ui-heading">${this.escapeHtml(component['content'] || '')}</h2>`;

      case 'subheading':
        return `<h3 class="a2ui-subheading">${this.escapeHtml(component['content'] || '')}</h3>`;

      case 'body':
        return `<p class="a2ui-body">${this.escapeHtml(component['content'] || '')}</p>`;

      case 'caption':
      case 'footer':
        return `<p class="a2ui-caption">${this.escapeHtml(component['content'] || '')}</p>`;

      case 'text':
        return this.renderText(component);

      // Container components
      case 'card':
        return this.renderCard(component);

      case 'grid':
        return this.renderGrid(component);

      // List components
      case 'list':
        return this.renderList(component);

      // Interactive components
      case 'button':
        return this.renderButton(component);

      case 'textfield':
      case 'input':
        return this.renderTextField(component);

      // Visual components
      case 'divider':
        return '<hr class="a2ui-divider">';

      case 'status':
        return this.renderStatus(component);

      case 'image':
        return this.renderImage(component);

      // Data components
      case 'json':
        return this.renderJson(component);

      case 'table':
        return this.renderTable(component);

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
    const mode = component['mode'] || 'both'; // 'raw', 'tree', or 'both'
    const data = component['data'];
    const collapsed = component['collapsed'] !== false; // default collapsed
    const jsonId = 'json-' + Math.random().toString(36).substr(2, 9);
    
    let content = '';
    
    if (mode === 'raw' || mode === 'both') {
      const jsonStr = JSON.stringify(data, null, 2);
      content += `
        <div class="a2ui-json-raw">
          ${mode === 'both' ? '<div class="a2ui-json-mode-label">Raw JSON</div>' : ''}
          <pre class="a2ui-json-content"><code>${this.escapeHtml(jsonStr)}</code></pre>
        </div>
      `;
    }
    
    if (mode === 'tree' || mode === 'both') {
      const treeHtml = this.renderJsonTree(data, collapsed, 0);
      content += `
        <div class="a2ui-json-tree">
          ${mode === 'both' ? '<div class="a2ui-json-mode-label">Tree View</div>' : ''}
          ${treeHtml}
        </div>
      `;
    }
    
    return `
      <div class="a2ui-json" id="${jsonId}">
        ${label}
        ${content}
      </div>
    `;
  }
  
  /**
   * Check if an object/array is simple enough to render inline
   */
  private isSimpleValue(data: any): boolean {
    if (data === null || data === undefined) return true;
    const type = typeof data;
    if (type === 'string' || type === 'number' || type === 'boolean') return true;
    
    if (Array.isArray(data)) {
      return data.length === 0 || (data.length <= 3 && data.every(item => this.isSimpleValue(item)));
    }
    
    if (type === 'object') {
      const keys = Object.keys(data);
      return keys.length === 0 || (keys.length <= 3 && keys.every(key => this.isSimpleValue(data[key])));
    }
    
    return false;
  }

  /**
   * Recursively render JSON as an expandable tree
   */
  private renderJsonTree(data: any, collapsed: boolean, level: number): string {
    if (data === null) return '<span class="json-null">null</span>';
    if (data === undefined) return '<span class="json-undefined">undefined</span>';
    
    const type = typeof data;
    
    if (type === 'string') {
      return `<span class="json-string">"${this.escapeHtml(data)}"</span>`;
    }
    if (type === 'number') {
      return `<span class="json-number">${data}</span>`;
    }
    if (type === 'boolean') {
      return `<span class="json-boolean">${data}</span>`;
    }
    
    if (Array.isArray(data)) {
      if (data.length === 0) return '<span class="json-array">[]</span>';
      
      // Render simple arrays inline
      if (this.isSimpleValue(data)) {
        const items = data.map(item => {
          if (typeof item === 'string') return `"${this.escapeHtml(item)}"`;
          return String(item);
        }).join(', ');
        return `<span class="json-array">[${items}]</span>`;
      }
      
      const collapsedClass = collapsed ? 'collapsed' : '';
      // Child nodes should start collapsed by default (level > 0)
      const childCollapsed = level > 0;
      const items = data.map((item, index) => {
        const itemHtml = this.renderJsonTree(item, childCollapsed, level + 1);
        return `
          <div class="json-tree-item">
            <span class="json-key">${index}:</span> ${itemHtml}
          </div>
        `;
      }).join('');
      
      return `
        <div class="json-tree-node ${collapsedClass}">
          <span class="json-toggle">
            <span class="toggle-icon">▼</span>
          </span>
          <span class="json-bracket">[</span>
          <span class="json-count">${data.length} items</span>
          <div class="json-tree-children">
            ${items}
            <div><span class="json-bracket">]</span></div>
          </div>
        </div>
      `;
    }
    
    if (type === 'object') {
      const keys = Object.keys(data);
      if (keys.length === 0) return '<span class="json-object">{}</span>';
      
      // Render simple objects inline
      if (this.isSimpleValue(data)) {
        const pairs = keys.map(key => {
          let value = data[key];
          if (typeof value === 'string') value = `"${this.escapeHtml(value)}"`;
          return `"${this.escapeHtml(key)}": ${value}`;
        }).join(', ');
        return `<span class="json-object">{${pairs}}</span>`;
      }
      
      const collapsedClass = collapsed ? 'collapsed' : '';
      // Child nodes should start collapsed by default (level > 0)
      const childCollapsed = level > 0;
      const items = keys.map(key => {
        const valueHtml = this.renderJsonTree(data[key], childCollapsed, level + 1);
        return `
          <div class="json-tree-item">
            <span class="json-key">"${this.escapeHtml(key)}":</span> ${valueHtml}
          </div>
        `;
      }).join('');
      
      return `
        <div class="json-tree-node ${collapsedClass}">
          <span class="json-toggle">
            <span class="toggle-icon">▼</span>
          </span>
          <span class="json-bracket">{</span>
          <span class="json-count">${keys.length} keys</span>
          <div class="json-tree-children">
            ${items}
            <div><span class="json-bracket">]</span></div>
          </div>
        </div>
      `;
    }
    
    return String(data);
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

  /**
   * Render TextField/Input component
   */
  private renderTextField(component: A2UIComponent): string {
    const placeholder = component['placeholder'] || '';
    const label = component['label'] || '';
    const value = component['value'] || '';
    const disabled = component['disabled'] ? 'disabled' : '';
    
    return `
      <div class="a2ui-textfield">
        ${label ? `<label class="a2ui-textfield-label">${this.escapeHtml(label)}</label>` : ''}
        <input 
          type="text" 
          class="a2ui-textfield-input" 
          placeholder="${this.escapeHtml(placeholder)}"
          value="${this.escapeHtml(value)}"
          ${disabled}
        />
      </div>
    `;
  }

  /**
   * Render Image component
   */
  private renderImage(component: A2UIComponent): string {
    const src = component['src'] || '';
    const alt = component['alt'] || '';
    const caption = component['caption'] || '';
    
    return `
      <div class="a2ui-image">
        <img src="${this.escapeHtml(src)}" alt="${this.escapeHtml(alt)}" class="a2ui-image-img" />
        ${caption ? `<p class="a2ui-image-caption">${this.escapeHtml(caption)}</p>` : ''}
      </div>
    `;
  }

  /**
   * Render Table component with pagination support
   */
  private renderTable(component: A2UIComponent): string {
    const headers = component['headers'] || [];
    const rows = component['rows'] || [];
    const pageSize = component['pageSize'] || 10;
    const showPagination = component['pagination'] !== false && rows.length > pageSize;
    const sortable = component['sortable'] !== false;
    const filterable = component['filterable'] === true;
    const tableId = 'table-' + Math.random().toString(36).substr(2, 9);
    
    const headerHtml = headers.length > 0 
      ? `<thead><tr>${headers.map((h: string, index: number) => 
          `<th class="${sortable ? 'sortable' : ''}" data-column="${index}">
            ${this.escapeHtml(h)}
            ${sortable ? '<span class="sort-icon">⇅</span>' : ''}
          </th>`
        ).join('')}</tr></thead>`
      : '';
    
    // Render first page of rows
    const displayRows = showPagination ? rows.slice(0, pageSize) : rows;
    const rowsHtml = displayRows.map((row: any[], rowIndex: number) => 
      `<tr data-row="${rowIndex}">${row.map(cell => 
        `<td>${this.escapeHtml(String(cell))}</td>`
      ).join('')}</tr>`
    ).join('');
    
    // Pagination controls
    const totalPages = Math.ceil(rows.length / pageSize);
    const paginationHtml = showPagination ? `
      <div class="a2ui-table-pagination">
        <button class="page-btn" data-action="first" ${totalPages <= 1 ? 'disabled' : ''}>⏮️ First</button>
        <button class="page-btn" data-action="prev" ${totalPages <= 1 ? 'disabled' : ''}>◀️ Prev</button>
        <span class="page-info">
          Page <span class="current-page">1</span> of <span class="total-pages">${totalPages}</span>
          (${rows.length} total rows)
        </span>
        <button class="page-btn" data-action="next" ${totalPages <= 1 ? 'disabled' : ''}>Next ▶️</button>
        <button class="page-btn" data-action="last" ${totalPages <= 1 ? 'disabled' : ''}>Last ⏭️</button>
      </div>
    ` : `<div class="a2ui-table-info">${rows.length} rows</div>`;
    
    // Filter input
    const filterHtml = filterable ? `
      <div class="a2ui-table-filter">
        <input type="text" placeholder="Filter table..." class="filter-input" />
        <span class="filter-icon">🔍</span>
      </div>
    ` : '';
    
    return `
      <div class="a2ui-table-container" id="${tableId}" 
           data-page-size="${pageSize}" 
           data-total-rows="${rows.length}"
           data-current-page="1"
           data-all-rows='${JSON.stringify(rows).replace(/'/g, "&apos;")}'>
        ${filterHtml}
        <table class="a2ui-table ${sortable ? 'sortable' : ''}">
          ${headerHtml}
          <tbody>${rowsHtml}</tbody>
        </table>
        ${paginationHtml}
      </div>
      <script>
        (function() {
          const container = document.getElementById('${tableId}');
          if (!container) return;
          
          const pageSize = parseInt(container.getAttribute('data-page-size'));
          let currentPage = 1;
          let allRows = JSON.parse(container.getAttribute('data-all-rows'));
          let filteredRows = allRows;
          
          function renderPage() {
            const start = (currentPage - 1) * pageSize;
            const end = start + pageSize;
            const pageRows = filteredRows.slice(start, end);
            
            const tbody = container.querySelector('tbody');
            tbody.innerHTML = pageRows.map((row, i) => 
              '<tr>' + row.map(cell => '<td>' + String(cell) + '</td>').join('') + '</tr>'
            ).join('');
            
            const totalPages = Math.ceil(filteredRows.length / pageSize);
            container.querySelector('.current-page').textContent = currentPage;
            container.querySelector('.total-pages').textContent = totalPages;
            container.querySelector('.page-info').innerHTML = 
              'Page ' + currentPage + ' of ' + totalPages + ' (' + filteredRows.length + ' rows)';
            
            container.querySelectorAll('.page-btn').forEach(btn => {
              const action = btn.getAttribute('data-action');
              btn.disabled = (action === 'first' || action === 'prev') && currentPage === 1 ||
                            (action === 'next' || action === 'last') && currentPage === totalPages;
            });
          }
          
          // Pagination
          container.querySelectorAll('.page-btn').forEach(btn => {
            btn.addEventListener('click', () => {
              const action = btn.getAttribute('data-action');
              const totalPages = Math.ceil(filteredRows.length / pageSize);
              
              if (action === 'first') currentPage = 1;
              else if (action === 'prev' && currentPage > 1) currentPage--;
              else if (action === 'next' && currentPage < totalPages) currentPage++;
              else if (action === 'last') currentPage = totalPages;
              
              renderPage();
            });
          });
          
          // Filtering
          const filterInput = container.querySelector('.filter-input');
          if (filterInput) {
            filterInput.addEventListener('input', (e) => {
              const query = e.target.value.toLowerCase();
              filteredRows = allRows.filter(row => 
                row.some(cell => String(cell).toLowerCase().includes(query))
              );
              currentPage = 1;
              renderPage();
            });
          }
          
          // Sorting
          container.querySelectorAll('th.sortable').forEach(th => {
            th.addEventListener('click', () => {
              const column = parseInt(th.getAttribute('data-column'));
              const isAsc = th.classList.contains('asc');
              
              container.querySelectorAll('th').forEach(h => h.classList.remove('asc', 'desc'));
              th.classList.add(isAsc ? 'desc' : 'asc');
              
              filteredRows.sort((a, b) => {
                const aVal = a[column];
                const bVal = b[column];
                const modifier = isAsc ? -1 : 1;
                return aVal > bVal ? modifier : aVal < bVal ? -modifier : 0;
              });
              
              currentPage = 1;
              renderPage();
            });
          });
        })();
      </script>
    `;
  }
}
