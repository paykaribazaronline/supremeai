# 📄 ফাইল: packages/ui-components/src/ChatBubble.tsx

**প্রকার:** .tsx  
**সাইজ:** 566 বাইট  
**আপডেট:** 2026-07-05T14:19:11.176142

---

## কোড

```tsx
import React from 'react';

export interface ChatBubbleProps {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
}

export const ChatBubble: React.FC<ChatBubbleProps> = ({ role, content, timestamp }) => {
  const isUser = role === 'user';
  const timeStr = timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <div className={`message ${role}`}>
      <div className="msg-bubble">{content}</div>
      <div className="msg-info">{isUser ? 'Admin' : 'SupremeAI'} • {timeStr}</div>
    </div>
  );
};

```