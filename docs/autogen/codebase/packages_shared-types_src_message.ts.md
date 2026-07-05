# 📄 ফাইল: packages/shared-types/src/message.ts

**প্রকার:** .ts  
**সাইজ:** 558 বাইট  
**আপডেট:** 2026-07-05T02:47:34.421778

---

## কোড

```ts
import { z } from 'zod';

export const ToolCallSchema = z.object({
  id: z.string(),
  name: z.string(),
  arguments: z.record(z.string(), z.unknown()),
  result: z.string().optional(),
  status: z.enum(['pending', 'success', 'error']),
});

export type ToolCall = z.infer<typeof ToolCallSchema>;

export const MessageSchema = z.object({
  id: z.string(),
  role: z.enum(['user', 'assistant', 'system']),
  content: z.string(),
  timestamp: z.date(),
  toolCalls: z.array(ToolCallSchema).optional(),
});

export type Message = z.infer<typeof MessageSchema>;

```