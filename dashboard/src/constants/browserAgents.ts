export interface BrowserAILink {
  id: string;
  name: string;
  url: string;
  type: 'web_scrape' | 'api' | 'extension';
  selectors?: {
    input: string;
    submit: string;
    response: string;
  };
}

// ব্রাউজার স্ক্র্যাপিংয়ের জন্য ডাইনামিক ওয়েব লিংক লিস্ট
export const BROWSER_AI_LINKS: BrowserAILink[] = [
  {
    id: 'openai-gpt-4o',
    name: 'ChatGPT (GPT-4o)',
    url: 'https://chatgpt.com/?model=gpt-4o',
    type: 'web_scrape',
    selectors: {
      input: '#prompt-textarea',
      submit: 'button[data-testid="send-button"]',
      response: '.markdown.prose'
    }
  },
  {
    id: 'openai-gpt-4',
    name: 'ChatGPT (GPT-4)',
    url: 'https://chatgpt.com/?model=gpt-4',
    type: 'web_scrape',
    selectors: {
      input: '#prompt-textarea',
      submit: 'button[data-testid="send-button"]',
      response: '.markdown.prose'
    }
  },
  {
    id: 'anthropic-claude',
    name: 'Claude 3.5 Sonnet',
    url: 'https://claude.ai/new',
    type: 'web_scrape',
    selectors: {
      input: 'div[contenteditable="true"]',
      submit: 'button[aria-label="Send Message"]',
      response: '.font-user-message'
    }
  },
  {
    id: 'google-gemini',
    name: 'Google Gemini',
    url: 'https://gemini.google.com/app',
    type: 'web_scrape',
    selectors: {
      input: 'rich-textarea',
      submit: 'button[aria-label="Send message"]',
      response: 'message-content'
    }
  }
];
