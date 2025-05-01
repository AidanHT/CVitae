import React, { useState, useCallback } from "react";
import { motion } from "framer-motion";
import { useDropzone } from "react-dropzone";
import { Upload, FileText, Briefcase, Settings, Sparkles } from "lucide-react";
import { toast } from "react-hot-toast";
import { useNavigate } from "react-router-dom";

const ResumeBuilderPage: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [masterResume, setMasterResume] = useState("");
  const [jobPosting, setJobPosting] = useState("");
  const [jobTitle, setJobTitle] = useState("");
  const [companyName, setCompanyName] = useState("");
  const [targetLength, setTargetLength] = useState(1);
  const [isGenerating, setIsGenerating] = useState(false);

  const onDropResume = useCallback((acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        setMasterResume(reader.result as string);
        toast.success("Resume uploaded successfully!");
      };
      reader.readAsText(file);
    }
  }, []);

  const onDropJob = useCallback((acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        setJobPosting(reader.result as string);
        toast.success("Job posting uploaded successfully!");
      };
      reader.readAsText(file);
    }
  }, []);

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
    },
    multiple: false,
  });

  const handleGenerateResume = async () => {
    if (!masterResume || !jobPosting) {
      toast.error("Please provide both master resume and job posting");
      return;
    }

    setIsGenerating(true);
    try {
      // Simulate API call
      await new Promise((resolve) => setTimeout(resolve, 3000));
      toast.success("Resume generated successfully!");
      navigate("/export/123"); // Navigate to export page with generated resume ID
    } catch (error) {
      toast.error("Failed to generate resume");
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
                  className={`w-12 h-12 rounded-full flex items-center justify-center border-2 mb-2 ${
                    step >= stepItem.number
                      ? "bg-black text-white border-black"
                      : "bg-white border-gray-300"
                  }`}
                >
                  {stepItem.icon}
                </div>
                <span className="text-sm font-medium hidden sm:block">
                  {stepItem.title}
                </span>
              </motion.div>
            ))}
          </div>
        </div>

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

              <div
                {...getResumeRootProps()}
                className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                  isResumeDragActive
                    ? "border-black bg-gray-50"
                    : "border-gray-300 hover:border-gray-400"
                }`}
              >
                <input {...getResumeInputProps()} />
                <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-lg font-medium text-gray-700 mb-2">
                  Drop your resume here, or click to browse
                </p>
                <p className="text-gray-500">
                  Supports PDF, DOCX, and TXT files
                </p>
              </div>

              <div className="space-y-4">
                <label className="block text-sm font-medium text-gray-700">
                  Or paste your resume content:
                </label>
                <textarea
                  value={masterResume}
                  onChange={(e) => setMasterResume(e.target.value)}
                  className="textarea h-48"
                  placeholder="Paste your complete resume content here..."
                />
              </div>

              <div className="flex justify-end">
                <button
                  onClick={() => setStep(2)}
                  disabled={!masterResume}
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

              <div
                {...getJobRootProps()}
                className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
                  isJobDragActive
                    ? "border-black bg-gray-50"
                    : "border-gray-300 hover:border-gray-400"
                }`}
              >
                <input {...getJobInputProps()} />
                <Upload className="h-10 w-10 text-gray-400 mx-auto mb-3" />
                <p className="text-md font-medium text-gray-700 mb-1">
                  Drop job posting here, or click to browse
                </p>
                <p className="text-sm text-gray-500">TXT files only</p>
              </div>

              <div className="space-y-4">
                <label className="block text-sm font-medium text-gray-700">
                  Or paste the job posting:
                </label>
                <textarea
                  value={jobPosting}
                  onChange={(e) => setJobPosting(e.target.value)}
                  className="textarea h-48"
                  placeholder="Paste the complete job description here..."
                />
              </div>

              <div className="flex justify-between">
                <button onClick={() => setStep(1)} className="btn-secondary">
                  Back
                </button>
                <button
                  onClick={() => setStep(3)}
                  disabled={!jobPosting}
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
