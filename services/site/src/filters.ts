import dayjs, { type Dayjs } from 'dayjs';
import { POSITION_LABELS, WORKER_FIELDS, type FieldType, type Position } from './api';

export type FilterOp = 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'substr' | 'isNull' | 'notNull';

export interface FilterRow {
  field: string;
  op: FilterOp;
  value?: string | number | Dayjs | null;
}

export const FILTER_OPS: { value: FilterOp; label: string }[] = [
  { value: 'eq', label: '=' },
  { value: 'ne', label: '≠' },
  { value: 'gt', label: '>' },
  { value: 'gte', label: '≥' },
  { value: 'lt', label: '<' },
  { value: 'lte', label: '≤' },
  { value: 'substr', label: 'содержит' },
  { value: 'isNull', label: 'не заполнено' },
  { value: 'notNull', label: 'заполнено' },
];

export const fieldOf = (name?: string) => WORKER_FIELDS.find((f) => f.name === name);

export function opsFor(type?: FieldType) {
  if (type === 'string') {
    return FILTER_OPS;
  }
  if (type === 'position') {
    return FILTER_OPS.filter(({ value }) => ['eq', 'ne', 'isNull', 'notNull'].includes(value));
  }
  return FILTER_OPS.filter(({ value }) => value !== 'substr');
}

export const needsValue = (op?: FilterOp) => op !== 'isNull' && op !== 'notNull';

function serialize(type: FieldType | undefined, value: unknown): string {
  if (value == null) {
    return '';
  }
  if (dayjs.isDayjs(value)) {
    return type === 'datetime' ? value.toISOString() : value.format('YYYY-MM-DD');
  }
  return String(value);
}

export function toQuery({ field, op, value }: FilterRow): string {
  if (!needsValue(op)) {
    return `${field}[null]=${op === 'isNull'}`;
  }
  return `${field}[${op}]=${serialize(fieldOf(field)?.type, value)}`;
}

function display(type: FieldType | undefined, value: unknown): string {
  if (value == null || value === '') {
    return '«»';
  }
  if (dayjs.isDayjs(value)) {
    return value.format(type === 'datetime' ? 'D MMM YYYY, HH:mm' : 'DD.MM.YYYY');
  }
  if (type === 'position') {
    return POSITION_LABELS[value as Position] ?? String(value);
  }
  return typeof value === 'number' ? value.toLocaleString('ru-RU', { maximumFractionDigits: 20 }) : `«${String(value)}»`;
}

export function describe({ field, op, value }: FilterRow): string {
  const meta = fieldOf(field);
  const label = meta?.label ?? field;
  const opLabel = FILTER_OPS.find((o) => o.value === op)?.label ?? op;
  return needsValue(op) ? `${label} ${opLabel} ${display(meta?.type, value)}` : `${label}: ${opLabel}`;
}
