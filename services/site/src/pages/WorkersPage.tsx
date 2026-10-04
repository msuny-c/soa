import { PlusOutlined } from '@ant-design/icons';
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import { Button, Card, Empty, Flex, Form, InputNumber, Space } from 'antd';
import { useState } from 'react';
import { api, type ListParams } from '../api';
import { ColumnsPicker } from '../components/ColumnsPicker';
import { ErrorAlert } from '../components/ErrorAlert';
import { AddFilterButton, FilterChips, SortButton } from '../components/TableToolbar';
import { useWorkerActions } from '../components/WorkerActions';
import { DEFAULT_COLUMNS, WorkersTable } from '../components/WorkersTable';
import { toQuery, type FilterRow } from '../filters';
import { toSortParam, type SortItem } from '../sorting';
import { int32Rule, integerRule, minRule, requiredRule } from '../validation';

export function WorkersPage() {
  const [filters, setFilters] = useState<FilterRow[]>([]);
  const [sort, setSort] = useState<SortItem[]>([]);
  const [pagination, setPagination] = useState({ page: 0, size: 10 });
  const [columns, setColumns] = useState(DEFAULT_COLUMNS);
  const actions = useWorkerActions();

  const params: ListParams = {
    sort: toSortParam(sort),
    filter: filters.map(toQuery),
    page: pagination.page,
    size: pagination.size,
  };

  const list = useQuery({
    queryKey: ['workers', params],
    queryFn: () => api.listWorkers(params),
    placeholderData: keepPreviousData,
  });

  const lookup = useMutation({ mutationFn: api.getWorker, onSuccess: actions.openDetails });

  const applyFilters = (next: FilterRow[]) => {
    setFilters(next);
    setPagination((p) => ({ ...p, page: 0 }));
  };

  const applySort = (next: SortItem[]) => {
    setSort(next);
    setPagination((p) => ({ ...p, page: 0 }));
  };

  const empty =
    filters.length > 0 ? (
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="По заданным условиям никого не нашлось">
        <Button onClick={() => applyFilters([])}>Сбросить фильтры</Button>
      </Empty>
    ) : (
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Сотрудников пока нет">
        <Button type="primary" onClick={() => actions.openEditor(null)}>
          Добавить сотрудника
        </Button>
      </Empty>
    );

  return (
    <Card
      size="small"
      title={list.data ? `Сотрудники · ${list.data.totalElements}` : 'Сотрудники'}
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => actions.openEditor(null)}>
          Добавить сотрудника
        </Button>
      }
    >
      <Flex justify="space-between" wrap gap={8} style={{ marginBottom: 12 }}>
        <Space wrap>
          <AddFilterButton onAdd={(row) => applyFilters([...filters, row])} />
          <SortButton sort={sort} onChange={applySort} />
          <ColumnsPicker value={columns} onChange={setColumns} />
        </Space>
        <Form layout="inline" validateTrigger="onBlur" onFinish={({ workerId }: { workerId: number }) => lookup.mutate(workerId)}>
          <Form.Item
            name="workerId"
            rules={[
              requiredRule('Введите ID'),
              integerRule('ID — целое число'),
              minRule(1, 'ID должен быть больше 0'),
              int32Rule('Слишком большой ID'),
            ]}
          >
            <InputNumber style={{ width: 150 }} placeholder="ID сотрудника" onChange={() => lookup.reset()} />
          </Form.Item>
          <Button htmlType="submit" loading={lookup.isPending}>
            Найти по ID
          </Button>
        </Form>
      </Flex>
      <FilterChips filters={filters} onChange={applyFilters} />
      <ErrorAlert error={lookup.error} onClose={lookup.reset} messages={{ 404: 'Сотрудник с таким ID не найден' }} />
      <ErrorAlert
        error={list.error}
        messages={{ 400: 'Не удалось применить фильтр или сортировку', 422: 'Проверьте значения в условиях фильтра' }}
      />
      {actions.elements}
      <WorkersTable
        page={list.data}
        loading={list.isFetching}
        visibleColumns={columns}
        sort={sort}
        onSortChange={applySort}
        onPageChange={(page, size) => setPagination({ page, size })}
        onRowClick={actions.openDetails}
        actions={actions.renderActions}
        empty={empty}
      />
    </Card>
  );
}
