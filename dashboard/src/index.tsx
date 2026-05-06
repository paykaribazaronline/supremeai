// index.tsx
(window as any).global = window;
(window as any).process = { env: {} };
import React from 'react';

import ReactDOM from 'react-dom';
import './index.css';
import App from './App';

const root = document.getElementById('root');
ReactDOM.render(<App />, root);
