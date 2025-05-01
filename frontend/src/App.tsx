import { Routes, Route } from "react-router-dom";
import { motion } from "framer-motion";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import ResumeBuilderPage from "./pages/ResumeBuilderPage";
import ExportPage from "./pages/ExportPage";
import AboutPage from "./pages/AboutPage";

function App() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="min-h-screen bg-white"
    >
      <Layout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/builder" element={<ResumeBuilderPage />} />
          <Route path="/export/:id" element={<ExportPage />} />
          <Route path="/about" element={<AboutPage />} />
        </Routes>
      </Layout>
    </motion.div>
  );
}

export default App;
