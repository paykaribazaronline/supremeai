import React from "react";
import { DashboardLayout } from "./DashboardLayout";

interface DashboardShellProps {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  isServerOnline: boolean;
  workspace: React.ReactNode;
}

export const DashboardShell: React.FC<DashboardShellProps> = ({
  theme,
  toggleTheme,
  isServerOnline,
  workspace
}) => {
  return (
    <DashboardLayout title="Dashboard">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Workspace */}
        <div className="lg:col-span-2">
          {workspace}
        </div>

        {/* Right Column - Server Status */}
        <div className="space-y-6">
          {/* Server Status */}
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-gray-200 dark:border-slate-800 p-4">
            <div className="flex items-center">
              <div className={`w-3 h-3 rounded-full mr-2 ${isServerOnline ? 'bg-green-500' : 'bg-red-500'}`}></div>
              <span className="text-sm font-medium text-gray-700 dark:text-slate-300">
                Server Status: {isServerOnline ? 'Online' : 'Offline'}
              </span>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};