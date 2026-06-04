import {
  BarChartOutlined,
  RocketOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  GlobalOutlined,
} from "@ant-design/icons";
import {
  Card,
  Row,
  Col,
  Statistic,
  Empty,
  Spin,
  Typography,
  Divider,
} from "antd";
import React, { useState, useEffect } from "react";

const { Title, Text } = Typography;

const UsageStats: React.FC = () => {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchUsage = async () => {
      setLoading(true);
      try {
        const token = localStorage.getItem("authToken");
        const response = await fetch("/api/apikeys/usage", {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.ok) setStats(await response.json());
      } catch {
        // Fail silently for demo if backend is not ready
      } finally {
        setLoading(false);
      }
    };
    fetchUsage();
  }, []);

  if (loading)
    return (
      <div style={{ textAlign: "center", padding: "50px" }}>
        <Spin size="large" tip="লোড হচ্ছে..." />
      </div>
    );

  return (
    <div className="usage-stats">
      <Card className="premium-card">
        <Title level={4}>
          <BarChartOutlined /> এপিআই ব্যবহারের পরিসংখ্যান
        </Title>
        <Text type="secondary">
          আপনার সিস্টেমের সকল এপিআই কী-এর ব্যবহারের রিয়েল-টাইম তথ্য।
        </Text>
        <Divider />

        {stats ? (
          <>
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={6}>
                <Card size="small" className="stat-card">
                  <Statistic
                    title="মোট রিকোয়েস্ট"
                    value={stats.totalRequests ?? 0}
                    prefix={<RocketOutlined style={{ color: "#1890ff" }} />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small" className="stat-card">
                  <Statistic
                    title="অ্যাক্টিভ কী"
                    value={stats.activeKeys ?? 0}
                    prefix={
                      <CheckCircleOutlined style={{ color: "#52c41a" }} />
                    }
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small" className="stat-card">
                  <Statistic
                    title="আনুমানিক খরচ"
                    value={stats.totalCost ?? 0}
                    prefix={<DollarOutlined style={{ color: "#faad14" }} />}
                    precision={2}
                    suffix="USD"
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Card size="small" className="stat-card">
                  <Statistic
                    title="প্রোভাইডার"
                    value={stats.providers ?? 0}
                    prefix={<GlobalOutlined style={{ color: "#722ed1" }} />}
                  />
                </Card>
              </Col>
            </Row>

            <div
              style={{
                marginTop: "30px",
                padding: "40px",
                background: "#f0f2f5",
                borderRadius: "12px",
                textAlign: "center",
              }}
            >
              <Title level={5} type="secondary">
                গ্রাফিকাল চার্ট শীঘ্রই আসছে...
              </Title>
              <Text type="secondary">
                ব্যবহারের ট্রেন্ড এবং প্রোভাইডার ভিত্তিক খরচের চার্ট পরবর্তী
                আপডেটে যুক্ত হবে।
              </Text>
            </div>
          </>
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="এখনও কোনো ব্যবহারের তথ্য নেই। এপিআই কী ব্যবহার শুরু করলে এখানে ডাটা দেখাবে।"
          />
        )}
      </Card>
    </div>
  );
};

export default UsageStats;
