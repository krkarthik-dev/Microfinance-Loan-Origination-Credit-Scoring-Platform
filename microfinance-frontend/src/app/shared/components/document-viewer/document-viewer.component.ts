import { Component, Input, SecurityContext } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="viewer-pane">
      <div class="viewer-header">
        <h3>Document Viewer: {{ title || 'None Selected' }}</h3>
      </div>
      <div class="viewer-body">
        <div class="empty-viewer" *ngIf="!documentUrl">
          <p>{{ emptyMessage }}</p>
        </div>
        
        <object *ngIf="documentUrl" 
                [data]="safeUrl" 
                type="application/pdf" 
                width="100%" 
                height="100%">
          <p>It appears your browser doesn't support embedded PDFs or images in object tags. 
             <a [href]="safeUrl" target="_blank">Click here to open it.</a></p>
        </object>
      </div>
    </div>
  `,
  styles: [`
    .viewer-pane {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      border: 1px solid #e5e7eb;
      display: flex;
      flex-direction: column;
      height: 100%;
      min-height: 600px;
    }

    .viewer-header {
      padding: 1.5rem;
      border-bottom: 1px solid #e5e7eb;
      background: #f9fafb;
      border-radius: 12px 12px 0 0;

      h3 {
        margin: 0;
        font-size: 1.125rem;
        color: #111827;
      }
    }

    .viewer-body {
      flex: 1;
      padding: 1rem;
      background: #e5e7eb;
      border-radius: 0 0 12px 12px;
      display: flex;
      flex-direction: column;
    }

    .empty-viewer {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f3f4f6;
      border: 2px dashed #d1d5db;
      border-radius: 8px;
      color: #6b7280;
    }
    
    object {
      border-radius: 8px;
      flex: 1;
    }
  `]
})
export class DocumentViewerComponent {
  @Input() title: string = '';
  @Input() emptyMessage: string = 'Select a document to view it here.';
  
  _documentUrl: string = '';
  safeUrl: SafeResourceUrl | null = null;

  constructor(private sanitizer: DomSanitizer) {}

  @Input()
  set documentUrl(value: string) {
    this._documentUrl = value;
    if (value) {
      this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(value);
    } else {
      this.safeUrl = null;
    }
  }

  get documentUrl(): string {
    return this._documentUrl;
  }
}
