import React from 'react';
import '../src/index.css';

const ThemeWrapper = ({ theme, Story }: any) => {
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

export const withTheme = (Story: any, context: any) => {
  const theme = context.globals.theme || 'light';

  return <ThemeWrapper theme={theme} Story={Story} />;
};
