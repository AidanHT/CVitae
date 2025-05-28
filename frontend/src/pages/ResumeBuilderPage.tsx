import React, { useState, useCallback } from "react";
import { motion } from "framer-motion";
import { useDropzone } from "react-dropzone";
import { useNavigate } from "react-router-dom";
import {
  Upload,
  FileText,
  Briefcase,
  Settings,
  Sparkles,
  X,
  CheckCircle,
} from "lucide-react";
import { toast } from "react-hot-toast";
import { ResumeApiService, GenerateResumeRequest } from "../services/resumeApi";
import LatexErrorDisplay from "../components/LatexErrorDisplay";

const ResumeBuilderPage: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [masterResume, setMasterResume] = useState("");
  const [jobPosting, setJobPosting] = useState("");
  const [jobTitle, setJobTitle] = useState("");
  const [companyName, setCompanyName] = useState("");
  const [targetLength, setTargetLength] = useState(1);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isUploadingResume, setIsUploadingResume] = useState(false);
  const [isUploadingJob, setIsUploadingJob] = useState(false);
  const [generationError, setGenerationError] = useState<string | null>(null);
  const [debugSession, setDebugSession] = useState<string | null>(null);

  // Separate file upload state
  const [uploadedResumeFile, setUploadedResumeFile] = useState<{
    name: string;
    content: string;
  } | null>(null);
  const [uploadedJobFile, setUploadedJobFile] = useState<{
    name: string;
    content: string;
  } | null>(null);

  const onDropResume = useCallback(async (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (file) {
      try {
        setIsUploadingResume(true);

        // For text files, read directly
        if (file.type === "text/plain") {
          const reader = new FileReader();
          reader.onload = () => {
            const content = reader.result as string;
            setUploadedResumeFile({
              name: file.name,
              content: content,
            });
            toast.success("Resume uploaded successfully!");
            setIsUploadingResume(false);
          };
          reader.readAsText(file);
          return;
        }

        // For binary files (PDF, DOCX), send to backend for processing
        const formData = new FormData();
        formData.append("file", file);

        const response = await fetch("/api/resumes/upload-master", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) {
          throw new Error(`Upload failed: ${response.statusText}`);
        }

        const extractedText = await response.text();
        setUploadedResumeFile({
          name: file.name,
          content: extractedText,
        });
        toast.success("Resume uploaded and processed successfully!");
      } catch (error) {
        console.error("Error uploading resume:", error);
        toast.error("Failed to upload resume. Please try again.");
      } finally {
        setIsUploadingResume(false);
      }
    }
  }, []);

  const onDropJob = useCallback(async (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (file) {
      try {
        setIsUploadingJob(true);

        // For text files, read directly
        if (file.type === "text/plain") {
          const reader = new FileReader();
          reader.onload = () => {
            const content = reader.result as string;
            setUploadedJobFile({
              name: file.name,
              content: content,
            });
            toast.success("Job posting uploaded successfully!");
            setIsUploadingJob(false);
          };
          reader.readAsText(file);
          return;
        }

        // For binary files (PDF, DOCX), send to backend for processing
        const formData = new FormData();
        formData.append("file", file);

        const response = await fetch("/api/resumes/upload-job", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) {
          throw new Error(`Upload failed: ${response.statusText}`);
        }

        const extractedText = await response.text();
        setUploadedJobFile({
          name: file.name,
          content: extractedText,
        });
        toast.success("Job posting uploaded and processed successfully!");
      } catch (error) {
        console.error("Error uploading job posting:", error);
        toast.error("Failed to upload job posting. Please try again.");
      } finally {
        setIsUploadingJob(false);
      }
    }
  }, []);

  // Helper functions
  const removeUploadedResume = () => {
    setUploadedResumeFile(null);
    toast.success("Resume file removed");
  };

  const removeUploadedJob = () => {
    setUploadedJobFile(null);
    toast.success("Job file removed");
  };

  const truncateFileName = (fileName: string, maxLength: number = 30) => {
    if (fileName.length <= maxLength) return fileName;
    const extension = fileName.split(".").pop();
    const name = fileName.substring(0, fileName.lastIndexOf("."));
    const truncatedName = name.substring(0, maxLength - extension!.length - 4);
    return `${truncatedName}...${extension}`;
  };

  // Get effective content (file content takes precedence over text input)
  const getEffectiveResumeContent = () => {
    return uploadedResumeFile?.content || masterResume;
  };

  const getEffectiveJobContent = () => {
    return uploadedJobFile?.content || jobPosting;
  };

  const {
    getRootProps: getResumeRootProps,
    getInputProps: getResumeInputProps,
    isDragActive: isResumeDragActive,
  } = useDropzone({
    onDrop: onDropResume,
    accept: {
      "text/plain": [".txt"],
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        [".docx"],
    },
    multiple: false,
  });

  const {
    getRootProps: getJobRootProps,
    getInputProps: getJobInputProps,
    isDragActive: isJobDragActive,
  } = useDropzone({
    onDrop: onDropJob,
    accept: {
      "text/plain": [".txt"],
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        [".docx"],
    },
    multiple: false,
  });

  const handleGenerateResume = async () => {
    const resumeContent = getEffectiveResumeContent();
    const jobContent = getEffectiveJobContent();

    if (!resumeContent || !jobContent) {
      toast.error("Please provide both master resume and job posting");
      return;
    }

    // Clear previous errors
    setGenerationError(null);
    setDebugSession(null);

    setIsGenerating(true);
    try {
      const request: GenerateResumeRequest = {
        masterResume: resumeContent,
        jobPosting: jobContent,
        jobTitle: jobTitle,
        companyName: companyName,
        targetLength: targetLength,
        includeExperience: true,
        includeEducation: true,
        includeProjects: true,
        includeSkills: true,
        includeLeadership: true,
        userId: "user-123", // In production, get from auth context
        sessionId: `session-${Date.now()}`,
      };

      const response = await ResumeApiService.generateResume(request);

      if (response.success && response.data) {
        toast.success("Resume generated successfully!");
        navigate(`/export/${response.data.id}`); // Navigate to export page with generated resume ID
      } else {
        throw new Error(response.error || "Failed to generate resume");
      }
    } catch (error) {
      console.error("Error generating resume:", error);

      // Extract debug session from error message if available
      const errorMessage =
        error instanceof Error ? error.message : String(error);
      if (errorMessage.includes("Debug Session:")) {
        const match = errorMessage.match(/Debug Session:\s*([^\s\n]+)/);
        if (match) {
          setDebugSession(match[1]);
        }
      }

      // Show detailed error for LaTeX compilation issues
      if (errorMessage.includes("LATEX COMPILATION ERROR")) {
        setGenerationError(errorMessage);
        console.error(
          "🔥 LaTeX Compilation Failed during generation - Showing detailed error display"
        );
      } else {
        toast.error("Failed to generate resume. Please try again.");
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const steps = [
    {
      number: 1,
      title: "Upload Master Resume",
      icon: <FileText className="h-5 w-5" />,
    },
    {
      number: 2,
      title: "Add Job Details",
      icon: <Briefcase className="h-5 w-5" />,
    },
    {
      number: 3,
      title: "Customize Settings",
      icon: <Settings className="h-5 w-5" />,
    },
    {
      number: 4,
      title: "Generate Resume",
      icon: <Sparkles className="h-5 w-5" />,
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-3xl font-bold text-gray-900 sm:text-4xl mb-4">
            Resume Builder
          </h1>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Transform your master resume into a targeted, ATS-optimized resume
            using AI
          </p>
        </div>

        {/* Progress Steps */}
        <div className="mb-12">
          <div className="flex justify-between items-center">
            {steps.map((stepItem, index) => (
              <motion.div
                key={stepItem.number}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className={`flex flex-col items-center ${
                  step >= stepItem.number ? "text-black" : "text-gray-400"
                }`}
              >
                <div
                  className={`w-12 h-12 rounded-full flex items-center justify-center border-2 mb-2 relative ${
                    step >= stepItem.number
                      ? "bg-black text-white border-black"
                      : "bg-white border-gray-300"
                  }`}
                >
                  {stepItem.icon}
                  {/* File upload indicator */}
                  {((stepItem.number === 1 && uploadedResumeFile) ||
                    (stepItem.number === 2 && uploadedJobFile)) && (
                    <div className="absolute -top-1 -right-1 w-4 h-4 bg-green-500 rounded-full flex items-center justify-center">
                      <CheckCircle className="w-3 h-3 text-white" />
                    </div>
                  )}
                </div>
                <span className="text-sm font-medium hidden sm:block">
                  {stepItem.title}
                </span>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Generation Error Display */}
        {generationError && (
          <div className="mb-8">
            <LatexErrorDisplay
              error={generationError}
              title="Resume Generation Error"
              debugSession={debugSession}
              onRetry={() => {
                setGenerationError(null);
                setDebugSession(null);
                handleGenerateResume();
              }}
            />
          </div>
        )}

        {/* Step Content */}
        <div className="bg-white rounded-xl shadow-elegant p-8">
          {step === 1 && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-6"
            >
              <h2 className="text-2xl font-semibold mb-6">
                Upload Your Master Resume
              </h2>

              <div className="space-y-4">
                <div
                  {...getResumeRootProps()}
                  className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                    isResumeDragActive
                      ? "border-black bg-gray-50"
                      : "border-gray-300 hover:border-gray-400"
                  } ${
                    isUploadingResume ? "opacity-50 cursor-not-allowed" : ""
                  }`}
                >
                  <input
                    {...getResumeInputProps()}
                    disabled={isUploadingResume}
                  />
                  {isUploadingResume ? (
                    <div className="animate-spin h-12 w-12 border-4 border-gray-300 border-t-black rounded-full mx-auto mb-4"></div>
                  ) : (
                    <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  )}
                  <p className="text-lg font-medium text-gray-700 mb-2">
                    {isUploadingResume
                      ? "Processing your resume..."
                      : "Drop your resume here, or click to browse"}
                  </p>
                  <p className="text-gray-500">
                    Supports PDF, DOCX, and TXT files
                  </p>
                </div>

                {/* File Upload Indicator */}
                {uploadedResumeFile && (
                  <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg p-4">
                    <div className="flex items-center space-x-3">
                      <CheckCircle className="h-5 w-5 text-green-600" />
                      <div>
                        <p className="text-sm font-medium text-green-800">
                          File uploaded successfully
                        </p>
                        <p
                          className="text-xs text-green-600"
                          title={uploadedResumeFile.name}
                        >
                          {truncateFileName(uploadedResumeFile.name)}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={removeUploadedResume}
                      className="p-1 hover:bg-green-100 rounded-full transition-colors"
                      title="Remove file"
                      aria-label="Remove uploaded resume file"
                    >
                      <X className="h-4 w-4 text-green-600" />
                    </button>
                  </div>
                )}
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-medium text-gray-700">
                    Or paste your resume content:
                  </label>
                  {uploadedResumeFile && (
                    <span className="text-xs text-amber-600 bg-amber-50 px-2 py-1 rounded">
                      File upload takes precedence
                    </span>
                  )}
                </div>
                <textarea
                  value={masterResume}
                  onChange={(e) => setMasterResume(e.target.value)}
                  className={`textarea h-48 ${
                    uploadedResumeFile ? "opacity-50 bg-gray-50" : ""
                  }`}
                  placeholder={
                    uploadedResumeFile
                      ? "File uploaded - this text area is now optional"
                      : "Paste your complete resume content here..."
                  }
                  disabled={uploadedResumeFile !== null}
                />
              </div>

              <div className="flex justify-end">
                <button
                  onClick={() => setStep(2)}
                  disabled={!getEffectiveResumeContent()}
                  className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next: Job Details
                </button>
              </div>
            </motion.div>
          )}

          {step === 2 && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-6"
            >
              <h2 className="text-2xl font-semibold mb-6">Add Job Details</h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Job Title
                  </label>
                  <input
                    type="text"
                    value={jobTitle}
                    onChange={(e) => setJobTitle(e.target.value)}
                    className="input"
                    placeholder="e.g., Senior Software Engineer"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Company Name
                  </label>
                  <input
                    type="text"
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    className="input"
                    placeholder="e.g., Google"
                  />
                </div>
              </div>

              <div className="space-y-4">
                <div
                  {...getJobRootProps()}
                  className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
                    isJobDragActive
                      ? "border-black bg-gray-50"
                      : "border-gray-300 hover:border-gray-400"
                  } ${isUploadingJob ? "opacity-50 cursor-not-allowed" : ""}`}
                >
                  <input {...getJobInputProps()} disabled={isUploadingJob} />
                  {isUploadingJob ? (
                    <div className="animate-spin h-10 w-10 border-4 border-gray-300 border-t-black rounded-full mx-auto mb-3"></div>
                  ) : (
                    <Upload className="h-10 w-10 text-gray-400 mx-auto mb-3" />
                  )}
                  <p className="text-md font-medium text-gray-700 mb-1">
                    {isUploadingJob
                      ? "Processing job posting..."
                      : "Drop job posting here, or click to browse"}
                  </p>
                  <p className="text-sm text-gray-500">
                    Supports PDF, DOCX, and TXT files
                  </p>
                </div>

                {/* File Upload Indicator */}
                {uploadedJobFile && (
                  <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg p-4">
                    <div className="flex items-center space-x-3">
                      <CheckCircle className="h-5 w-5 text-green-600" />
                      <div>
                        <p className="text-sm font-medium text-green-800">
                          File uploaded successfully
                        </p>
                        <p
                          className="text-xs text-green-600"
                          title={uploadedJobFile.name}
                        >
                          {truncateFileName(uploadedJobFile.name)}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={removeUploadedJob}
                      className="p-1 hover:bg-green-100 rounded-full transition-colors"
                      title="Remove file"
                      aria-label="Remove uploaded job posting file"
                    >
                      <X className="h-4 w-4 text-green-600" />
                    </button>
                  </div>
                )}
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-medium text-gray-700">
                    Or paste the job posting:
                  </label>
                  {uploadedJobFile && (
                    <span className="text-xs text-amber-600 bg-amber-50 px-2 py-1 rounded">
                      File upload takes precedence
                    </span>
                  )}
                </div>
                <textarea
                  value={jobPosting}
                  onChange={(e) => setJobPosting(e.target.value)}
                  className={`textarea h-48 ${
                    uploadedJobFile ? "opacity-50 bg-gray-50" : ""
                  }`}
                  placeholder={
                    uploadedJobFile
                      ? "File uploaded - this text area is now optional"
                      : "Paste the complete job description here..."
                  }
                  disabled={uploadedJobFile !== null}
                />
              </div>

              <div className="flex justify-between">
                <button onClick={() => setStep(1)} className="btn-secondary">
                  Back
                </button>
                <button
                  onClick={() => setStep(3)}
                  disabled={!getEffectiveJobContent()}
                  className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next: Settings
                </button>
              </div>
            </motion.div>
          )}

          {step === 3 && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-6"
            >
              <h2 className="text-2xl font-semibold mb-6">
                Customize Settings
              </h2>

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Target Resume Length
                  </label>
                  <div className="flex space-x-4">
                    {[1, 2].map((length) => (
                      <button
                        key={length}
                        onClick={() => setTargetLength(length)}
                        className={`px-4 py-2 rounded-lg border-2 transition-colors ${
                          targetLength === length
                            ? "border-black bg-black text-white"
                            : "border-gray-300 hover:border-gray-400"
                        }`}
                      >
                        {length} Page{length > 1 ? "s" : ""}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Resume Sections (All included by default)
                  </label>
                  <div className="grid grid-cols-2 gap-4">
                    {[
                      "Education",
                      "Experience",
                      "Projects",
                      "Skills",
                      "Leadership",
                    ].map((section) => (
                      <label key={section} className="flex items-center">
                        <input
                          type="checkbox"
                          defaultChecked
                          className="h-4 w-4 text-black border-gray-300 rounded focus:ring-black"
                        />
                        <span className="ml-2 text-sm text-gray-700">
                          {section}
                        </span>
                      </label>
                    ))}
                  </div>
                </div>
              </div>

              <div className="flex justify-between">
                <button onClick={() => setStep(2)} className="btn-secondary">
                  Back
                </button>
                <button onClick={() => setStep(4)} className="btn-primary">
                  Next: Generate
                </button>
              </div>
            </motion.div>
          )}

          {step === 4 && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-6 text-center"
            >
              <h2 className="text-2xl font-semibold mb-6">
                Generate Your Resume
              </h2>

              <div className="bg-gray-50 rounded-lg p-6 space-y-4">
                <h3 className="font-semibold text-lg">Review Your Settings</h3>
                <div className="text-sm text-gray-600 space-y-2">
                  <p>
                    <strong>Job Title:</strong> {jobTitle || "Not specified"}
                  </p>
                  <p>
                    <strong>Company:</strong> {companyName || "Not specified"}
                  </p>
                  <p>
                    <strong>Target Length:</strong> {targetLength} page
                    {targetLength > 1 ? "s" : ""}
                  </p>
                  <p>
                    <strong>Resume Length:</strong> {masterResume.length}{" "}
                    characters
                  </p>
                  <p>
                    <strong>Job Posting Length:</strong> {jobPosting.length}{" "}
                    characters
                  </p>
                </div>
              </div>

              {isGenerating ? (
                <div className="py-12">
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{
                      duration: 2,
                      repeat: Infinity,
                      ease: "linear",
                    }}
                    className="w-16 h-16 border-4 border-gray-200 border-t-black rounded-full mx-auto mb-4"
                  />
                  <p className="text-lg font-medium text-gray-700">
                    AI is analyzing and optimizing your resume...
                  </p>
                  <p className="text-sm text-gray-500 mt-2">
                    This may take a few moments
                  </p>
                </div>
              ) : (
                <div className="flex justify-between">
                  <button onClick={() => setStep(3)} className="btn-secondary">
                    Back
                  </button>
                  <button
                    onClick={handleGenerateResume}
                    className="btn-primary text-lg px-8 py-3"
                  >
                    <Sparkles className="mr-2 h-5 w-5" />
                    Generate Resume
                  </button>
                </div>
              )}
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResumeBuilderPage;
