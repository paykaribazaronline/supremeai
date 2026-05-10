// AdminOCRCard.tsx - NEURAL OCR EXTRACTION HUB
import React, { useState } from 'react';
import { Upload, Typography, Progress } from 'antd';
import { 
    InboxOutlined, 
    SyncOutlined, 
    PaperClipOutlined,
    ScanOutlined,
    SafetyCertificateOutlined,
    FileSearchOutlined
} from '@ant-design/icons';

const { Dragger } = Upload;

interface OCRResult {
    id: string;
    fileName: string;
    text: string;
    confidence: number;
    status: 'processing' | 'completed' | 'error';
    byteSize?: string;
}

const AdminOCRCard: React.FC = () => {
    const [results, setResults] = useState<OCRResult[]>([]);

    const uploadProps = {
        name: 'file',
        multiple: true,
        showUploadList: false,
        customRequest: async (options: any) => {
            const { file, onSuccess, onError } = options;
            const newId = Math.random().toString(36).substr(2, 9);
            
            const newResult: OCRResult = {
                id: newId,
                fileName: file.name,
                text: '',
                confidence: 0,
                status: 'processing',
                byteSize: (file.size / 1024).toFixed(1) + ' KB'
            };

            setResults(prev => [newResult, ...prev]);

            try {
                // Simulated Extraction Process
                setTimeout(() => {
                    setResults(prev => prev.map(r => r.id === newId ? {
                        ...r,
                        text: 'EXTRACTED_PAYLOAD: ' + file.name.toUpperCase(),
                        confidence: 0.94 + (Math.random() * 0.05),
                        status: 'completed'
                    } : r));
                    onSuccess("ok");
                }, 2000);

            } catch (err) {
                onError(err);
                setResults(prev => prev.map(r => r.id === newId ? { ...r, status: 'error' } : r));
            }
        }
    };

    return (
        <div className="flex flex-col gap-6">
            {/* OCR Telemetry Strip */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {[
                    { label: 'Neural Accuracy', value: '98.2%', icon: <SafetyCertificateOutlined />, color: 'emerald' },
                    { label: 'Queue Depth', value: results.filter(r => r.status === 'processing').length, icon: <SyncOutlined />, color: 'blue' },
                    { label: 'Total Scans', value: results.length, icon: <ScanOutlined />, color: 'white' }
                ].map((s, idx) => (
                    <div key={idx} className="glass-card p-5 flex flex-col justify-between h-20">
                        <div className="flex items-center justify-between">
                            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">{s.label}</span>
                            <span className={`text-lg text-${s.color}-500/30`}>{s.icon}</span>
                        </div>
                        <span className="text-2xl font-mono font-black text-white leading-none tracking-tighter">{s.value}</span>
                    </div>
                ))}
            </div>

            <Dragger {...uploadProps} className="compact-dragger bg-white/[0.01] border-dashed border-white/10 hover:bg-white/[0.03] hover:border-blue-500/30 transition-all rounded-xl overflow-hidden">
                <div className="py-8">
                    <p className="flex justify-center mb-3">
                        <FileSearchOutlined className="text-blue-500/40 text-3xl" />
                    </p>
                    <p className="text-xs text-white/70 uppercase tracking-[0.3em] font-black">Neural OCR Interface</p>
                    <p className="text-[10px] text-white/20 uppercase tracking-[0.1em] mt-2 italic">Ingest documents for deep pattern analysis</p>
                </div>
            </Dragger>

            {results.length > 0 && (
                <div className="glass-card overflow-hidden">
                    <div className="px-5 py-3 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                            <span className="text-xs font-black uppercase tracking-widest text-white/70">Extraction Manifest</span>
                        </div>
                        <span className="text-[10px] font-mono text-white/20 uppercase tracking-widest">{results.length} ACTIVE_NODES</span>
                    </div>
                    <div className="max-h-[400px] overflow-y-auto custom-scrollbar">
                        {results.map(item => (
                            <div key={item.id} className="px-5 py-4 border-b border-white/[0.03] flex items-center justify-between hover:bg-white/[0.02] transition-colors group">
                                <div className="flex items-center gap-4 overflow-hidden">
                                    <div className={`w-10 h-10 rounded-lg border flex items-center justify-center transition-colors ${
                                        item.status === 'completed' ? 'bg-emerald-500/5 border-emerald-500/20 text-emerald-500' :
                                        item.status === 'processing' ? 'bg-blue-500/5 border-blue-500/20 text-blue-500' : 'bg-red-500/5 border-red-500/20 text-red-500'
                                    }`}>
                                        {item.status === 'processing' ? <SyncOutlined spin className="text-lg" /> : <PaperClipOutlined className="text-lg" />}
                                    </div>
                                    <div className="flex flex-col overflow-hidden">
                                        <div className="flex items-center gap-3">
                                            <span className="text-sm font-black text-white/90 truncate max-w-[200px] uppercase tracking-tight">{item.fileName}</span>
                                            <span className="text-[10px] font-mono text-white/20">{item.byteSize}</span>
                                        </div>
                                        {item.status === 'completed' ? (
                                            <span className="text-xs text-white/40 leading-relaxed truncate font-mono mt-1 group-hover:text-white/60 transition-colors">
                                                {item.text}
                                            </span>
                                        ) : (
                                            <div className="mt-2 flex flex-col gap-1.5 w-32">
                                                <div className="text-[8px] text-blue-500/50 uppercase tracking-widest font-black">Scanning Matrix...</div>
                                                <Progress percent={45} size={[120, 2]} showInfo={false} strokeColor="#3b82f6" trailColor="rgba(255,255,255,0.05)" />
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="flex flex-col items-end gap-2 flex-shrink-0 ml-6">
                                    {item.confidence > 0 && (
                                        <div className="flex flex-col items-end gap-1">
                                            <span className="text-[10px] font-black text-emerald-500 uppercase tracking-tight">{(item.confidence * 100).toFixed(1)}% Precision</span>
                                            <Progress 
                                                percent={item.confidence * 100} 
                                                size={[60, 3]} 
                                                showInfo={false} 
                                                strokeColor="#10b981" 
                                                trailColor="rgba(255,255,255,0.05)" 
                                            />
                                        </div>
                                    )}
                                    {item.status === 'error' && (
                                        <span className="text-[10px] px-2 py-1 bg-red-500/10 text-red-500 border border-red-500/20 rounded-md font-black uppercase tracking-widest">
                                            FAILED
                                        </span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminOCRCard;
