import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Typography, 
  Space, 
  Tag, 
  Spin, 
  Empty, 
  Button,
  Modal,
  message
} from 'antd';
import { 
  PlayCircleOutlined, 
  InfoCircleOutlined,
  ClockCircleOutlined,
  FolderOutlined
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import './VideoTutorials.css';

const { Title, Paragraph, Text } = Typography;

interface UserGuide {
  id?: string;
  title: Record<string, string>;
  description: Record<string, string>;
  videoUrl: Record<string, string>;
  thumbnailUrl?: string;
  order?: number;
  category: string;
  durationSeconds?: number;
  isPublished?: boolean;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string;
}

interface VideoTutorialsProps {
  // Optional: filter by category
  category?: string;
  // Show as grid or list
  layout?: 'grid' | 'list';
}

const VideoTutorials: React.FC<VideoTutorialsProps> = ({ 
  category, 
  layout = 'grid' 
}) => {
  const { t, i18n } = useTranslation();
  const [guides, setGuides] = useState<UserGuide[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedGuide, setSelectedGuide] = useState<UserGuide | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  const currentLang = i18n.language.split('-')[0] || 'en'; // 'en' or 'bn'

  useEffect(() => {
    fetchGuides();
  }, [category]);

  const fetchGuides = async () => {
    setLoading(true);
    try {
      let url = '/api/guides';
      if (category) {
        url = `/api/guides/category/${encodeURIComponent(category)}`;
      }
      
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error('Failed to fetch guides');
      }
      const data = await response.json();
      // Convert Firestore document array to expected format
      const guidesArray = Array.isArray(data) ? data : [data];
      setGuides(guidesArray);
    } catch (error) {
      console.error('Error fetching guides:', error);
      message.error('Failed to load video tutorials');
    } finally {
      setLoading(false);
    }
  };

  const openVideoModal = (guide: UserGuide) => {
    setSelectedGuide(guide);
    setModalVisible(true);
  };

  const closeModal = () => {
    setModalVisible(false);
    setSelectedGuide(null);
  };

  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const getVideoEmbedUrl = (url: string) => {
    // Convert YouTube URLs to embed format if needed
    if (url.includes('youtube.com/watch')) {
      const videoId = url.split('v=')[1]?.split('&')[0];
      return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
    }
    if (url.includes('youtu.be')) {
      const videoId = url.split('/').pop();
      return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
    }
    return url;
  };

  if (loading) {
    return (
      <div className="video-tutorials-loading">
        <Spin size="large" />
        <p>Loading tutorials...</p>
      </div>
    );
  }

  if (guides.length === 0) {
    return (
      <Empty 
        description={
          <span>
            {t('no_tutorials_available', 'No tutorials available yet.')}
          </span>
        }
        image={Empty.PRESENTED_IMAGE_SIMPLE}
      />
    );
  }

  const renderGuideCard = (guide: UserGuide) => (
    <Col xs={24} sm={12} md={8} lg={6} key={guide.id} className="guide-card-wrapper">
      <Card
        hoverable
        className="guide-card"
        cover={
          guide.thumbnailUrl ? (
            <img 
              alt={guide.title[currentLang] || guide.title['en']} 
              src={guide.thumbnailUrl} 
              className="guide-thumbnail"
            />
          ) : (
            <div className="guide-thumbnail-placeholder">
              <PlayCircleOutlined />
            </div>
          )
        }
        onClick={() => openVideoModal(guide)}
      >
        <Card.Meta
          title={
            <Space>
              <span>{guide.title[currentLang] || guide.title['en']}</span>
              {guide.durationSeconds && (
                <Tag icon={<ClockCircleOutlined />} color="blue">
                  {formatDuration(guide.durationSeconds)}
                </Tag>
              )}
            </Space>
          }
          description={
            <div>
              <Paragraph 
                ellipsis={{ rows: 2 }}
                className="guide-description"
              >
                {guide.description[currentLang] || guide.description['en']}
              </Paragraph>
              {guide.tags && guide.tags.length > 0 && (
                <Space size={[0, 8]} wrap className="guide-tags">
                  {guide.tags.slice(0, 3).map(tag => (
                    <Tag key={tag} color="default">{tag}</Tag>
                  ))}
                </Space>
              )}
            </div>
          }
        />
      </Card>
    </Col>
  );

  return (
    <div className="video-tutorials">
      <div className="video-tutorials-header">
        <Title level={3}>
          <FolderOutlined /> {t('video_tutorials', 'Video Tutorials')}
        </Title>
        <Paragraph type="secondary">
          {t('video_tutorials_desc', 'Learn how to use SupremeAI with these step-by-step video guides.')}
        </Paragraph>
      </div>

      {layout === 'grid' ? (
        <Row gutter={[16, 16]}>
          {guides.sort((a, b) => (a.order || 0) - (b.order || 0)).map(renderGuideCard)}
        </Row>
      ) : (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          {guides.sort((a, b) => (a.order || 0) - (b.order || 0)).map(guide => (
            <Card 
              key={guide.id}
              hoverable
              className="guide-list-card"
              onClick={() => openVideoModal(guide)}
            >
              <Row align="middle" gutter={16}>
                <Col flex="200px">
                  {guide.thumbnailUrl ? (
                    <img 
                      src={guide.thumbnailUrl} 
                      alt={guide.title[currentLang] || guide.title['en']}
                      className="guide-list-thumbnail"
                    />
                  ) : (
                    <div className="guide-thumbnail-placeholder guide-list-thumbnail">
                      <PlayCircleOutlined />
                    </div>
                  )}
                </Col>
                <Col flex="auto">
                  <Space direction="vertical" size={0}>
                    <Title level={4} style={{ margin: 0 }}>
                      {guide.title[currentLang] || guide.title['en']}
                      {guide.durationSeconds && (
                        <Tag icon={<ClockCircleOutlined />} color="blue" style={{ marginLeft: 8 }}>
                          {formatDuration(guide.durationSeconds)}
                        </Tag>
                      )}
                    </Title>
                    <Paragraph 
                      ellipsis={{ rows: 2 }}
                      type="secondary"
                    >
                      {guide.description[currentLang] || guide.description['en']}
                    </Paragraph>
                    {guide.tags && guide.tags.length > 0 && (
                      <Space size={[0, 8]} wrap>
                        {guide.tags.map(tag => (
                          <Tag key={tag} color="default">{tag}</Tag>
                        ))}
                      </Space>
                    )}
                  </Space>
                </Col>
                <Col>
                  <Button type="primary" icon={<PlayCircleOutlined />}>
                    {t('watch_now', 'Watch Now')}
                  </Button>
                </Col>
              </Row>
            </Card>
          ))}
        </Space>
      )}

      {/* Video Modal */}
      <Modal
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width="80%"
        style={{ top: 20 }}
        title={
          <Space>
            <InfoCircleOutlined />
            <span>
              {selectedGuide?.title[currentLang] || selectedGuide?.title['en']}
            </span>
          </Space>
        }
      >
        {selectedGuide && (
          <div className="video-modal-content">
            <div className="video-container">
              <iframe
                src={getVideoEmbedUrl(selectedGuide.videoUrl[currentLang] || selectedGuide.videoUrl['en'] || '')}
                title={selectedGuide.title[currentLang] || selectedGuide.title['en']}
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                className="video-iframe"
              />
            </div>
            <div className="video-description">
              <Title level={4}>
                {selectedGuide.title[currentLang] || selectedGuide.title['en']}
              </Title>
              <Paragraph>
                {selectedGuide.description[currentLang] || selectedGuide.description['en']}
              </Paragraph>
              {selectedGuide.durationSeconds && (
                <Text type="secondary">
                  <ClockCircleOutlined /> Duration: {formatDuration(selectedGuide.durationSeconds)}
                </Text>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default VideoTutorials;
