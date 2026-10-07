import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { App, Button, Card, Empty, Form, Input, InputNumber, Statistic, Tooltip, Typography } from 'antd';
import { useState } from 'react';
import { api } from '../api';
import { ErrorAlert } from '../components/ErrorAlert';
import { useWorkerActions } from '../components/WorkerActions';
import { WorkerDetails } from '../components/WorkerDetails';
import { WorkersTable } from '../components/WorkersTable';
import { formatAverage, formatSalary, plural } from '../format';
import { integerRule, minRule, notBlankRule, requiredRule } from '../validation';

function AverageSalaryCard() {
  const average = useQuery({ queryKey: ['workers', 'average'], queryFn: api.averageSalary });
  return (
    <Card
      size="small"
      title="Средняя зарплата"
      style={{ marginBottom: 16 }}
      extra={
        <Tooltip title="Пересчитать">
          <Button
            type="text"
            aria-label="Пересчитать"
            icon={<ReloadOutlined spin={average.isFetching} />}
            onClick={() => average.refetch()}
          />
        </Tooltip>
      }
    >
      <ErrorAlert error={average.error} messages={{ 500: 'Не удалось посчитать среднюю зарплату' }} />
      {average.data && (
        <Statistic
          title={
            average.data.count > 0
              ? `По ${average.data.count} ${plural(average.data.count, ['сотруднику', 'сотрудникам', 'сотрудникам'])}`
              : 'Нет сотрудников для расчёта'
          }
          value={average.data.average == null ? 'Коллекция пуста' : formatAverage(average.data.average)}
        />
      )}
    </Card>
  );
}

function DeleteBySalaryCard() {
  const queryClient = useQueryClient();
  const { message, modal } = App.useApp();
  const remove = useMutation({
    mutationFn: api.deleteOneBySalary,
    onSuccess: (worker) => {
      message.success(`Удалён сотрудник «${worker.name}»`);
      queryClient.invalidateQueries({ queryKey: ['workers'] });
    },
  });

  const confirm = ({ salary }: { salary: number }) =>
    modal.confirm({
      title: 'Удалить одного сотрудника?',
      content: `Будет удалён один любой сотрудник с зарплатой ${formatSalary(salary)}. Действие нельзя отменить.`,
      okText: 'Удалить',
      okButtonProps: { danger: true },
      cancelText: 'Отмена',
      onOk: () => remove.mutateAsync(salary).catch(() => undefined),
    });

  return (
    <Card size="small" title="Удалить одного сотрудника с заданной зарплатой" style={{ marginBottom: 16 }}>
      <Form layout="inline" validateTrigger="onBlur" onFinish={confirm}>
        <Form.Item
          name="salary"
          rules={[
            requiredRule('Укажите зарплату'),
            integerRule('Зарплата — целое число'),
            minRule(1, 'Зарплата должна быть больше 0'),
          ]}
        >
          <InputNumber placeholder="Зарплата" style={{ width: 200 }} onChange={() => remove.reset()} />
        </Form.Item>
        <Button danger htmlType="submit" loading={remove.isPending}>
          Удалить
        </Button>
      </Form>
      {(remove.error || remove.data) && (
        <div style={{ marginTop: 16 }}>
          <ErrorAlert
            error={remove.error}
            onClose={remove.reset}
            messages={{ 404: 'Сотрудников с такой зарплатой нет' }}
          />
          {remove.data && (
            <>
              <Typography.Text type="secondary">Удалённый сотрудник</Typography.Text>
              <div style={{ marginTop: 8 }}>
                <WorkerDetails worker={remove.data} />
              </div>
            </>
          )}
        </div>
      )}
    </Card>
  );
}

function SearchByNameCard() {
  const [search, setSearch] = useState<{ substring: string; page: number; size: number } | null>(null);
  const actions = useWorkerActions();
  const result = useQuery({
    queryKey: ['workers', 'search', search],
    queryFn: () => api.searchByName(search!.substring, search!.page, search!.size),
    enabled: search !== null,
  });
  return (
    <Card size="small" title="Поиск по подстроке в имени">
      <Form
        layout="inline"
        validateTrigger="onBlur"
        onFinish={({ substring }: { substring: string }) => setSearch({ substring, page: 0, size: 10 })}
        style={{ marginBottom: search || result.error ? 16 : 0 }}
      >
        <Form.Item name="substring" rules={[notBlankRule('Введите часть имени')]}>
          <Input placeholder="Часть имени" style={{ width: 280 }} />
        </Form.Item>
        <Button type="primary" htmlType="submit">
          Найти
        </Button>
      </Form>
      <ErrorAlert error={result.error} messages={{ 400: 'Введите часть имени' }} />
      {actions.elements}
      {search && (
        <WorkersTable
          page={result.data}
          loading={result.isFetching}
          onPageChange={(page, size) => setSearch({ ...search, page, size })}
          onRowClick={actions.openDetails}
          actions={actions.renderActions}
          empty={
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`Нет сотрудников, в имени которых есть «${search.substring}»`} />
          }
        />
      )}
    </Card>
  );
}

export function OperationsPage() {
  return (
    <>
      <AverageSalaryCard />
      <DeleteBySalaryCard />
      <SearchByNameCard />
    </>
  );
}
