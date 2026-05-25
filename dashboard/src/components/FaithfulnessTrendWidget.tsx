import React, { useEffect, useState } from 'react';
import { Card, Spin, Typography } from 'antd';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import axios from 'axios';

const { Title } = Typography;

const FaithfulnessTrendWidget: React.FC = () => {
	const [data, setData] = useState<any[]>([]);
	const [loading, setLoading] = useState<boolean>(true);

	useEffect(() => {
		const fetchData = async () => {
			try {
				const response = await axios.get('/api/knowledge/recent-scraped?limit=50');
				const sortedData = response.data
					.map((item: any) => ({
						topic: item.topic,
						score: item.qualityScore,
						date: new Date(item.learnedAt).toLocaleTimeString()
					}))
					.reverse(); // Chronological order
				setData(sortedData);
			} catch (error) {
				console.error("Failed to fetch faithfulness trend:", error);
			} finally {
				setLoading(false);
			}
		};

		fetchData();
		const interval = setInterval(fetchData, 60000); // Refresh every minute
		return () => clearInterval(interval);
	}, []);

	if (loading) return <Card className="glass-card"><Spin /></Card>;

	return (
		<Card className="glass-card" style={{ marginBottom: 20 }}>
			<Title level={4} style={{ color: '#00f3ff' }}>🧠 Knowledge Faithfulness Trend</Title>
			<div style={{ height: 250, width: '100%' }}>
				<ResponsiveContainer>
					<LineChart data={data}>
						<CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
						<XAxis
							dataKey="date"
							stroke="#8b5cf6"
							tick={{ fontSize: 10 }}
						/>
						<YAxis
							domain={[0, 1]}
							stroke="#8b5cf6"
						/>
						<Tooltip
							contentStyle={{ backgroundColor: '#020205', borderColor: '#00f3ff' }}
							itemStyle={{ color: '#00f3ff' }}
						/>
						<Line
							type="monotone"
							dataKey="score"
							stroke="#00f3ff"
							strokeWidth={3}
							dot={{ r: 4, fill: '#00f3ff' }}
							activeDot={{ r: 8 }}
							name="Faithfulness Score"
						/>
					</LineChart>
				</ResponsiveContainer>
			</div>
			<div style={{ marginTop: 10, fontSize: 11, color: 'rgba(255,255,255,0.45)' }}>
				* Visualizing last 50 acquisition events judged by MultiAIVotingService.
			</div>
		</Card>
	);
};

export default FaithfulnessTrendWidget;
