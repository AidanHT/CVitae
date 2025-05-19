/**
 * Export/download API functions
 */

import { apiClient, ApiResponse } from './api';

export interface ExportRequest {
  resumeId: string;
  customLatexCode?: string;
  paperSize?: string;
  orientation?: string;
  dpi?: number;
  backgroundColor?: string;
  highQuality?: boolean;
}

export class ExportApiService {
  
  /**
   * Export resume as LaTeX code
   */
  static async exportLatex(request: ExportRequest): Promise<ApiResponse<string>> {
    return apiClient.post<string>('/export/latex', request);
  }

  /**
   * Export resume as PDF
   */
  static async exportPdf(request: ExportRequest): Promise<ApiResponse<void>> {
    try {
      const response = await apiClient.post<Response>('/export/pdf', request);
      
      if (response.success && response.data) {
        // Get the Response object and create blob
        const responseObj = response.data as Response;
        const blob = await responseObj.blob();
        
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'resume.pdf';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        return {
          success: true,
          status: response.status,
          traceId: response.traceId
        };
      } else {
        return {
          success: false,
          status: response.status,
          error: response.error || 'Failed to generate PDF',
          traceId: response.traceId
        };
      }
    } catch (error) {
      return {
        success: false,
        status: 0,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }

  /**
   * Export resume as image (PNG/JPG)
   */
  static async exportImage(request: ExportRequest, format: 'png' | 'jpg' = 'png'): Promise<ApiResponse<void>> {
    try {
      const response = await apiClient.post<Response>(`/export/image?format=${format}`, request);
      
      if (response.success && response.data) {
        // Get the Response object and create blob
        const responseObj = response.data as Response;
        const blob = await responseObj.blob();
        
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `resume.${format}`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        return {
          success: true,
          status: response.status,
          traceId: response.traceId
        };
      } else {
        return {
          success: false,
          status: response.status,
          error: response.error || `Failed to generate ${format.toUpperCase()} image`,
          traceId: response.traceId
        };
      }
    } catch (error) {
      return {
        success: false,
        status: 0,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }
}
