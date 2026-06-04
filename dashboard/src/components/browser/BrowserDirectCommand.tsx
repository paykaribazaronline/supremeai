import { Typography, Button } from "antd";
import React from "react";

const { Text } = Typography;

interface BrowserDirectCommandProps {
  keyInput: string;
  setKeyInput: (val: string) => void;
  handleTypeKey: (key?: string) => void;
}

const BrowserDirectCommand: React.FC<BrowserDirectCommandProps> = ({
  keyInput,
  setKeyInput,
  handleTypeKey,
}) => {
  return (
    <div
      style={{
        padding: "16px 24px",
        background: "#141517",
        borderTop: "1px solid rgba(255, 255, 255, 0.05)",
        display: "flex",
        alignItems: "center",
        gap: 16,
      }}
    >
      <Text
        style={{
          color: "rgba(255,255,255,0.3)",
          fontSize: 10,
          fontWeight: 600,
        }}
      >
        DIRECT CMD
      </Text>
      <input
        placeholder="Inject manual command..."
        value={keyInput}
        onChange={(e) => setKeyInput(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleTypeKey()}
        style={{
          background: "rgba(255,255,255,0.02)",
          color: "#fff",
          flex: 1,
          borderRadius: 8,
          height: 40,
          border: "1px solid rgba(255,255,255,0.05)",
          padding: "0 16px",
          outline: "none",
        }}
      />
      <Button type="primary" onClick={() => handleTypeKey()}>
        EXECUTE
      </Button>
    </div>
  );
};

export default BrowserDirectCommand;
