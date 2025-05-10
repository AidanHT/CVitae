import React, { useState, useEffect, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Wifi, WifiOff, Loader2, AlertCircle, CheckCircle } from "lucide-react";
import { checkApiHealth } from "../services/api";

type ApiStatus = "connecting" | "connected" | "error" | "offline";

interface ApiActivity {
  id: string;
  method: string;
  endpoint: string;
  status: "pending" | "success" | "error";
  timestamp: number;
  duration?: number;
  traceId?: string;
}

interface ApiActivityIndicatorProps {
  showDetails?: boolean;
  className?: string;
}

export const ApiActivityIndicator: React.FC<ApiActivityIndicatorProps> = ({
  showDetails = false,
  className = "",
}) => {
  const [status, setStatus] = useState<ApiStatus>("connecting");
  const [activities, setActivities] = useState<ApiActivity[]>([]);
  const [isExpanded, setIsExpanded] = useState(false);
  const counterRef = useRef(0);

  const generateUniqueId = useCallback(() => {
    counterRef.current += 1;
    return `activity-${Date.now()}-${counterRef.current}`;
  }, []);

  useEffect(() => {
    // Initial health check
    checkHealth();

    // Set up periodic health checks
    const healthInterval = setInterval(checkHealth, 30000); // Every 30 seconds

    // Listen to console logs for API activities (development only)
    if (import.meta.env.DEV) {
      const originalLog = console.log;
      console.log = (...args) => {
        originalLog(...args);
        handleConsoleLog(args);
      };
    }

    return () => {
      clearInterval(healthInterval);
    };
  }, []);

  const checkHealth = async () => {
    try {
      setStatus("connecting");
      const isHealthy = await checkApiHealth();
      setStatus(isHealthy ? "connected" : "error");
    } catch {
      setStatus("offline");
    }
  };

  const handleConsoleLog = (args: any[]) => {
    const message = args.join(" ");

    // Parse API request logs
    if (message.includes("🚀 API Request:")) {
      const match = message.match(/🚀 API Request: (\w+) (.+)/);
      if (match) {
        const [, method, endpoint] = match;
        const activity: ApiActivity = {
          id: generateUniqueId(),
          method,
          endpoint: endpoint.replace(/^.*\/api/, ""), // Remove base URL
          status: "pending",
          timestamp: Date.now(),
        };
        setActivities((prev) => [activity, ...prev.slice(0, 9)]); // Keep last 10
      }
    }

    // Parse API response logs
    if (message.includes("📡 API Response:")) {
      const match = message.match(
        /📡 API Response: (\d+) in (\d+)ms \[Trace: ([^\]]+)\]/
      );
      if (match) {
        const [, statusCode, duration, traceId] = match;
        const isSuccess = parseInt(statusCode) < 400;

        setActivities((prev) =>
          prev.map((activity) =>
            activity.status === "pending"
              ? {
                  ...activity,
                  status: isSuccess ? "success" : "error",
                  duration: parseInt(duration),
                  traceId,
                }
              : activity
          )
        );
      }
    }

    // Parse API error logs
    if (message.includes("❌ API Error") || message.includes("💥 API Error")) {
      setActivities((prev) =>
        prev.map((activity) =>
          activity.status === "pending"
            ? { ...activity, status: "error" }
            : activity
        )
      );
    }
  };

  const getStatusIcon = () => {
    switch (status) {
      case "connecting":
        return <Loader2 className="h-4 w-4 animate-spin text-yellow-500" />;
      case "connected":
        return <Wifi className="h-4 w-4 text-green-500" />;
      case "error":
        return <AlertCircle className="h-4 w-4 text-red-500" />;
      case "offline":
        return <WifiOff className="h-4 w-4 text-gray-500" />;
    }
  };

  const getStatusText = () => {
    switch (status) {
      case "connecting":
        return "Connecting...";
      case "connected":
        return "API Connected";
      case "error":
        return "API Error";
      case "offline":
        return "API Offline";
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case "connecting":
        return "text-yellow-600 bg-yellow-50 border-yellow-200";
      case "connected":
        return "text-green-600 bg-green-50 border-green-200";
      case "error":
        return "text-red-600 bg-red-50 border-red-200";
      case "offline":
        return "text-gray-600 bg-gray-50 border-gray-200";
    }
  };

  const recentActivities = activities.slice(0, 5);
  const pendingCount = activities.filter((a) => a.status === "pending").length;

  return (
    <div className={`relative ${className}`}>
      <motion.div
        className={`flex items-center gap-2 px-3 py-2 rounded-lg border text-sm font-medium cursor-pointer ${getStatusColor()}`}
        onClick={() => showDetails && setIsExpanded(!isExpanded)}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        {getStatusIcon()}
        <span className="hidden sm:inline">{getStatusText()}</span>
        {pendingCount > 0 && (
          <motion.span
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="bg-blue-500 text-white text-xs rounded-full px-2 py-0.5 min-w-[20px] text-center"
          >
            {pendingCount}
          </motion.span>
        )}
        {showDetails && (
          <motion.div
            animate={{ rotate: isExpanded ? 180 : 0 }}
            transition={{ duration: 0.2 }}
          >
            ▼
          </motion.div>
        )}
      </motion.div>

      <AnimatePresence>
        {showDetails && isExpanded && (
          <motion.div
            initial={{ opacity: 0, y: -10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="absolute top-full right-0 mt-2 w-80 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-96 overflow-hidden"
          >
            <div className="p-3 border-b border-gray-100">
              <h3 className="font-semibold text-gray-900">API Activity</h3>
              <p className="text-sm text-gray-500">
                Recent requests and responses
              </p>
            </div>

            <div className="max-h-64 overflow-y-auto">
              {recentActivities.length === 0 ? (
                <div className="p-4 text-center text-gray-500">
                  No recent API activity
                </div>
              ) : (
                recentActivities.map((activity) => (
                  <motion.div
                    key={activity.id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="p-3 border-b border-gray-50 last:border-b-0"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {activity.status === "pending" && (
                          <Loader2 className="h-3 w-3 animate-spin text-blue-500" />
                        )}
                        {activity.status === "success" && (
                          <CheckCircle className="h-3 w-3 text-green-500" />
                        )}
                        {activity.status === "error" && (
                          <AlertCircle className="h-3 w-3 text-red-500" />
                        )}
                        <span className="text-xs font-mono bg-gray-100 px-2 py-1 rounded">
                          {activity.method}
                        </span>
                      </div>
                      {activity.duration && (
                        <span className="text-xs text-gray-500">
                          {activity.duration}ms
                        </span>
                      )}
                    </div>
                    <div className="mt-1 text-sm text-gray-700 truncate">
                      {activity.endpoint}
                    </div>
                    {activity.traceId && (
                      <div className="mt-1 text-xs text-gray-400 font-mono">
                        Trace: {activity.traceId}
                      </div>
                    )}
                  </motion.div>
                ))
              )}
            </div>

            <div className="p-3 border-t border-gray-100">
              <button
                onClick={checkHealth}
                className="w-full text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                Check API Health
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
