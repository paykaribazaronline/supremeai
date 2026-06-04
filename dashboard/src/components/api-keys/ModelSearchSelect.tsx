// ModelSearchSelect.tsx - Searchable select component for AI models

import { QuestionCircleOutlined } from "@ant-design/icons";
import { Select, Tag, Typography, Space, Spin } from "antd";
import React, { useState, useEffect } from "react";

import { useModelSearch } from "../../hooks/useModelSearch";

import { POPULAR_MODELS, getProviderEndpoint } from "./constants";
import { ModelSearchResult } from "./types";

const { Text } = Typography;

interface ModelSearchSelectProps {
  value?: string;
  onChange: (model: ModelSearchResult | null) => void;
  placeholder?: string;
}

export const ModelSearchSelect: React.FC<ModelSearchSelectProps> = ({
  value,
  onChange,
  placeholder = "Search AI models... e.g., 'GPT-4', 'Llama', 'Claude'",
}) => {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const { results, loading, search, setResults } = useModelSearch(500, [
    "huggingface",
    "openrouter",
  ]);

  const selectedModel = React.useMemo(() => {
    if (!value) return null;
    const popular = POPULAR_MODELS.find((m) => m.id === value);
    if (popular) return popular;
    return {
      id: value,
      name: value,
      provider: "custom",
      providerTitle: "Custom",
      baseUrl: getProviderEndpoint(value.split("/")[0] || ""),
      description: "Custom or discovered model",
      category: "Discovered",
    };
  }, [value]);

  useEffect(() => {
    if (open && query.trim()) {
      search(query);
    }
  }, [query, open, search]);

  const handleSearch = (newQuery: string) => {
    setQuery(newQuery);
    if (newQuery.trim()) {
      search(newQuery);
    } else {
      setResults([]);
    }
  };

  const handleSelect = (model: ModelSearchResult) => {
    setQuery("");
    setOpen(false);
    setResults([]);
    onChange(model);
  };

  const mergedResults = React.useMemo(() => {
    const popularAsModels: ModelSearchResult[] = POPULAR_MODELS.map((m) => ({
      id: m.id,
      name: m.name,
      provider: m.provider,
      providerTitle: m.providerTitle,
      baseUrl: m.baseUrl,
      description: m.description,
      category: m.category,
    }));
    const all = [...popularAsModels, ...results];
    const seen = new Set<string>();
    return all.filter((m) => {
      if (seen.has(m.id)) return false;
      seen.add(m.id);
      return true;
    });
  }, [results]);

  const quickAccessModels = POPULAR_MODELS.slice(0, 8);

  return (
    <div style={{ position: "relative" }}>
      {!value && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary" style={{ fontSize: 12, marginRight: 8 }}>
            Popular:
          </Text>
          {quickAccessModels.map((model) => (
            <Tag
              key={model.id}
              style={{ cursor: "pointer", marginBottom: 4 }}
              color="blue"
              onClick={() => {
                handleSelect({
                  id: model.id,
                  name: model.name,
                  provider: model.provider,
                  providerTitle: model.providerTitle,
                  baseUrl: model.baseUrl,
                  description: model.description,
                  category: model.category,
                });
              }}
            >
              {model.name}
            </Tag>
          ))}
        </div>
      )}
      <Select
        showSearch
        size="large"
        value={selectedModel?.id || undefined}
        placeholder={placeholder}
        onSearch={handleSearch}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 200)}
        onChange={(val) => {
          const model = mergedResults.find((m) => m.id === val) || null;
          onChange(model);
        }}
        filterOption={false}
        style={{ width: "100%" }}
        notFoundContent={
          loading ? (
            <Spin size="small" />
          ) : query ? (
            "No models found."
          ) : (
            "Type to search HuggingFace & OpenRouter..."
          )
        }
      >
        {mergedResults.slice(0, 30).map((model) => (
          <Select.Option key={model.id} value={model.id}>
            <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <Space>
                <Text strong>{model.name}</Text>
                <Tag color="blue" style={{ marginLeft: 4 }}>
                  {model.providerTitle}
                </Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 11 }}>
                {model.description}
              </Text>
            </div>
          </Select.Option>
        ))}
      </Select>
      {loading && (
        <div
          style={{
            position: "absolute",
            right: 12,
            top: "50%",
            transform: "translateY(-50%)",
            zIndex: 1,
          }}
        >
          <Spin size="small" />
        </div>
      )}
    </div>
  );
};
