import { CloseOutlined, DownOutlined, PlusOutlined, SortAscendingOutlined, UpOutlined } from '@ant-design/icons';
import { Button, Flex, Popover, Select, Space, Tag, Typography } from 'antd';
import { useState } from 'react';
import { WORKER_FIELDS } from '../api';
import { describe, fieldOf, type FilterRow } from '../filters';
import type { SortItem } from '../sorting';
import { FilterEditor } from './FilterEditor';

export function AddFilterButton({ onAdd }: { onAdd: (row: FilterRow) => void }) {
  const [open, setOpen] = useState(false);
  return (
    <Popover
      trigger="click"
      placement="bottomLeft"
      open={open}
      onOpenChange={setOpen}
      destroyOnHidden
      content={
        <FilterEditor
          submitText="Добавить"
          onSubmit={(row) => {
            onAdd(row);
            setOpen(false);
          }}
          onCancel={() => setOpen(false)}
        />
      }
    >
      <Button icon={<PlusOutlined />}>Фильтр</Button>
    </Popover>
  );
}

export function FilterChips({ filters, onChange }: { filters: FilterRow[]; onChange: (filters: FilterRow[]) => void }) {
  const [editing, setEditing] = useState<number | null>(null);
  if (filters.length === 0) {
    return null;
  }
  return (
    <Space size={[8, 8]} wrap style={{ marginBottom: 12 }}>
      {filters.map((row, index) => (
        <Popover
          key={`${index}-${describe(row)}`}
          trigger="click"
          placement="bottomLeft"
          open={editing === index}
          onOpenChange={(open) => setEditing(open ? index : null)}
          destroyOnHidden
          content={
            <FilterEditor
              initial={row}
              submitText="Сохранить"
              onSubmit={(next) => {
                onChange(filters.map((r, i) => (i === index ? next : r)));
                setEditing(null);
              }}
              onCancel={() => setEditing(null)}
            />
          }
        >
          <Tag
            closable
            style={{ marginInlineEnd: 0, cursor: 'pointer' }}
            onClose={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onChange(filters.filter((_, i) => i !== index));
            }}
          >
            {describe(row)}
          </Tag>
        </Popover>
      ))}
      <Button type="link" size="small" onClick={() => onChange([])}>
        Сбросить все
      </Button>
    </Space>
  );
}

export function SortButton({ sort, onChange }: { sort: SortItem[]; onChange: (sort: SortItem[]) => void }) {
  const available = WORKER_FIELDS.filter((f) => !sort.some((s) => s.field === f.name));
  const move = (index: number, delta: number) => {
    const next = [...sort];
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item);
    onChange(next);
  };
  return (
    <Popover
      trigger="click"
      placement="bottomLeft"
      title="Сортировка"
      content={
        <div style={{ width: 360 }}>
          {sort.length === 0 ? (
            <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
              Сейчас — по ID. Добавьте поля в нужном порядке: сначала сортируется по первому.
            </Typography.Paragraph>
          ) : (
            <Flex vertical gap={4} style={{ marginBottom: 12 }}>
              {sort.map((item, index) => (
                <Flex key={item.field} align="center" gap={4}>
                  <Typography.Text type="secondary" style={{ width: 20 }}>
                    {index + 1}.
                  </Typography.Text>
                  <Typography.Text ellipsis style={{ flex: 1 }}>
                    {fieldOf(item.field)?.label ?? item.field}
                  </Typography.Text>
                  <Button
                    size="small"
                    style={{ width: 128 }}
                    onClick={() => onChange(sort.map((s) => (s.field === item.field ? { ...s, desc: !s.desc } : s)))}
                  >
                    {item.desc ? 'по убыванию' : 'по возрастанию'}
                  </Button>
                  <Button
                    size="small"
                    type="text"
                    aria-label="Выше"
                    icon={<UpOutlined />}
                    disabled={index === 0}
                    onClick={() => move(index, -1)}
                  />
                  <Button
                    size="small"
                    type="text"
                    aria-label="Ниже"
                    icon={<DownOutlined />}
                    disabled={index === sort.length - 1}
                    onClick={() => move(index, 1)}
                  />
                  <Button
                    size="small"
                    type="text"
                    aria-label="Убрать"
                    icon={<CloseOutlined />}
                    onClick={() => onChange(sort.filter((s) => s.field !== item.field))}
                  />
                </Flex>
              ))}
            </Flex>
          )}
          {available.length > 0 && (
            <Select
              size="small"
              style={{ width: '100%' }}
              showSearch
              optionFilterProp="label"
              placeholder="+ Добавить поле"
              value={null}
              options={available.map((f) => ({ value: f.name, label: f.label }))}
              onChange={(field: string) => onChange([...sort, { field, desc: false }])}
            />
          )}
        </div>
      }
    >
      <Button icon={<SortAscendingOutlined />}>{sort.length > 0 ? `Сортировка · ${sort.length}` : 'Сортировка'}</Button>
    </Popover>
  );
}
