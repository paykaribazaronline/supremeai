// বাংলা মন্তব্য: অ্যাডমিন সাইটের কনসোল এরর চেক করার জন্য প্লে-রাইট স্ক্রিপ্ট
const { chromium } = require('@playwright/test');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  const consoleErrors = [];
  const consoleWarnings = [];
  const consoleLogs = [];
  const pageErrors = [];
  const failedRequests = [];

  // কনসোল মেসেজ ক্যাপচার
  page.on('console', (msg) => {
    const text = msg.text();
    if (msg.type() === 'error') {
      consoleErrors.push(text);
    } else if (msg.type() === 'warning') {
      consoleWarnings.push(text);
    } else {
      consoleLogs.push(text);
    }
  });

  // পেজ এরর ক্যাপচার
  page.on('pageerror', (err) => {
    pageErrors.push(err.message);
  });

  // ফেইলড রিকোয়েস্ট ক্যাপচার
  page.on('requestfailed', (req) => {
    failedRequests.push({
      url: req.url(),
      failure: req.failure()?.errorText || 'unknown'
    });
  });

  // পেজ লোড
  await page.goto('https://supremeai-admin.web.app/admin', {
    waitUntil: 'networkidle',
    timeout: 60000
  });

  // কিছুক্ষণ অপেক্ষা করে আরও এরর ক্যাপচার
  await page.waitForTimeout(10000);

  console.log('=== CONSOLE ERRORS ===');
  consoleErrors.forEach(e => console.log('ERROR:', e));
  
  console.log('\n=== CONSOLE WARNINGS ===');
  consoleWarnings.forEach(w => console.log('WARN:', w));
  
  console.log('\n=== PAGE ERRORS ===');
  pageErrors.forEach(e => console.log('PAGE_ERROR:', e));
  
  console.log('\n=== FAILED REQUESTS ===');
  failedRequests.forEach(r => console.log('FAILED:', r.url, '-', r.failure));

  console.log('\n=== CONSOLE LOGS (first 20) ===');
  consoleLogs.slice(0, 20).forEach(l => console.log('LOG:', l));

  await browser.close();
})();