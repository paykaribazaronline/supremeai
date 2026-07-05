export function setupGlobalFetchInterceptor() {
  if (typeof window === 'undefined') return;

  const originalFetch = window.fetch;

  window.fetch = async function (...args) {
    try {
      const response = await originalFetch.apply(this, args);
      
      if (!response.ok) {
        let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
        try {
          const clone = response.clone();
          const text = await clone.text();
          if (text) {
             const parsed = JSON.parse(text);
             if (parsed.error) errorMsg = parsed.error;
             else if (parsed.message) errorMsg = parsed.message;
             else if (parsed.detail) errorMsg = typeof parsed.detail === 'string' ? parsed.detail : JSON.stringify(parsed.detail);
             else errorMsg = text.slice(0, 50);
          }
        } catch (e) {
          // ignore parsing error
        }

        if ((window as any).showGlobalToast) {
          (window as any).showGlobalToast('error', errorMsg);
        }
      }
      
      return response;
    } catch (error) {
      if ((window as any).showGlobalToast) {
        (window as any).showGlobalToast('error', `Network Error: ${error instanceof Error ? error.message : 'Unknown'}`);
      }
      throw error;
    }
  };
}
