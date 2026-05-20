import express from 'express';
import { BrowserController } from './browserController';

const app = express();
const port = 3001;

app.use(express.json());

const bc = new BrowserController();

// Launch browser on server start
bc.launchBrowser().then(() => {
  console.log('Browser launched successfully');
}).catch((error) => {
  console.error('Failed to launch browser:', error);
});

app.get('/test', (req, res) => res.json({ok: true}));

app.post('/navigate', async (req, res) => {
  try {
    console.log('Navigate request received');
    const { url } = req.body;
    console.log('Calling navigateTo');
    await bc.navigateTo(url);
    console.log('Navigate successful');
    res.json({ success: true });
  } catch (error) {
    console.log('Navigate error:', error);
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.post('/click', async (req, res) => {
  try {
    const { selector } = req.body;
    await bc.clickElement(selector);
    res.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.post('/fill', async (req, res) => {
  try {
    const { selector, value } = req.body;
    await bc.fillForm(selector, value);
    res.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.post('/click-at', async (req, res) => {
  try {
    const { x, y } = req.body;
    await bc.clickAt(x, y);
    res.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.post('/type-key', async (req, res) => {
  try {
    const { key } = req.body;
    await bc.typeKey(key);
    res.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/screenshot', async (req, res) => {
  console.log('Screenshot request received');
  try {
    const screenshot = await bc.takeScreenshot();
    console.log('Screenshot taken');
    res.json({ screenshot });
  } catch (error) {
    console.log('Screenshot error:', error);
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/console', async (req, res) => {
  try {
    const logs = await bc.getConsoleLogs();
    res.json({ logs });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/network', async (req, res) => {
  try {
    const requests = await bc.getNetworkRequests();
    res.json({ requests });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/performance', async (req, res) => {
  try {
    const performanceData = await bc.runPerformanceTrace();
    res.json(performanceData);
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/content', async (req, res) => {
  try {
    const content = await bc.getPageContent();
    res.json({ content });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/accessibility', async (req, res) => {
  try {
    const tree = await bc.getAccessibilityTree();
    res.json({ tree });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.post('/scroll', async (req, res) => {
  try {
    const { direction } = req.body;
    await bc.scroll(direction);
    res.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.get('/extract-text', async (req, res) => {
  try {
    const selector = req.query.selector as string | undefined;
    const text = await bc.getExtractedText(selector);
    res.json({ text: text?.trim() ?? '', length: text?.length ?? 0 });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    res.status(500).json({ error: message });
  }
});

app.listen(port, () => {
  console.log(`Browser automation server running on port ${port}`);
});