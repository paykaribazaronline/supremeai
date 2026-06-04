import {
  SearchOutlined,
  StarOutlined,
  ThunderboltOutlined,
  LinkOutlined,
  GlobalOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import {
  Card,
  Typography,
  Select,
  Row,
  Col,
  Input,
  Button,
  Tooltip,
  Space,
  Tag,
  List,
  Empty,
  Spin,
  message,
} from "antd";
import React, { useState, useCallback } from "react";

import { POPULAR_MODELS } from "./constants";
import { HuggingFaceModel, OpenRouterModel, GoogleAIModel } from "./types";

const { Title, Text, Paragraph } = Typography;
const { Search } = Input;

const ModelDiscovery: React.FC = () => {
  const [query, setQuery] = useState("");
  const [hfModels, setHfModels] = useState<HuggingFaceModel[]>([]);
  const [orModels, setOrModels] = useState<OpenRouterModel[]>([]);
  const [googleModels, setGoogleModels] = useState<GoogleAIModel[]>([]);
  const [loading, setLoading] = useState(false);
  const [source, setSource] = useState<
    "huggingface" | "openrouter" | "google" | "all"
  >("all");
  const [pipelineFilter, setPipelineFilter] = useState<string>("");
  const [googleApiKey, setGoogleApiKey] = useState("");
  const [popularFilter, setPopularFilter] = useState<string>("all");

  const searchHuggingFace = useCallback(
    async (q: string, pipeline?: string) => {
      const params = new URLSearchParams({
        search: q,
        limit: "30",
        sort: "likes",
        direction: "-1",
      });
      if (pipeline) params.set("pipeline_tag", pipeline);
      const response = await fetch(
        `https://huggingface.co/api/models?${params}`,
      );
      if (response.ok) return (await response.json()) as HuggingFaceModel[];
      return [];
    },
    [],
  );

  const searchOpenRouter = useCallback(async (q: string) => {
    try {
      const response = await fetch("https://openrouter.ai/api/v1/models");
      if (!response.ok) return [];
      const data = await response.json();
      const models = (data.data || []) as OpenRouterModel[];
      if (!q) return models.slice(0, 30);
      const lower = q.toLowerCase();
      return models
        .filter(
          (m) =>
            m.id.toLowerCase().includes(lower) ||
            m.name.toLowerCase().includes(lower),
        )
        .slice(0, 30);
    } catch {
      return [];
    }
  }, []);

  const searchGoogleAI = useCallback(async (q: string, apiKey: string) => {
    try {
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`,
      );
      if (!response.ok) return [];
      const data = await response.json();
      const models = (data.models || []) as GoogleAIModel[];
      if (!q) return models;
      const lower = q.toLowerCase();
      return models.filter(
        (m) =>
          m.name.toLowerCase().includes(lower) ||
          m.displayName.toLowerCase().includes(lower),
      );
    } catch {
      return [];
    }
  }, []);

  const handleSearch = async (searchQuery?: string) => {
    const q = (searchQuery ?? query).trim();
    if (!q) {
      message.info("সার্চ করার জন্য একটি মডেলের নাম বা কীওয়ার্ড লিখুন");
      return;
    }
    setLoading(true);
    try {
      const promises: Promise<any>[] = [];
      if (source !== "openrouter" && source !== "google")
        promises.push(searchHuggingFace(q, pipelineFilter));
      else promises.push(Promise.resolve([]));

      if (source !== "huggingface" && source !== "google")
        promises.push(searchOpenRouter(q));
      else promises.push(Promise.resolve([]));

      if ((source === "google" || source === "all") && googleApiKey.trim()) {
        promises.push(searchGoogleAI(q, googleApiKey.trim()));
      } else {
        promises.push(Promise.resolve([]));
      }

      const [hf, or, google] = await Promise.all(promises);
      setHfModels(hf);
      setOrModels(or);
      setGoogleModels(google);
    } catch {
      message.error(
        "সার্চ করার সময় সমস্যা হয়েছে। আপনার ইন্টারনেট কানেকশন চেক করুন।",
      );
    } finally {
      setLoading(false);
    }
  };

  const formatNumber = (n: number) => {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + "M";
    if (n >= 1_000) return (n / 1_000).toFixed(1) + "K";
    return n.toString();
  };

  return (
    <div className="model-discovery">
      <Card className="premium-card" style={{ marginBottom: 16 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 16,
          }}
        >
          <Title level={4} style={{ margin: 0 }}>
            <StarOutlined style={{ color: "#faad14" }} /> জনপ্রিয় মডেলসমূহ
          </Title>
          <Select
            value={popularFilter}
            onChange={setPopularFilter}
            style={{ width: 180 }}
            options={[
              { value: "all", label: "সব প্রোভাইডার" },
              { value: "Gemini", label: "Google Gemini" },
              { value: "OpenAI", label: "OpenAI" },
              { value: "Anthropic", label: "Anthropic" },
              { value: "Meta", label: "Meta Llama" },
              { value: "DeepSeek", label: "DeepSeek" },
            ]}
          />
        </div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {POPULAR_MODELS.filter(
            (m) => popularFilter === "all" || m.category === popularFilter,
          ).map((model) => (
            <Tag
              key={model.id}
              className="clickable-tag"
              color="blue"
              onClick={() => {
                navigator.clipboard.writeText(model.id);
                message.success(`"${model.name}" কপি করা হয়েছে!`);
              }}
            >
              {model.name}{" "}
              <Text type="secondary" style={{ fontSize: "10px" }}>
                ({model.providerTitle})
              </Text>
            </Tag>
          ))}
        </div>
      </Card>

      <Card className="search-card" style={{ marginBottom: 16 }}>
        <Row gutter={[12, 12]}>
          <Col xs={24} md={12}>
            <Search
              placeholder="মডেল সার্চ করুন (উদাঃ Llama 3, GPT-4, Gemini)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onSearch={() => handleSearch()}
              enterButton={
                <>
                  <SearchOutlined /> সার্চ
                </>
              }
              size="large"
              loading={loading}
            />
          </Col>
          <Col xs={12} md={6}>
            <Select
              value={source}
              onChange={setSource}
              style={{ width: "100%" }}
              size="large"
              options={[
                { value: "all", label: "সব সোর্স" },
                { value: "huggingface", label: "HuggingFace" },
                { value: "openrouter", label: "OpenRouter" },
                { value: "google", label: "Google AI" },
              ]}
            />
          </Col>
          <Col xs={12} md={6}>
            <Button
              type="primary"
              size="large"
              block
              icon={<ThunderboltOutlined />}
              onClick={() => handleSearch()}
            >
              খুঁজুন
            </Button>
          </Col>
        </Row>

        {(source === "google" || source === "all") && (
          <div style={{ marginTop: 12 }}>
            <Input.Password
              placeholder="Google AI API Key দিন (Gemini মডেল সার্চের জন্য)"
              value={googleApiKey}
              onChange={(e) => setGoogleApiKey(e.target.value)}
              style={{ maxWidth: "400px" }}
            />
            <Text
              type="secondary"
              style={{ fontSize: 11, display: "block", marginTop: 4 }}
            >
              আপনার কী আপনার ব্রাউজারেই থাকবে, আমাদের সার্ভারে পাঠানো হবে না।
            </Text>
          </div>
        )}
      </Card>

      <Spin spinning={loading}>
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          {googleModels.length > 0 && (
            <Card title={<span>Google AI Models</span>} size="small">
              <List
                dataSource={googleModels}
                renderItem={(model) => (
                  <List.Item
                    actions={[
                      <Button
                        key="copy"
                        type="link"
                        onClick={() => {
                          navigator.clipboard.writeText(
                            model.name.replace("models/", ""),
                          );
                          message.success("কপি হয়েছে");
                        }}
                      >
                        Copy ID
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      title={model.displayName}
                      description={model.description}
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}

          {hfModels.length > 0 && (
            <Card title={<span>HuggingFace Models</span>} size="small">
              <List
                dataSource={hfModels}
                renderItem={(model) => (
                  <List.Item
                    actions={[
                      <Button
                        key="copy"
                        type="link"
                        onClick={() => {
                          navigator.clipboard.writeText(model.id);
                          message.success("কপি হয়েছে");
                        }}
                      >
                        Copy ID
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      title={model.id}
                      description={`${formatNumber(model.downloads)} downloads • ${formatNumber(model.likes)} likes`}
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}
        </div>
      </Spin>
    </div>
  );
};

export default ModelDiscovery;
