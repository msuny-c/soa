export interface SortItem {
  field: string;
  desc: boolean;
}

export const toSortParam = (sort: SortItem[]) => sort.map(({ field, desc }) => `${desc ? '-' : ''}${field}`);

export function mergeHeaderSort(current: SortItem[], fromHeader: SortItem[], headerFields: string[]): SortItem[] {
  const next = new Map(fromHeader.map((item) => [item.field, item]));
  const kept = current
    .filter((item) => !headerFields.includes(item.field) || next.has(item.field))
    .map((item) => next.get(item.field) ?? item);
  const added = fromHeader.filter((item) => !current.some(({ field }) => field === item.field));
  return [...kept, ...added];
}
