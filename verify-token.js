const jwt = require('jsonwebtoken');
const fs = require('fs');

const secret = 'mySuperSecretKeyForLocalDevelopment12345678901234567890';
const token = 'eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJGMzVTNEFTWExwT1l0cE1jdWlUOHNOTnFrbWcyIiwiaWF0IjoxNzc3MjM5MDQ3LCJleHAiOjE3NzcyNzUwNDcsInJvbGUiOiJhZG1pbiJ9.Nvyw9wxAdGoMua8FyJJdXFNy7momkyKsbwDxdJFSptddyJkXezGwPl16Z4uVlQTP';

try {
  const decoded = jwt.verify(token, secret);
  console.log('Token is valid. Claims:', decoded);
} catch (err) {
  console.error('Invalid token:', err.message);
  // Try to decode without verification
  const decoded = jwt.decode(token);
  console.log('Decoded (no verify):', decoded);
}
