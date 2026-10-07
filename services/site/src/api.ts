import createClient, { serializeArrayParam, serializePrimitiveParam, type Middleware } from 'openapi-fetch';
import type { components as HrComponents, paths as HrPaths } from './generated/hr-service';
import type { components as WorkerComponents, paths as WorkerPaths } from './generated/worker-service';

type WorkerSchemas = WorkerComponents['schemas'];

export type Worker = WorkerSchemas['Worker'];
export type WorkerWrite = WorkerSchemas['WorkerWrite'];
export type WorkerPage = WorkerSchemas['WorkerPage'];
export type Position = WorkerSchemas['Position'];
export type ErrorBody = WorkerSchemas['Error'];
export type IndexationResult = HrComponents['schemas']['IndexationResult'];

export class ServiceError extends Error {
  constructor(
    readonly status: number,
    readonly details: NonNullable<ErrorBody['details']>,
  ) {
    super(`HTTP ${status}`);
  }
}

const throwOnError: Middleware = {
  async onResponse({ response }) {
    if (!response.ok) {
      const body: Partial<ErrorBody> = await response.clone().json().catch(() => ({}));
      throw new ServiceError(response.status, body.details ?? []);
    }
  },
};

const workers = createClient<WorkerPaths>({ baseUrl: import.meta.env.VITE_WORKER_API });
workers.use(throwOnError);
const hr = createClient<HrPaths>({ baseUrl: import.meta.env.VITE_HR_API });
hr.use(throwOnError);

async function data<T>(request: Promise<{ data?: T }>): Promise<T> {
  return (await request).data as T;
}

type ListQuery = NonNullable<WorkerPaths['/workers']['get']['parameters']['query']>;

export type ListParams = Required<ListQuery>;

const listQuery = ({ sort = [], filter = [], page, size }: ListQuery) =>
  [
    sort.length > 0 && serializeArrayParam('sort', sort, { style: 'form', explode: false }),
    filter.length > 0 && serializeArrayParam('filter', filter, { style: 'form', explode: true }),
    page != null && serializePrimitiveParam('page', String(page)),
    size != null && serializePrimitiveParam('size', String(size)),
  ]
    .filter(Boolean)
    .join('&');

export const api = {
  listWorkers: (query: ListParams) => data(workers.GET('/workers', { params: { query }, querySerializer: listQuery })),
  getWorker: (id: number) => data(workers.GET('/workers/{id}', { params: { path: { id } } })),
  createWorker: (body: WorkerWrite) => data(workers.POST('/workers', { body })),
  updateWorker: (id: number, body: WorkerWrite) => data(workers.PUT('/workers/{id}', { params: { path: { id } }, body })),
  deleteWorker: (id: number) => data(workers.DELETE('/workers/{id}', { params: { path: { id } } })),
  averageSalary: () => data(workers.GET('/workers/salary/average')),
  deleteOneBySalary: (salary: number) =>
    data(workers.DELETE('/workers/salary/value/{salary}', { params: { path: { salary } } })),
  searchByName: (substring: string, page: number, size: number) =>
    data(workers.GET('/workers/search/name/{substring}', { params: { path: { substring }, query: { page, size } } })),
  indexWorker: (workerId: number, coeff: number) =>
    data(hr.POST('/index/{worker-id}/{coeff}', { params: { path: { 'worker-id': workerId, coeff } } })),
  indexOrganization: (orgId: string, coeff: number) =>
    data(hr.POST('/index/all/{org-id}/{coeff}', { params: { path: { 'org-id': orgId, coeff } } })),
};

export const POSITION_LABELS: Record<Position, string> = {
  DEVELOPER: 'Разработчик',
  LEAD_DEVELOPER: 'Ведущий разработчик',
  BAKER: 'Пекарь',
};

export const POSITION_OPTIONS = Object.entries(POSITION_LABELS).map(([value, label]) => ({ value, label }));

export type FieldType = 'number' | 'string' | 'date' | 'datetime' | 'position';

export const WORKER_FIELDS: { name: string; label: string; type: FieldType; integer?: boolean }[] = [
  { name: 'id', label: 'ID', type: 'number', integer: true },
  { name: 'name', label: 'Имя', type: 'string' },
  { name: 'coordinates.x', label: 'Координата X', type: 'number', integer: true },
  { name: 'coordinates.y', label: 'Координата Y', type: 'number' },
  { name: 'creationDate', label: 'Дата создания', type: 'datetime' },
  { name: 'salary', label: 'Зарплата', type: 'number', integer: true },
  { name: 'startDate', label: 'Начало работы', type: 'date' },
  { name: 'endDate', label: 'Окончание работы', type: 'date' },
  { name: 'position', label: 'Должность', type: 'position' },
  { name: 'person.passportID', label: 'Паспорт', type: 'string' },
  { name: 'person.location.x', label: 'Локация X', type: 'number' },
  { name: 'person.location.y', label: 'Локация Y', type: 'number' },
  { name: 'person.location.z', label: 'Локация Z', type: 'number', integer: true },
  { name: 'person.location.name', label: 'Организация', type: 'string' },
];
