import { Layout, Tabs, Typography } from 'antd';
import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { HrPage } from './pages/HrPage';
import { OperationsPage } from './pages/OperationsPage';
import { WorkersPage } from './pages/WorkersPage';

const SECTIONS: { path: string; label: string; element: ReactNode }[] = [
  { path: '/workers', label: 'Сотрудники', element: <WorkersPage /> },
  { path: '/reports', label: 'Отчёты и поиск', element: <OperationsPage /> },
  { path: '/indexation', label: 'Индексация зарплат', element: <HrPage /> },
];

export function Root() {
  const location = useLocation();
  const navigate = useNavigate();
  const active = SECTIONS.find((s) => location.pathname.startsWith(s.path))?.path;

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Content style={{ padding: 24, maxWidth: 1600, width: '100%', margin: '0 auto' }}>
        <Typography.Title level={4}>Управление сотрудниками</Typography.Title>
        <Tabs activeKey={active} onChange={navigate} items={SECTIONS.map(({ path, label }) => ({ key: path, label }))} />
        <Routes>
          {SECTIONS.map(({ path, element }) => (
            <Route key={path} path={path} element={element} />
          ))}
          <Route path="*" element={<Navigate to={SECTIONS[0].path} replace />} />
        </Routes>
      </Layout.Content>
    </Layout>
  );
}
