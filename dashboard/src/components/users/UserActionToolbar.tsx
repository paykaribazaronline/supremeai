import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ClearOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
} from "@ant-design/icons";
import { Input, Button, Select, Space, Typography, Tooltip } from "antd";
import React from "react";

import { UserSortField } from "./types";

const { Option } = Select;
const { Text } = Typography;

interface UserActionToolbarProps {
  searchTerm: string;
  setSearchTerm: (value: string) => void;
  sortBy: UserSortField | null;
  setSortBy: (value: UserSortField | null) => void;
  sortOrder: "ascend" | "descend";
  setSortOrder: (value: "ascend" | "descend") => void;
  onAddUser: () => void;
  onRefresh: () => void;
  minimal?: boolean;
}

const UserActionToolbar: React.FC<UserActionToolbarProps> = ({
  searchTerm,
  setSearchTerm,
  sortBy,
  setSortBy,
  sortOrder,
  setSortOrder,
  onAddUser,
  onRefresh,
}) => {
  return (
    <div
      className="glass-card"
      style={{
        marginBottom: 24,
        padding: "20px 24px",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        flexWrap: "wrap",
        gap: "20px",
        borderRadius: "16px",
        background: "rgba(255, 255, 255, 0.02)",
        border: "1px solid rgba(255, 255, 255, 0.08)",
        backdropFilter: "blur(12px)",
        boxShadow: "0 4px 24px -1px rgba(0, 0, 0, 0.2)",
      }}
    >
      <div
        style={{
          display: "flex",
          gap: "12px",
          alignItems: "center",
          flex: "1 1 300px",
        }}
      >
        <Input
          placeholder="ইমেইল বা নাম দিয়ে খুঁজুন..."
          prefix={
            <SearchOutlined style={{ color: "rgba(255,255,255,0.25)" }} />
          }
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{
            maxWidth: 320,
            borderRadius: "10px",
            background: "rgba(255, 255, 255, 0.04)",
            border: "1px solid rgba(255, 255, 255, 0.1)",
            color: "#fff",
            height: "42px",
          }}
          className="dark-input"
          suffix={
            searchTerm && (
              <ClearOutlined
                onClick={() => setSearchTerm("")}
                style={{ cursor: "pointer", color: "rgba(255,255,255,0.45)" }}
              />
            )
          }
        />
      </div>

      <div
        style={{
          display: "flex",
          gap: "20px",
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <Space size="large">
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: "11px",
                textTransform: "uppercase",
                letterSpacing: "1.2px",
                fontWeight: 600,
              }}
            >
              সর্ট করুন
            </Text>
            <Select
              value={sortBy || ""}
              onChange={(value) =>
                setSortBy(value === "" ? null : (value as UserSortField))
              }
              style={{ width: 180 }}
              className="premium-select"
              dropdownClassName="premium-dropdown"
              placeholder="বাছাই করুন"
            >
              <Option value="">ডিফল্ট (Default)</Option>
              <Option value="email">ইমেইল ঠিকানা</Option>
              <Option value="displayName">পূর্ণ নাম</Option>
              <Option value="tier">অ্যাকাউন্ট টিয়ার</Option>
              <Option value="isActive">অ্যাকাউন্ট স্ট্যাটাস</Option>
              <Option value="usagePercent">ব্যবহারের হার (%)</Option>
              <Option value="currentUsage">মোট ব্যবহার</Option>
              <Option value="monthlyQuota">মান্থলি কোটা</Option>
              <Option value="lastLoginAt">সর্বশেষ লগইন</Option>
            </Select>
          </div>

          <Tooltip
            title={
              sortOrder === "ascend" ? "ক্রমানুসারে" : "বিপরীত ক্রমানুসারে"
            }
          >
            <Button
              onClick={() =>
                setSortOrder(sortOrder === "ascend" ? "descend" : "ascend")
              }
              className="sort-order-btn"
              icon={
                sortOrder === "ascend" ? (
                  <SortAscendingOutlined />
                ) : (
                  <SortDescendingOutlined />
                )
              }
              style={{
                borderRadius: "10px",
                background: "rgba(255, 255, 255, 0.04)",
                border: "1px solid rgba(255, 255, 255, 0.1)",
                color: "#fff",
                width: "42px",
                height: "42px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                transition: "all 0.3s ease",
              }}
            />
          </Tooltip>
        </Space>

        <div
          style={{
            height: "32px",
            width: "1px",
            background: "rgba(255,255,255,0.08)",
          }}
        />

        <Space size="middle">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={onAddUser}
            style={{
              borderRadius: "10px",
              height: "42px",
              background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
              border: "none",
              padding: "0 24px",
              fontWeight: 600,
              boxShadow: "0 4px 12px rgba(59, 130, 246, 0.3)",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            নতুন ইউজার
          </Button>

          <Tooltip title="রিফ্রেশ করুন">
            <Button
              icon={<ReloadOutlined />}
              onClick={onRefresh}
              style={{
                borderRadius: "10px",
                height: "42px",
                width: "42px",
                background: "rgba(255, 255, 255, 0.04)",
                border: "1px solid rgba(255, 255, 255, 0.1)",
                color: "#fff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            />
          </Tooltip>
        </Space>
      </div>
    </div>
  );
};

export default UserActionToolbar;
