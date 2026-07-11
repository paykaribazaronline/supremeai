import React from 'react';
import '../src/index.css';

export const withTheme = (Story, context) => {
  const theme = context.globals.theme || 'light';

  // Apply the theme to the body or a wrapper class
  React.useEffect(() => {
    document.body.classList.remove('light', 'dark');
    document.body.classList.add(theme);
  }, [theme]);

  return (
    <div className={`theme-wrapper ${theme} min-h-screen p-8 bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)]`}>
      <Story />
    </div>
  );
};
