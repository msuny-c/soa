import { SettingOutlined } from '@ant-design/icons';
import { Button, Checkbox, Popover, Space } from 'antd';
import { DEFAULT_COLUMNS, WORKER_COLUMNS } from './WorkersTable';

export function ColumnsPicker({ value, onChange }: { value: string[]; onChange: (columns: string[]) => void }) {
  return (
    <Popover
      trigger="click"
      placement="bottomLeft"
      title="Колонки таблицы"
      content={
        <Space direction="vertical">
          <Checkbox.Group
            style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
            value={value}
            options={WORKER_COLUMNS.map((c) => ({ value: c.key, label: c.title }))}
            onChange={(checked) => {
              const next = WORKER_COLUMNS.map((c) => c.key).filter((key) => checked.includes(key));
              if (next.length > 0) {
                onChange(next);
              }
            }}
          />
          <Button type="link" size="small" style={{ padding: 0 }} onClick={() => onChange(DEFAULT_COLUMNS)}>
            По умолчанию
          </Button>
        </Space>
      }
    >
      <Button icon={<SettingOutlined />}>Колонки</Button>
    </Popover>
  );
}
