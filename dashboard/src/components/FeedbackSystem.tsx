import { motion, AnimatePresence } from "framer-motion";
import {
  Wifi,
  WifiOff,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Info,
  Bell,
} from "lucide-react";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";

import authUtils from "../lib/authUtils";

interface Notification {
  id: string;
  type: "success" | "error" | "warning" | "info";
  message: string;
  description?: string;
  duration?: number;
}

export const ConnectionIndicator: React.FC = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [apiStatus, setApiStatus] = useState<
    "connected" | "error" | "checking"
  >("checking");

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  useEffect(() => {
    let mounted = true;

    const checkApi = async () => {
      if (!mounted) return;
      try {
        const response = await authUtils.fetchWithAuth("/healthCheck");
        if (mounted) {
          if (response.ok) setApiStatus("connected");
          else setApiStatus("error");
        }
      } catch (err) {
        console.warn("Health check failed:", err);
        if (mounted) setApiStatus("error");
      }
    };

    checkApi();
    const interval = setInterval(checkApi, 30000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "8px",
        padding: "6px 12px",
        borderRadius: "12px",
        background: isOnline
          ? "rgba(16, 185, 129, 0.1)"
          : "rgba(239, 68, 68, 0.1)",
        border: `1px solid ${isOnline ? "rgba(16, 185, 129, 0.2)" : "rgba(239, 68, 68, 0.2)"}`,
        backdropFilter: "blur(8px)",
        color: isOnline ? "#10b981" : "#ef4444",
        fontSize: "11px",
        fontWeight: 600,
      }}
    >
      {isOnline ? <Wifi size={14} /> : <WifiOff size={14} />}
      {isOnline
        ? apiStatus === "connected"
          ? "Cloud Online"
          : "Syncing..."
        : "Offline"}
    </motion.div>
  );
};

export const SessionMonitor: React.FC = () => {
  const [expiryWarning, setExpiryWarning] = useState(false);

  useEffect(() => {
    const checkToken = () => {
      const token = authUtils.getToken();
      if (token && token !== "GUEST_MODE") {
        try {
          const payload = JSON.parse(atob(token.split(".")[1]));
          const exp = payload.exp * 1000;
          const timeLeft = exp - Date.now();

          if (timeLeft < 300000 && timeLeft > 0) {
            // 5 minutes warning
            setExpiryWarning(true);
          } else {
            setExpiryWarning(false);
          }
        } catch (e) {
          // Not a JWT or invalid
        }
      }
    };

    const interval = setInterval(checkToken, 60000);
    checkToken();
    return () => clearInterval(interval);
  }, []);

  if (!expiryWarning) return null;

  return (
    <motion.div
      initial={{ y: -50, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      style={{
        position: "fixed",
        top: "80px",
        left: "50%",
        transform: "translateX(-50%)",
        zIndex: 1000,
        background: "rgba(245, 158, 11, 0.9)",
        color: "#000",
        padding: "10px 20px",
        borderRadius: "30px",
        display: "flex",
        alignItems: "center",
        gap: "12px",
        boxShadow: "0 10px 25px rgba(245, 158, 11, 0.4)",
        fontWeight: 600,
        fontSize: "13px",
        backdropFilter: "blur(10px)",
      }}
    >
      <Bell size={18} className="animate-bounce" />
      সেশন শীঘ্রই শেষ হবে। অনুগ্রহ করে পেজটি রিফ্রেশ করুন।
      <button
        onClick={() => window.location.reload()}
        style={{
          background: "#000",
          color: "#fff",
          border: "none",
          padding: "4px 12px",
          borderRadius: "15px",
          cursor: "pointer",
          fontSize: "11px",
        }}
      >
        রিফ্রেশ
      </button>
    </motion.div>
  );
};

export const FeedbackSystem: React.FC = () => {
  const { t } = useTranslation();
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [notifications, setNotifications] = useState<Notification[]>([]);

  // Monitor online/offline status for notifications
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      addNotification(
        "success",
        t("feedback.online_title", "Back Online"),
        t(
          "feedback.online_desc",
          "Your internet connection has been restored.",
        ),
      );
    };
    const handleOffline = () => {
      setIsOnline(false);
      addNotification(
        "error",
        t("feedback.offline_title", "Offline"),
        t("feedback.offline_desc", "You are currently working offline."),
      );
    };

    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);

    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, [t]);

  const addNotification = (
    type: Notification["type"],
    message: string,
    description?: string,
    duration = 5000,
  ) => {
    const id = Math.random().toString(36).substring(2, 9);
    setNotifications((prev) => [
      ...prev,
      { id, type, message, description, duration },
    ]);

    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id);
      }, duration);
    }
  };

  const removeNotification = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  useEffect(() => {
    (window as any)["showNotification"] = addNotification;
    return () => {
      delete (window as any)["showNotification"];
    };
  }, []);

  return (
    <div
      className="feedback-system-container"
      style={{
        position: "fixed",
        top: 20,
        right: 20,
        zIndex: 9999,
        pointerEvents: "none",
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "10px",
          width: "320px",
          pointerEvents: "auto",
        }}
      >
        <AnimatePresence>
          {notifications.map((note) => (
            <motion.div
              key={note.id}
              initial={{ opacity: 0, x: 50, scale: 0.9 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.2 } }}
              className={`notification-card ${note.type}`}
              style={{
                background: "rgba(255, 255, 255, 0.05)",
                backdropFilter: "blur(16px)",
                borderLeft: `4px solid ${
                  note.type === "success"
                    ? "#10b981"
                    : note.type === "error"
                      ? "#ef4444"
                      : note.type === "warning"
                        ? "#f59e0b"
                        : "#3b82f6"
                }`,
                borderRadius: "8px",
                padding: "12px 16px",
                boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1)",
                display: "flex",
                gap: "12px",
                position: "relative",
                overflow: "hidden",
              }}
            >
              <div style={{ marginTop: "2px" }}>
                {note.type === "success" && (
                  <CheckCircle size={18} color="#10b981" />
                )}
                {note.type === "error" && <XCircle size={18} color="#ef4444" />}
                {note.type === "warning" && (
                  <AlertTriangle size={18} color="#f59e0b" />
                )}
                {note.type === "info" && <Info size={18} color="#3b82f6" />}
              </div>
              <div style={{ flex: 1 }}>
                <div
                  style={{ fontWeight: 600, fontSize: "14px", color: "#fff" }}
                >
                  {note.message}
                </div>
                {note.description && (
                  <div
                    style={{
                      fontSize: "12px",
                      color: "rgba(255,255,255,0.6)",
                      marginTop: "4px",
                    }}
                  >
                    {note.description}
                  </div>
                )}
              </div>
              <button
                onClick={() => removeNotification(note.id)}
                style={{
                  background: "none",
                  border: "none",
                  color: "rgba(255,255,255,0.3)",
                  cursor: "pointer",
                  alignSelf: "flex-start",
                }}
              >
                <XCircle size={14} />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default FeedbackSystem;
