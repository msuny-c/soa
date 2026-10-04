import { Button, DatePicker, Flex, Form, Input, InputNumber, Select } from 'antd';
import type { Dayjs } from 'dayjs';
import { POSITION_OPTIONS, WORKER_FIELDS, type FieldType } from '../api';
import { fieldOf, needsValue, opsFor, type FilterOp, type FilterRow } from '../filters';
import { integerRule, requiredRule } from '../validation';

const FIELD_OPTIONS = WORKER_FIELDS.map((f) => ({ value: f.name, label: f.label }));
const ITEM_STYLE = { marginBottom: 12 };

type ValueInputProps = { type?: FieldType; value?: unknown; onChange?: (value: unknown) => void };

function ValueInput({ type, value, onChange }: ValueInputProps) {
  const style = { width: '100%' };
  switch (type) {
    case 'position':
      return <Select value={value} onChange={onChange} style={style} options={POSITION_OPTIONS} placeholder="Должность" />;
    case 'number':
      return <InputNumber value={value as number | null} onChange={onChange} style={style} placeholder="Число" />;
    case 'date':
      return <DatePicker value={value as Dayjs | null} onChange={onChange} style={style} format="DD.MM.YYYY" />;
    case 'datetime':
      return <DatePicker value={value as Dayjs | null} onChange={onChange} style={style} showTime format="DD.MM.YYYY HH:mm" />;
    default:
      return <Input value={value as string} onChange={(e) => onChange?.(e.target.value)} style={style} placeholder="Текст" />;
  }
}

interface Props {
  initial?: FilterRow;
  submitText: string;
  onSubmit: (row: FilterRow) => void;
  onCancel: () => void;
}

export function FilterEditor({ initial, submitText, onSubmit, onCancel }: Props) {
  const [form] = Form.useForm<FilterRow>();
  const field: string | undefined = Form.useWatch('field', form);
  const op: FilterOp | undefined = Form.useWatch('op', form);
  const meta = fieldOf(field ?? initial?.field);
  const type = meta?.type;
  const valueRules =
    type === 'string' ? [] : [requiredRule('Укажите значение'), ...(meta?.integer ? [integerRule('Введите целое число')] : [])];

  return (
    <Form<FilterRow>
      form={form}
      layout="vertical"
      validateTrigger="onBlur"
      style={{ width: 280 }}
      initialValues={initial ?? { field: 'name', op: 'substr' }}
      onFinish={(row) => onSubmit({ field: row.field, op: row.op, value: needsValue(row.op) ? row.value : undefined })}
    >
      <Form.Item name="field" label="Поле" style={ITEM_STYLE}>
        <Select
          showSearch
          optionFilterProp="label"
          options={FIELD_OPTIONS}
          onChange={(next: string) => form.setFieldsValue({ op: opsFor(fieldOf(next)?.type)[0].value, value: undefined })}
        />
      </Form.Item>
      <Form.Item name="op" label="Условие" style={ITEM_STYLE}>
        <Select options={opsFor(type)} />
      </Form.Item>
      {needsValue(op ?? initial?.op) && (
        <Form.Item name="value" label="Значение" style={ITEM_STYLE} rules={valueRules}>
          <ValueInput type={type} />
        </Form.Item>
      )}
      <Flex justify="flex-end" gap={8}>
        <Button size="small" onClick={onCancel}>
          Отмена
        </Button>
        <Button size="small" type="primary" htmlType="submit">
          {submitText}
        </Button>
      </Flex>
    </Form>
  );
}
