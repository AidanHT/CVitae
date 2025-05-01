import React from "react";
import { motion } from "framer-motion";
import Header from "./Header";
import Footer from "./Footer";
import ChatBot from "./ChatBot";

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <motion.main
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="flex-1"
      >
        {children}
      </motion.main>

      <Footer />
      <ChatBot />
    </div>
  );
};

export default Layout;
