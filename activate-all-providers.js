const fs = require('fs');
const fetch = require('node-fetch');

async function activateAll() {
  try {
    if (!fs.existsSync('./auth-token.txt')) {
      console.error('auth-token.txt not found. Please run setup-admin-user.js first.');
      process.exit(1);
    }
    
    const token = fs.readFileSync('./auth-token.txt', 'utf8').trim();
    console.log('Read admin token from auth-token.txt');

    // 1. Fetch all configured providers
    console.log('Fetching configured providers...');
    const listResponse = await fetch('http://localhost:8080/api/admin/providers/configured', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (!listResponse.ok) {
      throw new Error(`Failed to fetch providers: ${listResponse.statusText}`);
    }

    const listData = await listResponse.json();
    if (!listData.success) {
      throw new Error(`Failed to fetch providers: ${JSON.stringify(listData.error)}`);
    }

    const providers = listData.data.providers || [];
    console.log(`Found ${providers.length} providers.`);

    // 2. Loop and activate each provider
    for (const provider of providers) {
      console.log(`\nActivating provider: ${provider.id} (${provider.name}) [Current Status: ${provider.status}]`);
      
      const activateResponse = await fetch(`http://localhost:8080/api/admin/providers/${provider.id}/activate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const activateData = await activateResponse.json();
      if (activateResponse.ok && activateData.success) {
        console.log(`✅ Success: ${provider.id} status is now ${activateData.data.status}`);
      } else {
        console.error(`❌ Failed to activate ${provider.id}:`, activateData.error || activateData.message);
      }
    }

    // 3. Trigger sanitization
    console.log('\nTriggering provider sanitization...');
    const sanitizeResponse = await fetch('http://localhost:8080/api/admin/providers/sanitize', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (sanitizeResponse.ok) {
      const sanitizeData = await sanitizeResponse.json();
      console.log('✅ Sanitization completed:', sanitizeData.data || sanitizeData.message);
    } else {
      console.error('❌ Sanitization failed:', sanitizeResponse.statusText);
    }

    // 4. Fetch updated health stats
    console.log('\nFetching updated health stats...');
    const statsResponse = await fetch('http://localhost:8080/api/admin/providers/health-stats', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (statsResponse.ok) {
      const statsData = await statsResponse.json();
      console.log('✅ Updated Health Stats:', JSON.stringify(statsData.data, null, 2));
    }

  } catch (error) {
    console.error('Error during activation:', error.message);
    process.exit(1);
  }
}

activateAll();
