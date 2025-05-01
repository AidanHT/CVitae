import React, { useState } from "react";
import { motion } from "framer-motion";
import { useParams } from "react-router-dom";
import { Download, FileText, Image, Copy, CheckCircle } from "lucide-react";
import { toast } from "react-hot-toast";

const ExportPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [activeFormat, setActiveFormat] = useState("pdf");
  const [isExporting, setIsExporting] = useState(false);
  const [latexCode, setLatexCode] = useState("");

  // Mock resume data
  const resumeData = {
    id: id,
    jobTitle: "Senior Software Engineer",
    companyName: "Google",
    createdAt: new Date().toLocaleDateString(),
    atsScore: 0.92,
  };

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
    setIsExporting(true);
    try {
      // Simulate export API call
      await new Promise((resolve) => setTimeout(resolve, 2000));
      toast.success(`Resume exported as ${format.toUpperCase()}!`);

      if (format === "latex") {
        // Show LaTeX code
        setLatexCode(getMockLatexCode());
      } else {
        // Trigger download for other formats
        const link = document.createElement("a");
        link.href = `#`; // In real app, this would be the download URL
        link.download = `resume.${format}`;
        link.click();
      }
    } catch (error) {
      toast.error("Export failed. Please try again.");
    } finally {
      setIsExporting(false);
    }
  };

  const copyLatexCode = () => {
    navigator.clipboard.writeText(latexCode);
    toast.success("LaTeX code copied to clipboard!");
  };

  const getMockLatexCode = () => {
    return `\\documentclass[letterpaper,11pt]{article}
\\usepackage{latexsym}
\\usepackage[empty]{fullpage}
\\usepackage{titlesec}
\\usepackage{marvosym}
\\usepackage[usenames,dvipsnames]{color}
\\usepackage{verbatim}
\\usepackage{enumitem}
\\usepackage[hidelinks]{hyperref}
\\usepackage{fancyhdr}

\\begin{document}

\\begin{center}
    \\textbf{\\Huge \\scshape John Doe} \\\\ \\vspace{1pt}
    \\small +1-555-123-4567 $|$ \\href{mailto:john@example.com}{\\underline{john@example.com}} $|$ 
    \\href{https://linkedin.com/in/johndoe}{\\underline{linkedin.com/in/johndoe}} $|$
    \\href{https://github.com/johndoe}{\\underline{github.com/johndoe}}
\\end{center}

\\section{Education}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {University of Technology}{City, State}
      {Bachelor of Science in Computer Science}{May 2020}
  \\resumeSubHeadingListEnd

\\section{Experience}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {Software Engineer}{June 2020 -- Present}
      {Tech Company Inc.}{City, State}
      \\resumeItemListStart
        \\resumeItem{Developed scalable web applications using React and Node.js}
        \\resumeItem{Collaborated with cross-functional teams to deliver features}
      \\resumeItemListEnd
  \\resumeSubHeadingListEnd

\\end{document}`;
  };

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
                Generated on {resumeData.createdAt}
              </p>
            </div>
            <div className="text-right">
              <div className="flex items-center text-green-600">
                <CheckCircle className="h-5 w-5 mr-2" />
                <span className="font-semibold">
                  {Math.round(resumeData.atsScore * 100)}% ATS Compatible
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
