import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App, ConfigProvider } from 'antd';
import ruRU from 'antd/locale/ru_RU';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { Root } from './App';
import { appTheme } from './theme';

dayjs.locale('ru');

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } });

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider locale={ruRU} theme={appTheme} card={{ variant: 'outlined' }}>
      <App>
        <QueryClientProvider client={queryClient}>
          <HashRouter>
            <Root />
          </HashRouter>
        </QueryClientProvider>
      </App>
    </ConfigProvider>
  </StrictMode>,
);
