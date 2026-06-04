import {
  RobotOutlined,
  AimOutlined,
  ThunderboltOutlined,
  DatabaseOutlined,
} from "@ant-design/icons";
import { Card, Typography, Space, Tooltip, Badge } from "antd";
import React from "react";

import type { AnalysisResponse } from "../types";

const { Text } = Typography;

interface AnalysisResultsDisplayProps {
  response: AnalysisResponse | null;
}

const AnalysisResultsDisplay: React.FC<AnalysisResultsDisplayProps> = ({
  response,
}) => {
  if (!response) return null;

  return (
    <Card
      size="small"
      style={{
        marginBottom: 16,
        border: "1px solid rgba(255,255,255,0.1)",
        background: "rgba(0,0,0,0.2)",
        borderRadius: 8,
      }}
    >
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap" }}>
        {response.ragUsed && (
          <Tooltip title="RAG context selection was used to filter relevant files">
            <Badge count={<AimOutlined style={{ color: "#00f3ff" }} />}>
              <Text style={{ color: "#fff", fontSize: 12 }}>RAG</Text>
            </Badge>
          </Tooltip>
        )}

        {response.incrementalUsed && (
          <Tooltip
            title={`Incremental analysis: ${response.changedFiles} changed files, ${response.cachedFindings} cached`}
          >
            <Badge count={<DatabaseOutlined style={{ color: "#34c759" }} />}>
              <Text style={{ color: "#fff", fontSize: 12 }}>
                Incremental ({response.changedFiles} changes)
              </Text>
            </Badge>
          </Tooltip>
        )}

        {response.fixes && response.fixes.length > 0 && (
          <Tooltip
            title={`${response.fixes.length} LLM-generated fix suggestions available`}
          >
            <Badge count={response.fixes.length}>
              <Space>
                <ThunderboltOutlined style={{ color: "#ffcc00" }} />
                <Text style={{ color: "#fff", fontSize: 12 }}>Fixes</Text>
              </Space>
            </Badge>
          </Tooltip>
        )}

        {response.filesAnalyzed > 0 &&
          response.totalFiles &&
          response.totalFiles > 0 && (
            <Text style={{ color: "var(--text-dim)", fontSize: 12 }}>
              Scanned {response.filesAnalyzed} of {response.totalFiles} files
            </Text>
          )}
      </div>
    </Card>
  );
};

export default AnalysisResultsDisplay;
