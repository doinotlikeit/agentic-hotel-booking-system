import { Injectable } from '@angular/core';
import { marked } from 'marked';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root'
})
export class MarkdownService {

  constructor(private sanitizer: DomSanitizer) {
    this.configureMarked();
  }

  private configureMarked(): void {
    // Configure marked options
    marked.setOptions({
      breaks: true,
      gfm: true
    });

    // Configure renderer for custom styling
    const renderer = new marked.Renderer();
    
    // Override link rendering to open in new tab
    renderer.link = (href, title, text) => {
      const titleAttr = title ? ` title="${title}"` : '';
      return `<a href="${href}"${titleAttr} target="_blank" rel="noopener noreferrer">${text}</a>`;
    };

    // Override code block rendering
    renderer.code = (code, language) => {
      const lang = language || 'text';
      return `<pre><code class="language-${lang}">${this.escapeHtml(code)}</code></pre>`;
    };

    marked.use({ renderer });
  }

  /**
   * Parse markdown text and return sanitized HTML
   */
  parseMarkdown(text: string): SafeHtml {
    if (!text) {
      return '';
    }

    try {
      const html = marked.parse(text) as string;
      return this.sanitizer.sanitize(1, html) || '';
    } catch (error) {
      console.error('Error parsing markdown:', error);
      return text;
    }
  }

  /**
   * Parse markdown and return as trusted HTML (for innerHTML binding)
   */
  parseMarkdownUnsafe(text: string): string {
    if (!text) {
      return '';
    }

    try {
      return marked.parse(text) as string;
    } catch (error) {
      console.error('Error parsing markdown:', error);
      return text;
    }
  }

  /**
   * Check if text contains markdown syntax
   */
  hasMarkdownSyntax(text: string): boolean {
    if (!text) return false;

    const markdownPatterns = [
      /#{1,6}\s/,           // Headers
      /\*\*.*?\*\*/,        // Bold
      /\*.*?\*/,            // Italic
      /__.*?__/,            // Bold (underscore)
      /_.*?_/,              // Italic (underscore)
      /\[.*?\]\(.*?\)/,     // Links
      /!\[.*?\]\(.*?\)/,    // Images
      /```[\s\S]*?```/,     // Code blocks
      /`.*?`/,              // Inline code
      /^\s*[-*+]\s/m,       // Unordered lists
      /^\s*\d+\.\s/m,       // Ordered lists
      /^\s*>/m,             // Blockquotes
      /^\s*---+\s*$/m,      // Horizontal rules
      /\|.*\|/              // Tables
    ];

    return markdownPatterns.some(pattern => pattern.test(text));
  }

  private escapeHtml(text: string): string {
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
