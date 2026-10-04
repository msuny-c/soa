import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Form, Input, InputNumber, Table } from 'antd';
import type { Rule } from 'antd/es/form';
import type { ReactNode } from 'react';
import { api, type IndexationResult } from '../api';
import { ErrorAlert, type ErrorMessages } from '../components/ErrorAlert';
import { formatDelta, formatSalary, plural, WORKERS_FORMS } from '../format';
import { int32Rule, integerRule, minRule, notBlankRule, positiveRule, requiredRule } from '../validation';

const COLUMNS = [
  { key: 'id', title: 'ID сотрудника', dataIndex: 'workerID' },
  { key: 'old', title: 'Было', align: 'right' as const, render: (_: unknown, r: IndexationResult) => formatSalary(r.oldSalary) },
  { key: 'new', title: 'Стало', align: 'right' as const, render: (_: unknown, r: IndexationResult) => formatSalary(r.newSalary) },
  {
    key: 'diff',
    title: 'Изменение',
    align: 'right' as const,
    render: (_: unknown, r: IndexationResult) => formatDelta(r.newSalary - r.oldSalary),
  },
];

function summary(results: IndexationResult[]) {
  const total = results.reduce((sum, r) => sum + (r.newSalary - r.oldSalary), 0);
  return `Проиндексировано: ${results.length} ${plural(results.length, WORKERS_FORMS)}, суммарное изменение ${formatDelta(total)}`;
}

const COEFF_RULES: Rule[] = [requiredRule('Укажите коэффициент'), positiveRule('Коэффициент должен быть больше 0')];

const UNAVAILABLE = 'Сервис сотрудников сейчас недоступен. Попробуйте позже';

function IndexationCard<T extends { target: number | string; coeff: number }>({
  title,
  target,
  messages,
  run,
}: {
  title: string;
  target: { label: string; rules: Rule[]; input: ReactNode };
  messages: ErrorMessages;
  run: (values: T) => Promise<IndexationResult[]>;
}) {
  const [form] = Form.useForm<T>();
  const queryClient = useQueryClient();
  const indexation = useMutation({
    mutationFn: run,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workers'] }),
  });
  return (
    <Card size="small" title={title} style={{ marginBottom: 16 }}>
      <Form<T>
        form={form}
        layout="inline"
        validateTrigger="onBlur"
        initialValues={{ coeff: 1.1 } as Partial<T>}
        onFinish={(values) => indexation.mutate(values)}
      >
        <Form.Item name="target" label={target.label} rules={target.rules}>
          {target.input}
        </Form.Item>
        <Form.Item name="coeff" label="Коэффициент" rules={COEFF_RULES}>
          <InputNumber step={0.05} />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={indexation.isPending}>
          Проиндексировать
        </Button>
      </Form>
      {(indexation.error || indexation.data) && (
        <div style={{ marginTop: 16 }}>
          <ErrorAlert error={indexation.error} onClose={indexation.reset} messages={messages} />
          {indexation.data && (
            <>
              <Alert type="success" showIcon style={{ marginBottom: 12 }} message={summary(indexation.data)} />
              <Table rowKey="workerID" size="small" columns={COLUMNS} dataSource={indexation.data} pagination={false} />
            </>
          )}
        </div>
      )}
    </Card>
  );
}

export function HrPage() {
  return (
    <>
      <IndexationCard<{ target: number; coeff: number }>
        title="Индексация зарплаты сотрудника"
        target={{
          label: 'ID сотрудника',
          rules: [
            requiredRule('Укажите ID сотрудника'),
            integerRule('ID — целое число'),
            minRule(1, 'ID должен быть больше 0'),
            int32Rule('Слишком большой ID'),
          ],
          input: <InputNumber />,
        }}
        messages={{
          404: 'Сотрудник с таким ID не найден',
          422: 'Новая зарплата выходит за допустимые пределы',
          502: UNAVAILABLE,
        }}
        run={async ({ target, coeff }) => [await api.indexWorker(target, coeff)]}
      />
      <IndexationCard<{ target: string; coeff: number }>
        title="Индексация зарплат организации"
        target={{
          label: 'Организация',
          rules: [notBlankRule('Укажите организацию')],
          input: <Input />,
        }}
        messages={{
          404: 'В этой организации нет сотрудников',
          422: 'Для части сотрудников новая зарплата выходит за допустимые пределы. Зарплаты не изменены',
          502: UNAVAILABLE,
        }}
        run={async ({ target, coeff }) => (await api.indexOrganization(target, coeff)).results}
      />
    </>
  );
}
