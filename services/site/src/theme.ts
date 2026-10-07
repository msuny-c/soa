import type { ThemeConfig } from 'antd';

const text = '#1c1d1f';
const textSecondary = '#6b6f76';
const textTertiary = '#8a8f98';
const border = '#e3e4e8';
const borderSoft = '#ececef';
const surfaceHover = '#f6f6f7';

export const appTheme: ThemeConfig = {
  token: {
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    fontSize: 13,
    fontSizeHeading4: 18,
    colorPrimary: '#5e6ad2',
    colorLink: '#5e6ad2',
    colorText: text,
    colorTextSecondary: textSecondary,
    colorTextTertiary: textTertiary,
    colorTextPlaceholder: textTertiary,
    colorBorder: border,
    colorBorderSecondary: borderSoft,
    colorSplit: borderSoft,
    colorBgLayout: '#f7f7f8',
    colorFillAlter: '#fafafb',
    controlHeight: 30,
    controlHeightSM: 24,
    controlHeightLG: 36,
    borderRadius: 6,
    borderRadiusSM: 4,
    borderRadiusLG: 8,
    boxShadow: 'none',
    boxShadowSecondary: '0 0 0 1px rgba(28, 29, 31, 0.06), 0 8px 24px rgba(28, 29, 31, 0.08)',
    boxShadowTertiary: 'none',
    lineHeight: 1.5,
    motionDurationMid: '0.12s',
    motionDurationSlow: '0.18s',
  },
  components: {
    Card: {
      borderRadiusLG: 0,
      headerFontSizeSM: 13,
      headerHeightSM: 40,
      headerPaddingSM: 12,
      bodyPaddingSM: 12,
    },
    Table: {
      headerBg: 'transparent',
      headerColor: textSecondary,
      headerSplitColor: 'transparent',
      headerSortActiveBg: 'transparent',
      headerSortHoverBg: surfaceHover,
      bodySortBg: 'transparent',
      rowHoverBg: surfaceHover,
      borderColor: borderSoft,
      cellFontSizeSM: 13,
      cellPaddingBlockSM: 9,
      cellPaddingInlineSM: 12,
      headerBorderRadius: 0,
    },
    Button: {
      fontWeight: 500,
      defaultShadow: 'none',
      primaryShadow: 'none',
      dangerShadow: 'none',
      defaultBorderColor: border,
      defaultHoverBg: surfaceHover,
      defaultHoverColor: text,
      defaultHoverBorderColor: border,
      paddingInline: 12,
    },
    Tabs: {
      itemColor: textSecondary,
      itemHoverColor: text,
      itemSelectedColor: text,
      itemActiveColor: text,
      inkBarColor: text,
      titleFontSize: 13,
      horizontalItemGutter: 24,
      horizontalItemPadding: '10px 0',
      horizontalMargin: '0 0 16px 0',
    },
    Tag: {
      defaultBg: '#f3f3f5',
      defaultColor: text,
    },
    Modal: {
      titleFontSize: 15,
    },
    Typography: {
      titleMarginBottom: 12,
    },
    Form: {
      itemMarginBottom: 14,
      verticalLabelPadding: '0 0 4px',
      inlineItemMarginBottom: 0,
    },
  },
};
