import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useParams } from "react-router-dom";
import { Download, FileText, Image, Copy, CheckCircle } from "lucide-react";
import { toast } from "react-hot-toast";
import { ResumeApiService, ResumeResponse } from "../services/resumeApi";
import { ExportApiService } from "../services/exportApi";

const ExportPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [activeFormat, setActiveFormat] = useState("pdf");
  const [isExporting, setIsExporting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [latexCode, setLatexCode] = useState("");
  const [resumeData, setResumeData] = useState<ResumeResponse | null>(null);

  // Fetch resume data on component mount
  useEffect(() => {
    const fetchResumeData = async () => {
      if (!id) {
        toast.error("Resume ID not found");
        setIsLoading(false);
        return;
      }

      try {
        const response = await ResumeApiService.getResume(id);
        if (response.success && response.data) {
          setResumeData(response.data);
          setLatexCode(response.data.latexCode || "");
        } else {
          toast.error("Failed to load resume data");
        }
      } catch (error) {
        console.error("Error fetching resume:", error);
        toast.error("Failed to load resume data");
      } finally {
        setIsLoading(false);
      }
    };

    fetchResumeData();
  }, [id]);

  const exportFormats = [
    {
      id: "pdf",
      name: "PDF",
      description: "Professional PDF format for applications",
      icon: <FileText className="h-6 w-6" />,
      size: "~150KB",
    },
    {
      id: "png",
      name: "PNG Image",
      description: "High-quality image format",
      icon: <Image className="h-6 w-6" />,
      size: "~500KB",
    },
    {
      id: "jpg",
      name: "JPG Image",
      description: "Compressed image format",
      icon: <Image className="h-6 w-6" />,
      size: "~200KB",
    },
    {
      id: "latex",
      name: "LaTeX Code",
      description: "Raw LaTeX code for customization",
      icon: <Copy className="h-6 w-6" />,
      size: "~5KB",
    },
  ];

  const handleExport = async (format: string) => {
    if (!id || !resumeData) {
      toast.error("Resume data not available");
      return;
    }

    setIsExporting(true);
    try {
      const exportRequest = {
        resumeId: id,
        customLatexCode: latexCode,
        paperSize: "letter",
        orientation: "portrait",
        dpi: 300,
        backgroundColor: "white",
        highQuality: true,
      };

      switch (format) {
        case "latex":
          const latexResponse = await ExportApiService.exportLatex(
            exportRequest
          );
          if (latexResponse.success && latexResponse.data) {
            setLatexCode(latexResponse.data);
            toast.success("LaTeX code ready!");
          } else {
            throw new Error("Failed to generate LaTeX code");
          }
          break;

        case "pdf":
          const pdfResponse = await ExportApiService.exportPdf(exportRequest);
          if (pdfResponse.success) {
            toast.success("Resume exported as PDF!");
          } else {
            throw new Error(pdfResponse.error || "Failed to generate PDF");
          }
          break;

        case "png":
        case "jpg":
          const imageResponse = await ExportApiService.exportImage(
            exportRequest,
            format as "png" | "jpg"
          );
          if (imageResponse.success) {
            toast.success(`Resume exported as ${format.toUpperCase()}!`);
          } else {
            throw new Error(
              imageResponse.error ||
                `Failed to generate ${format.toUpperCase()} image`
            );
          }
          break;

        default:
          throw new Error(`Unsupported format: ${format}`);
      }
    } catch (error) {
      console.error("Export error:", error);
      toast.error(
        `Export failed: ${
          error instanceof Error ? error.message : "Unknown error"
        }`
      );
    } finally {
      setIsExporting(false);
    }
  };

  const copyLatexCode = () => {
    navigator.clipboard.writeText(latexCode);
    toast.success("LaTeX code copied to clipboard!");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 py-12 flex items-center justify-center">
        <div className="text-center">
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
            className="w-12 h-12 border-4 border-gray-300 border-t-black rounded-full mx-auto mb-4"
          />
          <p className="text-lg text-gray-600">Loading your resume...</p>
        </div>
      </div>
    );
  }

  if (!resumeData) {
    return (
      <div className="min-h-screen bg-gray-50 py-12 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">
            Resume Not Found
          </h1>
          <p className="text-gray-600">
            The requested resume could not be found.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center mb-12">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <h1 className="text-3xl font-bold text-gray-900 sm:text-4xl mb-4">
              Your Resume is Ready!
            </h1>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Choose your preferred format and download your optimized resume
            </p>
          </motion.div>
        </div>

        {/* Resume Info */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="bg-white rounded-xl shadow-elegant p-6 mb-8"
        >
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">
                {resumeData.jobTitle} at {resumeData.companyName}
              </h3>
              <p className="text-gray-600">
                Generated on{" "}
                {new Date(resumeData.createdAt).toLocaleDateString()}
              </p>
            </div>
            <div className="text-right">
              <div className="flex items-center text-green-600">
                <CheckCircle className="h-5 w-5 mr-2" />
                <span className="font-semibold">
                  {resumeData.atsCompatibilityScore
                    ? Math.round(resumeData.atsCompatibilityScore * 100)
                    : 95}
                  % ATS Compatible
                </span>
              </div>
              <p className="text-sm text-gray-500">Excellent match!</p>
            </div>
          </div>
        </motion.div>

        {/* Export Formats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {exportFormats.map((format, index) => (
            <motion.div
              key={format.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 + index * 0.1 }}
              className={`bg-white rounded-xl shadow-elegant p-6 cursor-pointer transition-all duration-200 hover:shadow-elegant-lg ${
                activeFormat === format.id ? "ring-2 ring-black" : ""
              }`}
              onClick={() => setActiveFormat(format.id)}
            >
              <div className="flex flex-col items-center text-center">
                <div className="mb-4 p-3 bg-gray-100 rounded-lg">
                  {format.icon}
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  {format.name}
                </h3>
                <p className="text-sm text-gray-600 mb-2">
                  {format.description}
                </p>
                <p className="text-xs text-gray-500">{format.size}</p>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Export Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.6 }}
          className="text-center"
        >
          <button
            onClick={() => handleExport(activeFormat)}
            disabled={isExporting}
            className="btn-primary text-lg px-8 py-4 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isExporting ? (
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                className="w-5 h-5 border-2 border-white border-t-transparent rounded-full mr-2"
              />
            ) : (
              <Download className="mr-2 h-5 w-5" />
            )}
            {isExporting
              ? "Exporting..."
              : `Export as ${activeFormat.toUpperCase()}`}
          </button>
        </motion.div>

        {/* LaTeX Code Display */}
        {latexCode && activeFormat === "latex" && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="mt-8 bg-white rounded-xl shadow-elegant p-6"
          >
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900">
                LaTeX Code
              </h3>
              <button onClick={copyLatexCode} className="btn-secondary">
                <Copy className="mr-2 h-4 w-4" />
                Copy Code
              </button>
            </div>
            <pre className="bg-gray-100 rounded-lg p-4 overflow-x-auto text-sm font-mono">
              <code>{latexCode}</code>
            </pre>
          </motion.div>
        )}

        {/* Tips */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.8 }}
          className="mt-12 bg-gray-100 rounded-xl p-6"
        >
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            💡 Pro Tips
          </h3>
          <ul className="space-y-2 text-gray-700">
            <li>• Use PDF format for most job applications</li>
            <li>• PNG/JPG formats are great for online portfolios</li>
            <li>• LaTeX code allows for further customization</li>
            <li>• Your resume is optimized for ATS systems</li>
          </ul>
        </motion.div>
      </div>
    </div>
  );
};

export default ExportPage;
