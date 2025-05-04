/**
 * Resume-specific API functions
 */

import { apiClient, ApiResponse } from './api';

export interface JobAnalysisRequest {
  jobPosting: string;
  jobTitle: string;
  companyName: string;
}

export interface JobAnalysisResponse {
  id: string;
  jobTitle: string;
  companyName: string;
  requiredSkills: string[];
  preferredSkills: string[];
  keyResponsibilities: string[];
  experienceLevel: string;
  salaryRange?: string;
  location?: string;
  analysisNotes: string;
  atsKeywords: string[];
  optimizationTips: string[];
}

export interface GenerateResumeRequest {
  masterResume: string;
  jobPosting: string;
  jobTitle: string;
  companyName: string;
  targetLength: number;
  includeExperience: boolean;
  includeEducation: boolean;
  includeProjects: boolean;
  includeSkills: boolean;
  includeLeadership: boolean;
  userId: string;
  sessionId: string;
}

export interface ResumeResponse {
  id: string;
  tailoredResume: string;
  latexCode: string;
  jobTitle: string;
  companyName: string;
  targetLength: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  jobAnalysis?: JobAnalysisResponse;
  atsCompatibilityScore?: number;
  availableExports?: string[];
}

export class ResumeApiService {
  
  /**
   * Analyze a job posting to extract requirements
   */
  static async analyzeJob(request: JobAnalysisRequest): Promise<ApiResponse<JobAnalysisResponse>> {
    return apiClient.withRetry(() => 
      apiClient.post<JobAnalysisResponse>('/resumes/analyze-job', request)
    );
  }

  /**
   * Generate a tailored resume from master resume and job posting
   */
  static async generateResume(request: GenerateResumeRequest): Promise<ApiResponse<ResumeResponse>> {
    return apiClient.withRetry(() => 
      apiClient.post<ResumeResponse>('/resumes/generate', request),
      2, // Only 2 retries for expensive operations
      2000 // 2 second delay
    );
  }

  /**
   * Upload a master resume file
   */
  static async uploadMasterResume(file: File): Promise<ApiResponse<string>> {
    return apiClient.uploadFile<string>('/resumes/upload-master', file);
  }

  /**
   * Upload a job posting file
   */
  static async uploadJobPosting(file: File): Promise<ApiResponse<string>> {
    return apiClient.uploadFile<string>('/resumes/upload-job', file);
  }

  /**
   * Get a specific resume by ID
   */
  static async getResume(id: string): Promise<ApiResponse<ResumeResponse>> {
    return apiClient.get<ResumeResponse>(`/resumes/${id}`);
  }

  /**
   * Update an existing resume
   */
  static async updateResume(id: string, request: GenerateResumeRequest): Promise<ApiResponse<ResumeResponse>> {
    return apiClient.put<ResumeResponse>(`/resumes/${id}`, request);
  }

  /**
   * Get all resumes for a user
   */
  static async getUserResumes(userId: string): Promise<ApiResponse<ResumeResponse[]>> {
    return apiClient.get<ResumeResponse[]>(`/resumes/user/${userId}`);
  }

  /**
   * Delete a resume
   */
  static async deleteResume(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/resumes/${id}`);
  }
}
