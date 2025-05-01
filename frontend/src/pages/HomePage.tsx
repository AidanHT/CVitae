import React from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Sparkles,
  Target,
  Zap,
  Brain,
  FileText,
  Download,
} from "lucide-react";

const HomePage: React.FC = () => {
  const features = [
    {
      icon: <Brain className="h-8 w-8" />,
      title: "AI-Powered Intelligence",
      description:
        "Groq AI analyzes job postings and optimizes your resume for maximum impact",
    },
    {
      icon: <Target className="h-8 w-8" />,
      title: "ATS Optimization",
      description:
        "Ensure your resume passes Applicant Tracking Systems with targeted keywords",
    },
    {
      icon: <FileText className="h-8 w-8" />,
      title: "Jake's LaTeX Format",
      description:
        "Professional, clean formatting that recruiters love and ATS systems parse",
    },
    {
      icon: <Download className="h-8 w-8" />,
      title: "Multiple Export Formats",
      description:
        "Download as PDF, PNG, JPG, or get the LaTeX code for further customization",
    },
  ];

  const steps = [
    {
      number: "01",
      title: "Upload Master Resume",
      description:
        "Share your comprehensive resume with all experiences and skills",
    },
    {
      number: "02",
      title: "Paste Job Posting",
      description: "Add the job description you're targeting",
    },
    {
      number: "03",
      title: "AI Optimization",
      description: "Our AI tailors your resume to match the job requirements",
    },
    {
      number: "04",
      title: "Export & Apply",
      description: "Download your optimized resume and apply with confidence",
    },
  ];

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-gray-50 to-white py-20 sm:py-32">
        <div className="absolute inset-0 bg-grid-pattern opacity-5"></div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              className="mx-auto max-w-4xl"
            >
              <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-6xl">
                Transform Your Resume with
                <span className="gradient-text block">Elite AI Technology</span>
              </h1>

              <p className="mt-6 text-lg leading-8 text-gray-600 max-w-2xl mx-auto">
                Turn your master resume into targeted, ATS-optimized resumes
                using cutting-edge AI. Get Jake's LaTeX formatting and
                intelligent keyword optimization.
              </p>

              <div className="mt-10 flex items-center justify-center gap-x-6">
                <motion.div
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <Link to="/builder" className="btn-primary text-lg px-8 py-4">
                    Start Building
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Link>
                </motion.div>

                <Link
                  to="/about"
                  className="text-lg font-semibold leading-6 text-gray-900 hover:text-gray-700 transition-colors"
                >
                  Learn more <span aria-hidden="true">→</span>
                </Link>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl">
                Why Choose CVitae?
              </h2>
              <p className="mt-4 text-lg text-gray-600">
                Built by elite engineers for professionals who demand excellence
              </p>
            </motion.div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                viewport={{ once: true }}
                className="card text-center hover:shadow-elegant-lg transition-all duration-300"
              >
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-lg bg-black text-white mb-6">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  {feature.title}
                </h3>
                <p className="text-gray-600">{feature.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl">
                How It Works
              </h2>
              <p className="mt-4 text-lg text-gray-600">
                Four simple steps to your perfect resume
              </p>
            </motion.div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {steps.map((step, index) => (
              <motion.div
                key={step.number}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.15 }}
                viewport={{ once: true }}
                className="text-center"
              >
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-black text-white text-xl font-bold mb-6">
                  {step.number}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  {step.title}
                </h3>
                <p className="text-gray-600">{step.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24 bg-black text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            viewport={{ once: true }}
          >
            <h2 className="text-3xl font-bold sm:text-4xl mb-6">
              Ready to Land Your Dream Job?
            </h2>
            <p className="text-xl text-gray-300 mb-8 max-w-2xl mx-auto">
              Join thousands of professionals who've transformed their careers
              with AI-optimized resumes
            </p>

            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Link
                to="/builder"
                className="inline-flex items-center bg-white text-black px-8 py-4 text-lg font-semibold rounded-lg hover:bg-gray-100 transition-colors"
              >
                <Sparkles className="mr-2 h-5 w-5" />
                Start Building Now
              </Link>
            </motion.div>
          </motion.div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
