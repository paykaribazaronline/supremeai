import { chromium, Browser, Page } from 'playwright';

export class BrowserController {
  private browser: Browser | null = null;
  private page: Page | null = null;

  async launchBrowser(): Promise<void> {
    console.log('Launching browser with Playwright...');
    this.browser = await chromium.launch({ headless: true });
    this.page = await this.browser.newPage();
    console.log('Browser launched');
  }

  async closeBrowser(): Promise<void> {
    if (this.browser) {
      await this.browser.close();
      this.browser = null;
      this.page = null;
    }
  }

  async navigateTo(url: string): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    console.log('Navigating to:', url);
    await this.page.goto(url);
    console.log('Navigation complete');
  }

  async clickElement(selector: string): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    await this.page.click(selector);
  }

  async fillForm(selector: string, value: string): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    await this.page.fill(selector, value);
  }

  async takeScreenshot(): Promise<string> {
    if (!this.page) throw new Error('Browser not launched');
    const buffer = await this.page.screenshot({ type: 'png' });
    return buffer.toString('base64');
  }

  async clickAt(x: number, y: number): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    await this.page.mouse.click(x, y);
  }

  async typeKey(key: string): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    await this.page.keyboard.press(key);
  }

  async getConsoleLogs(): Promise<any[]> {
    // Playwright does not have built-in console log collection like CDP
    // This is a simplified version
    return [];
  }

  async getNetworkRequests(): Promise<any[]> {
    if (!this.page) throw new Error('Browser not launched');
    const requests: any[] = [];
    this.page.on('request', (request) => {
      requests.push({
        url: request.url(),
        method: request.method(),
        headers: request.headers()
      });
    });
    // Wait a bit
    await new Promise(resolve => setTimeout(resolve, 2000));
    return requests;
  }

  async runPerformanceTrace(): Promise<any> {
    if (!this.page) throw new Error('Browser not launched');
    // Simplified performance analysis
    const metrics = await this.page.evaluate(() => {
      const perf = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        domContentLoaded: perf.domContentLoadedEventEnd - perf.domContentLoadedEventStart,
        loadComplete: perf.loadEventEnd - perf.loadEventStart
      };
    });
    return {
      insights: [`DOM Content Loaded: ${metrics.domContentLoaded}ms`, `Load Complete: ${metrics.loadComplete}ms`]
    };
  }

  async getPageContent(): Promise<string> {
    if (!this.page) throw new Error('Browser not launched');
    return await this.page.content();
  }

  async getAccessibilityTree(): Promise<any> {
    if (!this.page) throw new Error('Browser not launched');
    return await this.page.accessibility.snapshot();
  }

  async scroll(direction: string): Promise<void> {
    if (!this.page) throw new Error('Browser not launched');
    const scrollAmount = direction === 'down' ? 500 : direction === 'up' ? -500 : 0;
    if (scrollAmount !== 0) {
      await this.page.evaluate((amount) => window.scrollBy(0, amount), scrollAmount);
    } else if (direction === 'top') {
      await this.page.evaluate(() => window.scrollTo(0, 0));
    } else if (direction === 'bottom') {
      await this.page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    }
  }

  async getExtractedText(selector?: string): Promise<string> {
    if (!this.page) throw new Error('Browser not launched');
    if (selector) {
      return await this.page.$eval(selector, (el) => el.textContent || '');
    } else {
      return await this.page.evaluate(() => document.body.innerText || '');
    }
  }
}