# 📄 ফাইল: apps/studio-client/src/lib/etag.ts

**প্রকার:** .ts  
**সাইজ:** 731 বাইট  
**আপডেট:** 2026-07-07T11:35:20.620818

---

## কোড

```ts
export async function etagify(response: Response): Promise<Response> {
  const newHeaders = new Headers(response.headers);
  const etag = crypto.randomUUID().split('-')[0];
  newHeaders.set('ETag', etag);
  return new Response(response.body, {
    status: response.status,
    headers: newHeaders,
  });
}

export function checkETag(currentEtag: string, serverEtag: string): boolean {
  return currentEtag !== serverEtag;
}

export function generateETag(data: unknown): string {
  const str = typeof data === 'string' ? data : JSON.stringify(data);
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash;
  }
  return `${hash}`;
}
```