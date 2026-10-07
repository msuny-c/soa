import { Table, Tooltip, type TableProps } from 'antd';
import type { SortOrder } from 'antd/es/table/interface';
import type { ReactNode } from 'react';
import type { Worker, WorkerPage } from '../api';
import {
  formatDate,
  formatDateTime,
  formatDateTimeFull,
  formatPoint,
  formatPosition,
  formatSalary,
  formatText,
} from '../format';
import { mergeHeaderSort, type SortItem } from '../sorting';

export interface WorkerColumn {
  key: string;
  title: string;
  sortField?: string;
  align?: 'right';
  defaultVisible: boolean;
  render: (worker: Worker) => ReactNode;
}

export const WORKER_COLUMNS: WorkerColumn[] = [
  { key: 'id', title: 'ID', sortField: 'id', defaultVisible: true, render: (w) => w.id },
  { key: 'name', title: 'Имя', sortField: 'name', defaultVisible: true, render: (w) => w.name },
  { key: 'position', title: 'Должность', sortField: 'position', defaultVisible: true, render: (w) => formatPosition(w.position) },
  {
    key: 'salary',
    title: 'Зарплата',
    sortField: 'salary',
    align: 'right',
    defaultVisible: true,
    render: (w) => formatSalary(w.salary),
  },
  { key: 'startDate', title: 'Начало работы', sortField: 'startDate', defaultVisible: true, render: (w) => formatDate(w.startDate) },
  {
    key: 'endDate',
    title: 'Окончание работы',
    sortField: 'endDate',
    defaultVisible: true,
    render: (w) => (w.endDate ? formatDate(w.endDate) : 'по наст. время'),
  },
  {
    key: 'organization',
    title: 'Организация',
    sortField: 'person.location.name',
    defaultVisible: true,
    render: (w) => formatText(w.person.location?.name),
  },
  {
    key: 'passport',
    title: 'Паспорт',
    sortField: 'person.passportID',
    defaultVisible: false,
    render: (w) => formatText(w.person.passportID),
  },
  {
    key: 'coordinates',
    title: 'Координаты (X; Y)',
    defaultVisible: false,
    render: (w) => formatPoint(w.coordinates.x, w.coordinates.y),
  },
  {
    key: 'location',
    title: 'Локация (X; Y; Z)',
    defaultVisible: false,
    render: (w) => {
      const location = w.person.location;
      return location ? formatPoint(location.x, location.y, location.z) : '—';
    },
  },
  {
    key: 'creationDate',
    title: 'Дата создания',
    sortField: 'creationDate',
    defaultVisible: false,
    render: (w) => <Tooltip title={formatDateTimeFull(w.creationDate)}>{formatDateTime(w.creationDate)}</Tooltip>,
  },
];

export const DEFAULT_COLUMNS = WORKER_COLUMNS.filter((c) => c.defaultVisible).map((c) => c.key);

interface Props {
  page?: WorkerPage;
  loading?: boolean;
  visibleColumns?: string[];
  sort?: SortItem[];
  onSortChange?: (sort: SortItem[]) => void;
  onPageChange: (page: number, size: number) => void;
  onRowClick?: (worker: Worker) => void;
  actions?: (worker: Worker) => ReactNode;
  empty?: ReactNode;
}

export function WorkersTable({
  page,
  loading,
  visibleColumns = DEFAULT_COLUMNS,
  sort,
  onSortChange,
  onPageChange,
  onRowClick,
  actions,
  empty,
}: Props) {
  const shown = WORKER_COLUMNS.filter((c) => visibleColumns.includes(c.key));
  const sortable = sort !== undefined && onSortChange !== undefined;

  const columns: TableProps<Worker>['columns'] = shown.map((c) => {
    const sorted = sortable && c.sortField ? sort.find((s) => s.field === c.sortField) : undefined;
    const sortOrder: SortOrder = sorted ? (sorted.desc ? 'descend' : 'ascend') : null;
    return {
      key: c.sortField ?? c.key,
      title: c.title,
      align: c.align,
      render: (_, worker) => c.render(worker),
      ...(sortable && c.sortField
        ? { sorter: { multiple: 1 }, sortOrder }
        : {}),
    };
  });
  if (actions) {
    columns.push({ key: 'actions', fixed: 'right', width: 96, render: (_, worker) => actions(worker) });
  }

  const handleChange: TableProps<Worker>['onChange'] = (pagination, _, sorter, extra) => {
    if (extra.action === 'sort' && sortable) {
      const fromHeader = (Array.isArray(sorter) ? sorter : [sorter])
        .filter((s) => s.order && s.columnKey != null)
        .map((s) => ({ field: String(s.columnKey), desc: s.order === 'descend' }));
      const headerFields = shown.flatMap((c) => (c.sortField ? [c.sortField] : []));
      onSortChange(mergeHeaderSort(sort, fromHeader, headerFields));
      return;
    }
    if (extra.action === 'paginate') {
      onPageChange((pagination.current ?? 1) - 1, pagination.pageSize ?? page?.size ?? 10);
    }
  };

  return (
    <Table<Worker>
      rowKey="id"
      size="small"
      scroll={{ x: 'max-content' }}
      showSorterTooltip={false}
      loading={loading}
      columns={columns}
      dataSource={page?.items}
      onChange={handleChange}
      locale={empty ? { emptyText: empty } : undefined}
      onRow={onRowClick ? (worker) => ({ onClick: () => onRowClick(worker), style: { cursor: 'pointer' } }) : undefined}
      pagination={{
        current: (page?.page ?? 0) + 1,
        pageSize: page?.size ?? 10,
        total: page?.totalElements ?? 0,
        showSizeChanger: true,
        pageSizeOptions: [5, 10, 20, 50, 100],
        showTotal: (total) => `Всего: ${total}`,
      }}
    />
  );
}
