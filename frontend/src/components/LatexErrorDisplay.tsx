import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Copy,
  ExternalLink,
  FileText,
  Terminal,
  Bug,
} from "lucide-react";
import { toast } from "react-hot-toast";

interface LatexErrorDisplayProps {
  error: string;
  title?: string;
  debugSession?: string;
  onRetry?: () => void;
  className?: string;
}

export const LatexErrorDisplay: React.FC<LatexErrorDisplayProps> = ({
  error,
  title = "LaTeX Compilation Error",
  debugSession,
  onRetry,
  className = "",
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [debugInfo, setDebugInfo] = useState<any>(null);
  const [loadingDebug, setLoadingDebug] = useState(false);

  // Parse the enhanced error message
  const parseError = (errorText: string) => {
    const sections = {
      summary: "",
      analysis: "",
      steps: "",
      rawError: "",
    };

    if (errorText.includes("🔥 LATEX COMPILATION ERROR")) {
      const parts = errorText.split("─────────────────────");
      sections.summary = parts[0] || errorText.substring(0, 200);

      if (errorText.includes("📋 DETAILED ERROR ANALYSIS:")) {
        const analysisStart = errorText.indexOf("📋 DETAILED ERROR ANALYSIS:");
        const stepsStart = errorText.indexOf("🔧 DEBUGGING STEPS:");
        if (analysisStart !== -1 && stepsStart !== -1) {
          sections.analysis = errorText
            .substring(analysisStart, stepsStart)
            .trim();
        }
      }

      if (errorText.includes("🔧 DEBUGGING STEPS:")) {
        const stepsStart = errorText.indexOf("🔧 DEBUGGING STEPS:");
        const rawStart = errorText.indexOf("📄 RAW ERROR RESPONSE:");
        if (stepsStart !== -1 && rawStart !== -1) {
          sections.steps = errorText.substring(stepsStart, rawStart).trim();
        }
      }

      if (errorText.includes("📄 RAW ERROR RESPONSE:")) {
        const rawStart = errorText.indexOf("📄 RAW ERROR RESPONSE:");
        sections.rawError = errorText.substring(rawStart).trim();
      }
    } else {
      sections.summary = errorText;
    }

    return sections;
  };

  const errorSections = parseError(error);

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success("Copied to clipboard!");
    } catch (err) {
      toast.error("Failed to copy to clipboard");
    }
  };

  const loadDebugInfo = async () => {
    if (!debugSession) return;

    setLoadingDebug(true);
    try {
      const response = await fetch(`/api/export/debug/${debugSession}`);
      if (response.ok) {
        const data = await response.json();
        setDebugInfo(data);
        console.log("🔍 Debug Info:", data);
      } else {
        toast.error("Failed to load debug information");
      }
    } catch (err) {
      console.error("Failed to load debug info:", err);
      toast.error("Failed to load debug information");
    } finally {
      setLoadingDebug(false);
    }
  };

  // Log error to console for immediate visibility
  React.useEffect(() => {
    console.error("🔥 LaTeX Compilation Error:", error);
    if (debugSession) {
      console.log("🔍 Debug Session:", debugSession);
    }
  }, [error, debugSession]);

  return (
    <div className={`bg-red-50 border border-red-200 rounded-lg ${className}`}>
      {/* Error Header */}
      <div className="p-4 border-b border-red-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <AlertTriangle className="h-6 w-6 text-red-600" />
            <div>
              <h3 className="text-lg font-semibold text-red-800">{title}</h3>
              <p className="text-sm text-red-600">
                LaTeX compilation failed - detailed error information below
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            {debugSession && (
              <button
                onClick={loadDebugInfo}
                disabled={loadingDebug}
                className="px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors flex items-center space-x-1"
              >
                <Bug className="h-3 w-3" />
                <span>{loadingDebug ? "Loading..." : "Debug Info"}</span>
              </button>
            )}
            <button
              onClick={() => setIsExpanded(!isExpanded)}
              className="p-1 hover:bg-red-100 rounded transition-colors"
            >
              {isExpanded ? (
                <ChevronUp className="h-5 w-5 text-red-600" />
              ) : (
                <ChevronDown className="h-5 w-5 text-red-600" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Error Summary */}
      <div className="p-4">
        <div className="bg-white rounded border border-red-200 p-3">
          <pre className="text-sm text-red-700 whitespace-pre-wrap font-mono">
            {errorSections.summary}
          </pre>
        </div>
      </div>

      {/* Expanded Error Details */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.3 }}
            className="border-t border-red-200"
          >
            <div className="p-4 space-y-4">
              {/* Error Analysis */}
              {errorSections.analysis && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-sm font-semibold text-red-800 flex items-center">
                      <Terminal className="h-4 w-4 mr-1" />
                      Error Analysis
                    </h4>
                    <button
                      onClick={() => copyToClipboard(errorSections.analysis)}
                      className="p-1 hover:bg-red-100 rounded"
                    >
                      <Copy className="h-4 w-4 text-red-600" />
                    </button>
                  </div>
                  <div className="bg-white rounded border border-red-200 p-3">
                    <pre className="text-xs text-gray-700 whitespace-pre-wrap font-mono">
                      {errorSections.analysis}
                    </pre>
                  </div>
                </div>
              )}

              {/* Debugging Steps */}
              {errorSections.steps && (
                <div>
                  <h4 className="text-sm font-semibold text-red-800 mb-2 flex items-center">
                    <FileText className="h-4 w-4 mr-1" />
                    Debugging Steps
                  </h4>
                  <div className="bg-white rounded border border-red-200 p-3">
                    <pre className="text-xs text-gray-700 whitespace-pre-wrap font-mono">
                      {errorSections.steps}
                    </pre>
                  </div>
                </div>
              )}

              {/* Raw Error */}
              {errorSections.rawError && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-sm font-semibold text-red-800 flex items-center">
                      <ExternalLink className="h-4 w-4 mr-1" />
                      Raw Error Response
                    </h4>
                    <button
                      onClick={() => copyToClipboard(errorSections.rawError)}
                      className="p-1 hover:bg-red-100 rounded"
                    >
                      <Copy className="h-4 w-4 text-red-600" />
                    </button>
                  </div>
                  <div className="bg-gray-50 rounded border border-gray-200 p-3 max-h-40 overflow-y-auto">
                    <pre className="text-xs text-gray-600 whitespace-pre-wrap font-mono">
                      {errorSections.rawError}
                    </pre>
                  </div>
                </div>
              )}

              {/* Debug Info */}
              {debugInfo && (
                <div>
                  <h4 className="text-sm font-semibold text-red-800 mb-2">
                    Debug Session Files
                  </h4>
                  <div className="bg-white rounded border border-red-200 p-3">
                    <div className="text-xs text-gray-600 mb-2">
                      Session: {debugInfo.sessionId} | Files:{" "}
                      {debugInfo.filesFound}
                    </div>
                    {debugInfo.files &&
                      debugInfo.files.map((file: any, index: number) => (
                        <div
                          key={index}
                          className="border-t border-gray-100 pt-2 mt-2"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-mono text-xs text-gray-700">
                              {file.name} ({file.size} bytes)
                            </span>
                            {file.content && (
                              <button
                                onClick={() => copyToClipboard(file.content)}
                                className="p-1 hover:bg-gray-100 rounded"
                              >
                                <Copy className="h-3 w-3 text-gray-500" />
                              </button>
                            )}
                          </div>
                          {file.content && (
                            <pre className="text-xs text-gray-600 mt-1 max-h-32 overflow-y-auto bg-gray-50 p-2 rounded">
                              {file.content.substring(0, 500)}
                              {file.content.length > 500 && "..."}
                            </pre>
                          )}
                        </div>
                      ))}
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="flex justify-between items-center pt-4 border-t border-red-200">
                <div className="text-xs text-red-600">
                  💡 Check the browser console for additional debug information
                </div>
                {onRetry && (
                  <button
                    onClick={onRetry}
                    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors text-sm"
                  >
                    Try Again
                  </button>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default LatexErrorDisplay;
