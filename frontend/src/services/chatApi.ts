/**
 * Chat/AI assistant API functions
 */

import { apiClient, ApiResponse } from './api';

export interface ChatMessageRequest {
  message: string;
  sessionId: string;
  context: string;
  currentResumeContent?: string;
  jobPostingContent?: string;
  userId: string;
}

export interface ChatMessageResponse {
  id: string;
  message: string;
  response: string;
  sessionId: string;
  context: string;
  timestamp: string;
  suggestions?: string[];
}

export interface SuggestionRequest {
  resumeContent: string;
  jobPostingContent?: string;
  context: string;
  userId: string;
}

export interface SuggestionResponse {
  id: string;
  suggestion: string;
  category: string;
  priority: string;
  explanation: string;
}

export class ChatApiService {
  
  /**
   * Send a message to the AI assistant
   */
  static async sendMessage(request: ChatMessageRequest): Promise<ApiResponse<ChatMessageResponse>> {
    return apiClient.withRetry(() => 
      apiClient.post<ChatMessageResponse>('/chat/message', request),
      2 // Limited retries for AI operations
    );
  }

  /**
   * Get AI suggestions for resume improvement
   */
  static async getSuggestions(request: SuggestionRequest): Promise<ApiResponse<SuggestionResponse[]>> {
    return apiClient.withRetry(() => 
      apiClient.post<SuggestionResponse[]>('/chat/suggestions', request),
      2
    );
  }

  /**
   * Get conversation history for a session
   */
  static async getConversation(sessionId: string): Promise<ApiResponse<ChatMessageResponse[]>> {
    return apiClient.get<ChatMessageResponse[]>(`/chat/conversation/${sessionId}`);
  }

  /**
   * Clear conversation history for a session
   */
  static async clearConversation(sessionId: string): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/chat/conversation/${sessionId}`);
  }
}
