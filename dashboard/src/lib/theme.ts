import type { ThemeConfig } from 'antd';

export const supremeTheme: ThemeConfig = {
  token: {
    colorPrimary: '#7C3AED',
    colorSuccess: '#10B981',
    colorWarning: '#F59E0B',
    colorError: '#EF4444',
    colorInfo: '#3B82F6',
    colorBgBase: '#FFFFFF',
    colorBgContainer: '#FFFFFF',
    colorBorder: '#E5E7EB',
    colorText: '#111827',
    colorTextSecondary: '#6B7280',
    borderRadius: 12,
    borderRadiusLG: 16,
    borderRadiusSM: 8,
    fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
    fontSize: 14,
    lineHeight: 1.5,
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
    boxShadowSecondary: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
    boxShadowTertiary: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
  },
  components: {
    Card: {
      padding: 24,
      paddingLG: 32,
      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
      boxShadowHover: '0 10px 40px -10px rgba(0, 0, 0, 0.15)',
    },
    Button: {
      borderRadius: 10,
      controlHeight: 44,
      controlHeightLG: 52,
    },
    Input: {
      borderRadius: 10,
      controlHeight: 44,
    },
    Menu: {
      itemBorderRadius: 10,
      itemHeight: 48,
      darkItemBg: 'transparent',
      darkSubMenuItemBg: 'transparent',
    },
    Layout: {
      siderBg: '#0F172A',
      headerBg: '#FFFFFF',
      bodyBg: '#F8FAFC',
    },
    Table: {
      borderRadius: 12,
      headerBg: '#F8FAFC',
    },
    Statistic: {
      titleFontSize: 12,
      contentFontSize: 28,
    },
    Tabs: {
      borderRadius: 10,
      horizontalItemPadding: '16px 20px',
    },
  }
};

export const gradientBg = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
export const darkGradientBg = 'linear-gradient(135deg, #0F172A 0%, #1E293B 100%)';
export const purpleGradient = 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)';
