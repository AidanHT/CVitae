import React from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Brain,
  Target,
  Zap,
  Shield,
  Users,
  Award,
} from "lucide-react";

const AboutPage: React.FC = () => {
  const features = [
    {
      icon: <Brain className="h-8 w-8" />,
      title: "Advanced AI Technology",
      description:
        "Powered by Groq AI for lightning-fast resume optimization and analysis",
    },
    {
      icon: <Target className="h-8 w-8" />,
      title: "ATS Optimization",
      description:
        "Ensures your resume passes through Applicant Tracking Systems with flying colors",
    },
    {
      icon: <Zap className="h-8 w-8" />,
      title: "Lightning Fast",
      description: "Generate professional resumes in seconds, not hours",
    },
    {
      icon: <Shield className="h-8 w-8" />,
      title: "Privacy Focused",
      description:
        "Your data stays secure with enterprise-grade privacy protection",
    },
    {
      icon: <Users className="h-8 w-8" />,
      title: "Expert Designed",
      description:
        "Built by elite engineers with deep hiring and recruitment experience",
    },
    {
      icon: <Award className="h-8 w-8" />,
      title: "Proven Results",
      description:
        "Join thousands who've landed their dream jobs with AI-optimized resumes",
    },
  ];

  const stats = [
    { value: "50,000+", label: "Resumes Generated" },
    { value: "92%", label: "ATS Pass Rate" },
    { value: "3.2x", label: "Interview Rate Increase" },
    { value: "4.8/5", label: "User Satisfaction" },
  ];

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="py-20 sm:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
            >
              <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-6xl mb-6">
                About CVitae
              </h1>
              <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-8">
                We're revolutionizing the job application process with
                AI-powered resume optimization. Our mission is to help every
                professional land their dream job by creating perfectly
                tailored, ATS-optimized resumes.
              </p>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                viewport={{ once: true }}
                className="text-center"
              >
                <div className="text-3xl font-bold text-black mb-2">
                  {stat.value}
                </div>
                <div className="text-gray-600 font-medium">{stat.label}</div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl mb-4">
                Why Choose CVitae?
              </h2>
              <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                Built with cutting-edge technology and deep understanding of
                modern hiring practices
              </p>
            </motion.div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                viewport={{ once: true }}
                className="bg-white p-8 rounded-xl border border-gray-200 hover:shadow-elegant-lg transition-all duration-300"
              >
                <div className="flex items-center justify-center w-16 h-16 bg-black text-white rounded-lg mb-6">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  {feature.title}
                </h3>
                <p className="text-gray-600 leading-6">{feature.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Technology Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl mb-4">
                Built with Elite Technology
              </h2>
              <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                Our stack represents the pinnacle of modern web development and
                AI integration
              </p>
            </motion.div>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            viewport={{ once: true }}
            className="bg-white rounded-xl p-8 shadow-elegant"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
              <div>
                <h3 className="text-xl font-semibold text-gray-900 mb-6">
                  Frontend Excellence
                </h3>
                <ul className="space-y-3 text-gray-600">
                  <li>• React 18+ with TypeScript for type-safe development</li>
                  <li>• Tailwind CSS for rapid, consistent styling</li>
                  <li>• Framer Motion for smooth, engaging animations</li>
                  <li>• Vite for lightning-fast development experience</li>
                  <li>• Modern PWA capabilities for offline access</li>
                </ul>
              </div>
              <div>
                <h3 className="text-xl font-semibold text-gray-900 mb-6">
                  Backend Power
                </h3>
                <ul className="space-y-3 text-gray-600">
                  <li>• Java 17+ with Spring Boot 3.x framework</li>
                  <li>• Groq AI integration for intelligent processing</li>
                  <li>• PostgreSQL with Supabase for reliability</li>
                  <li>• Docker containerization for scalability</li>
                  <li>• Enterprise-grade security and monitoring</li>
                </ul>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Story Section */}
      <section className="py-20">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            viewport={{ once: true }}
            className="text-center"
          >
            <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl mb-8">
              Our Story
            </h2>
            <div className="text-lg text-gray-600 leading-8 space-y-6">
              <p>
                CVitae was born from a simple frustration: talented
                professionals struggling to get past ATS systems and land
                interviews despite having amazing qualifications. We realized
                that the problem wasn't the people—it was the process.
              </p>
              <p>
                As elite software engineers who've been on both sides of the
                hiring table, we understood exactly what ATS systems look for
                and what human recruiters want to see. We combined this
                knowledge with cutting-edge AI technology to create the ultimate
                resume optimization platform.
              </p>
              <p>
                Today, CVitae helps thousands of professionals worldwide turn
                their master resumes into targeted, compelling job applications
                that actually get results. We're not just building
                software—we're building careers.
              </p>
            </div>
          </motion.div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-black text-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            viewport={{ once: true }}
          >
            <h2 className="text-3xl font-bold sm:text-4xl mb-6">
              Ready to Transform Your Career?
            </h2>
            <p className="text-xl text-gray-300 mb-8 max-w-2xl mx-auto">
              Join the thousands of professionals who've already discovered the
              power of AI-optimized resumes
            </p>

            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Link
                to="/builder"
                className="inline-flex items-center bg-white text-black px-8 py-4 text-lg font-semibold rounded-lg hover:bg-gray-100 transition-colors"
              >
                Start Building Your Resume
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </motion.div>
          </motion.div>
        </div>
      </section>
    </div>
  );
};

export default AboutPage;
