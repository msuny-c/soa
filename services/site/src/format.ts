import dayjs from 'dayjs';
import { POSITION_LABELS, type Position } from './api';

const integer = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 });
const decimal = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 });

export const formatSalary = (value?: number | null) => (value == null ? '—' : integer.format(value));
export const formatAverage = (value?: number | null) => (value == null ? '—' : decimal.format(value));
export const formatDelta = (value: number) => `${value > 0 ? '+' : ''}${integer.format(value)}`;
export const formatDate = (value?: string | null) => (value ? dayjs(value).format('DD.MM.YYYY') : '—');
export const formatDateTime = (value?: string | null) => (value ? dayjs(value).format('D MMM YYYY, HH:mm') : '—');
export const formatDateTimeFull = (value?: string | null) => (value ? dayjs(value).format('DD.MM.YYYY HH:mm:ss') : '—');
export const formatPosition = (value: Position) => POSITION_LABELS[value];
export const formatText = (value?: string | number | null) => (value == null || value === '' ? '—' : String(value));
export const formatPoint = (...coordinates: (number | null | undefined)[]) =>
  `(${coordinates.map((c) => (c == null ? '—' : String(c))).join('; ')})`;

const pluralRules = new Intl.PluralRules('ru-RU');

export function plural(count: number, [one, few, many]: [string, string, string]) {
  const category = pluralRules.select(count);
  return category === 'one' ? one : category === 'few' ? few : many;
}

export const WORKERS_FORMS: [string, string, string] = ['сотрудник', 'сотрудника', 'сотрудников'];
