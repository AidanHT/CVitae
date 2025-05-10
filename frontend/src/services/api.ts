/**
 * Centralized API client for all backend communication
 */

export interface ApiResponse<T = any> {
  data?: T;
  error?: string;
  traceId?: string;
  status: number;
  success: boolean;
}

export interface ApiError {
  traceId: string;
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: Record<string, any>;
}

class ApiClient {
  private baseURL: string;
  private readonly timeout: number = 30000; // 30 seconds

  constructor() {
    // Resolve API base URL based on environment
    this.baseURL = this.resolveApiBaseUrl();
    console.log(`🔗 API Client initialized with base URL: ${this.baseURL}`);
  }

  private resolveApiBaseUrl(): string {
    const currentPort = window.location.port;
    const currentHost = window.location.hostname;
    
    console.log(`🔗 API URL Resolution - Current: ${window.location.origin}, Port: ${currentPort}`);

    // Check for explicit environment variable
    const envApiUrl = import.meta.env.VITE_API_URL;
    console.log(`🔗 Environment VITE_API_URL: ${envApiUrl}`);
    
    // Only use environment variable if it's not the Docker internal URL
    if (envApiUrl && !envApiUrl.includes('localhost:8080')) {
      const apiUrl = envApiUrl.endsWith('/api') ? envApiUrl : `${envApiUrl}/api`;
      console.log(`🔗 Using environment API URL: ${apiUrl}`);
      return apiUrl;
    }

    // Development scenarios
    if (import.meta.env.DEV) {
      // Direct frontend access on port 3000 (Vite dev server)
      if (currentPort === '3000') {
        const apiUrl = 'http://localhost:8080/api';
        console.log(`🔗 Development port 3000 detected, using direct backend: ${apiUrl}`);
        return apiUrl;
      }
      
      // Docker frontend container (also port 3000 but containerized)
      if (currentPort === '3000' && currentHost === 'localhost') {
        const apiUrl = 'http://localhost:8080/api';
        console.log(`🔗 Docker development detected, using host backend: ${apiUrl}`);
        return apiUrl;
      }
    }

    // NGINX proxy scenarios (port 80 or no port)
    if (currentPort === '' || currentPort === '80' || currentPort === '443') {
      const apiUrl = '/api';
      console.log(`🔗 NGINX proxy detected, using relative path: ${apiUrl}`);
      return apiUrl;
    }

    // Production fallback
    const apiUrl = `${window.location.origin}/api`;
    console.log(`🔗 Fallback to current origin: ${apiUrl}`);
    return apiUrl;
  }

  private async makeRequest<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`;
    const startTime = Date.now();

    // Set default headers
    const headers = new Headers(options.headers);
    if (!headers.has('Content-Type') && options.body) {
      headers.set('Content-Type', 'application/json');
    }

    // Create abort controller for timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      console.log(`🚀 API Request: ${options.method || 'GET'} ${url}`);

      const response = await fetch(url, {
        ...options,
        headers,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const duration = Date.now() - startTime;
      const traceId = response.headers.get('X-Trace-ID') || 'unknown';

      console.log(`📡 API Response: ${response.status} in ${duration}ms [Trace: ${traceId}]`);

      // Handle non-JSON responses (like file downloads)
      const contentType = response.headers.get('content-type');
      if (contentType && !contentType.includes('application/json')) {
        if (response.ok) {
          return {
            data: response as unknown as T,
            status: response.status,
            success: true,
            traceId,
          };
        }
      }

      // Parse JSON response
      let responseData: any = null;
      try {
        responseData = await response.json();
      } catch (e) {
        // Handle non-JSON error responses
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
      }

      if (response.ok) {
        return {
          data: responseData,
          status: response.status,
          success: true,
          traceId,
        };
      } else {
        // Handle structured error responses
        const apiError = responseData as ApiError;
        console.error(`❌ API Error [${traceId}]:`, apiError);
        
        return {
          error: apiError.message || `HTTP ${response.status}`,
          status: response.status,
          success: false,
          traceId: apiError.traceId || traceId,
        };
      }
    } catch (error) {
      clearTimeout(timeoutId);

      if (error instanceof Error) {
        if (error.name === 'AbortError') {
          console.error(`⏱️ API Timeout: ${url}`);
          return {
            error: 'Request timeout - please try again',
            status: 0,
            success: false,
          };
        }

        console.error(`💥 API Error: ${url}`, error);
        return {
          error: error.message || 'Network error occurred',
          status: 0,
          success: false,
        };
      }

      return {
        error: 'Unknown error occurred',
        status: 0,
        success: false,
      };
    }
  }

  // HTTP method helpers
  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.makeRequest<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.makeRequest<T>(endpoint, { method: 'DELETE' });
  }

  async uploadFile<T>(
    endpoint: string,
    file: File,
    fieldName: string = 'file'
  ): Promise<ApiResponse<T>> {
    const formData = new FormData();
    formData.append(fieldName, file);

    return this.makeRequest<T>(endpoint, {
      method: 'POST',
      body: formData,
      // Don't set Content-Type for FormData, let browser set it with boundary
    });
  }

  // Retry wrapper for important operations
  async withRetry<T>(
    operation: () => Promise<ApiResponse<T>>,
    maxRetries: number = 3,
    delayMs: number = 1000
  ): Promise<ApiResponse<T>> {
    let lastResponse: ApiResponse<T>;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      lastResponse = await operation();

      if (lastResponse.success || lastResponse.status >= 400) {
        // Success or client error (don't retry client errors)
        return lastResponse;
      }

      if (attempt < maxRetries) {
        console.log(`🔄 Retrying API call (attempt ${attempt + 1}/${maxRetries})...`);
        await new Promise(resolve => setTimeout(resolve, delayMs * attempt));
      }
    }

    return lastResponse!;
  }
}

// Export singleton instance
export const apiClient = new ApiClient();

// Health check function
export const checkApiHealth = async (): Promise<boolean> => {
  try {
    const response = await apiClient.get<any>('/health');
    return response.success && response.data?.healthy === true;
  } catch {
    return false;
  }
};
