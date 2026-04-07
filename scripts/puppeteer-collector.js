#!/usr/bin/env node
/**
 * SupremeAI Puppeteer Collector
 * 
 * Browser-based web scraping fallback (emergency only, 10 req/day).
 * Called by BrowserDataCollector.java via ProcessBuilder subprocess.
 * 
 * Usage: node scripts/puppeteer-collector.js <url> [--screenshot]
 * Output: JSON to stdout (parsed by Java caller)
 * Errors: stderr only (never pollute stdout)
 * 
 * Exit codes:
 *   0 = success
 *   1 = invalid arguments
 *   2 = navigation error
 *   3 = timeout
 *   4 = puppeteer launch error
 */

const TIMEOUT_MS = 30000;
const NAVIGATION_TIMEOUT_MS = 20000;

async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args[0] === '--help') {
        process.stderr.write('Usage: node puppeteer-collector.js <url> [--screenshot]\n');
        process.exit(1);
    }
    
    const url = args[0];
    const takeScreenshot = args.includes('--screenshot');
    
    // Validate URL
    try {
        const parsed = new URL(url);
        if (!['http:', 'https:'].includes(parsed.protocol)) {
            throw new Error('Only http/https URLs allowed');
        }
    } catch (e) {
        process.stderr.write(`Invalid URL: ${e.message}\n`);
        process.exit(1);
    }
    
    let browser = null;
    
    try {
        // Dynamic import puppeteer
        let puppeteer;
        try {
            puppeteer = require('puppeteer');
        } catch {
            process.stderr.write('Puppeteer not installed. Run: npm install puppeteer\n');
            process.exit(4);
        }
        
        browser = await puppeteer.launch({
            headless: 'new',
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--single-process'
            ],
            timeout: TIMEOUT_MS
        });
        
        const page = await browser.newPage();
        
        // Set realistic user agent
        await page.setUserAgent(
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
            '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        );
        
        // Set viewport
        await page.setViewport({ width: 1280, height: 720 });
        
        // Block unnecessary resources to speed up
        await page.setRequestInterception(true);
        page.on('request', (req) => {
            const type = req.resourceType();
            if (['image', 'font', 'media'].includes(type)) {
                req.abort();
            } else {
                req.continue();
            }
        });
        
        // Navigate
        const response = await page.goto(url, {
            waitUntil: 'networkidle2',
            timeout: NAVIGATION_TIMEOUT_MS
        });
        
        const statusCode = response ? response.status() : 0;
        
        // Wait for dynamic content
        await page.waitForSelector('body', { timeout: 5000 }).catch(() => {});
        
        // Extract data
        const extracted = await page.evaluate(() => {
            const getMeta = (name) => {
                const el = document.querySelector(
                    `meta[name="${name}"], meta[property="${name}"]`
                );
                return el ? el.getAttribute('content') : null;
            };
            
            // Get visible text, trim whitespace
            const bodyText = document.body ? document.body.innerText : '';
            const truncatedContent = bodyText.substring(0, 50000); // Cap at 50KB
            
            // Collect links
            const links = Array.from(document.querySelectorAll('a[href]'))
                .slice(0, 100)
                .map(a => ({
                    text: (a.textContent || '').trim().substring(0, 200),
                    href: a.href
                }))
                .filter(l => l.href.startsWith('http'));
            
            // Collect headings
            const headings = Array.from(document.querySelectorAll('h1, h2, h3'))
                .slice(0, 50)
                .map(h => ({
                    level: parseInt(h.tagName.charAt(1)),
                    text: (h.textContent || '').trim().substring(0, 300)
                }));
            
            return {
                title: document.title || '',
                content: truncatedContent,
                description: getMeta('description') || getMeta('og:description') || '',
                author: getMeta('author') || '',
                ogImage: getMeta('og:image') || '',
                canonical: (document.querySelector('link[rel="canonical"]') || {}).href || '',
                linkCount: document.querySelectorAll('a[href]').length,
                links: links,
                headings: headings,
                wordCount: bodyText.split(/\s+/).filter(Boolean).length
            };
        });
        
        // Optional screenshot (base64)
        let screenshotBase64 = null;
        if (takeScreenshot) {
            const screenshotBuffer = await page.screenshot({ 
                type: 'png', 
                fullPage: false 
            });
            screenshotBase64 = screenshotBuffer.toString('base64');
        }
        
        // Build result
        const result = {
            success: true,
            url: url,
            statusCode: statusCode,
            title: extracted.title,
            content: extracted.content,
            description: extracted.description,
            author: extracted.author,
            ogImage: extracted.ogImage,
            canonical: extracted.canonical,
            linkCount: extracted.linkCount,
            links: extracted.links,
            headings: extracted.headings,
            wordCount: extracted.wordCount,
            screenshot: screenshotBase64,
            scrapedAt: new Date().toISOString(),
            puppeteerUsed: true
        };
        
        // Output JSON to stdout (Java reads this)
        process.stdout.write(JSON.stringify(result));
        
        await browser.close();
        process.exit(0);
        
    } catch (error) {
        if (browser) {
            await browser.close().catch(() => {});
        }
        
        // Output error as JSON so Java can parse it consistently
        const errorResult = {
            success: false,
            url: url,
            error: error.message,
            scrapedAt: new Date().toISOString(),
            puppeteerUsed: true
        };
        
        process.stdout.write(JSON.stringify(errorResult));
        
        if (error.message.includes('timeout') || error.message.includes('Timeout')) {
            process.exit(3);
        } else if (error.message.includes('net::') || error.message.includes('Navigation')) {
            process.exit(2);
        } else {
            process.exit(4);
        }
    }
}

main();
