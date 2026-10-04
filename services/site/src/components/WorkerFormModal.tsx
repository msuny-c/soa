import { useMutation, useQueryClient } from '@tanstack/react-query';
import { App, Checkbox, Col, DatePicker, Form, Input, InputNumber, Modal, Row, Select } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useState } from 'react';
import { api, POSITION_OPTIONS, ServiceError, WORKER_FIELDS, type Worker, type WorkerWrite } from '../api';
import {
  int32Rule,
  integerRule,
  lengthRule,
  minRule,
  notBlankRule,
  notOnlySpacesRule,
  requiredRule,
} from '../validation';
import { ErrorAlert, type ErrorDetails } from './ErrorAlert';

interface Values extends Omit<WorkerWrite, 'startDate' | 'endDate'> {
  startDate?: Dayjs;
  endDate?: Dayjs | null;
  hasLocation: boolean;
}

const FORM_FIELDS = WORKER_FIELDS.map((f) => f.name).filter((name) => name !== 'id' && name !== 'creationDate');

const FULL = { width: '100%' };

function toValues(worker: Worker | null): Partial<Values> {
  if (!worker) {
    return { hasLocation: false };
  }
  return {
    ...worker,
    startDate: dayjs(worker.startDate),
    endDate: worker.endDate ? dayjs(worker.endDate) : null,
    hasLocation: worker.person.location != null,
  };
}

function toBody({ hasLocation, startDate, endDate, person, ...rest }: Values): WorkerWrite {
  return {
    ...rest,
    startDate: startDate?.format('YYYY-MM-DD') as string,
    endDate: endDate ? endDate.format('YYYY-MM-DD') : null,
    person: {
      passportID: person?.passportID || null,
      location: hasLocation && person?.location ? { ...person.location, name: person.location.name || null } : null,
    },
  };
}

interface Props {
  worker: Worker | null;
  open: boolean;
  onClose: () => void;
}

export function WorkerFormModal({ worker, open, onClose }: Props) {
  const [form] = Form.useForm();
  const hasLocation = Form.useWatch('hasLocation', form);
  const [alert, setAlert] = useState<{ error: unknown; details?: ErrorDetails } | null>(null);
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  const save = useMutation({
    mutationFn: (values: Values) =>
      worker ? api.updateWorker(worker.id, toBody(values)) : api.createWorker(toBody(values)),
    onSuccess: (saved) => {
      message.success(worker ? `Сотрудник «${saved.name}» обновлён` : `Сотрудник «${saved.name}» добавлен`);
      queryClient.invalidateQueries({ queryKey: ['workers'] });
      onClose();
    },
    onError: (error) => {
      if (!(error instanceof ServiceError)) {
        setAlert({ error });
        return;
      }
      if (error.status === 409) {
        form.setFields([{ name: ['person', 'passportID'], errors: ['Этот паспорт уже указан у другого сотрудника'] }]);
        return;
      }
      const inForm = error.details.filter((d) => d.field && FORM_FIELDS.includes(d.field));
      const rest = error.details.filter((d) => !inForm.includes(d));
      form.setFields(inForm.map((d) => ({ name: d.field!.split('.'), errors: [d.issue ?? ''] })));
      if (inForm.length === 0 || rest.length > 0) {
        setAlert({ error, details: rest });
      }
    },
  });

  const submit = () => {
    setAlert(null);
    form.validateFields().then(() => save.mutate(form.getFieldsValue(true)), () => undefined);
  };

  return (
    <Modal
      open={open}
      destroyOnHidden
      width={560}
      title={worker ? `Сотрудник ${worker.name}` : 'Новый сотрудник'}
      okText="Сохранить"
      cancelText="Отмена"
      confirmLoading={save.isPending}
      onOk={submit}
      onCancel={onClose}
      afterOpenChange={(visible) => {
        if (visible) {
          save.reset();
          setAlert(null);
        }
      }}
    >
      <ErrorAlert
        error={alert?.error}
        details={alert?.details}
        messages={{ 404: 'Этот сотрудник уже удалён' }}
      />
      <Form
        form={form}
        layout="vertical"
        validateTrigger="onBlur"
        initialValues={toValues(worker)}
        preserve={false}
        clearOnDestroy
      >
        <Form.Item name="name" label="Имя" rules={[notBlankRule('Введите имя')]}>
          <Input />
        </Form.Item>
        <Row gutter={12}>
          <Col span={12}>
            <Form.Item
              name="salary"
              label="Зарплата"
              rules={[
                requiredRule('Укажите зарплату'),
                integerRule('Зарплата — целое число'),
                minRule(1, 'Зарплата должна быть больше 0'),
              ]}
            >
              <InputNumber style={FULL} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="position" label="Должность" rules={[requiredRule('Выберите должность')]}>
              <Select options={POSITION_OPTIONS} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={12}>
          <Col span={12}>
            <Form.Item name="startDate" label="Начало работы" rules={[requiredRule('Укажите дату начала')]}>
              <DatePicker format="DD.MM.YYYY" placeholder="ДД.ММ.ГГГГ" style={FULL} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="endDate" label="Окончание работы">
              <DatePicker format="DD.MM.YYYY" placeholder="Работает сейчас" style={FULL} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={12}>
          <Col span={6}>
            <Form.Item
              name={['coordinates', 'x']}
              label="Координата X"
              rules={[requiredRule('Укажите X'), integerRule('Целое число'), int32Rule()]}
            >
              <InputNumber style={FULL} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name={['coordinates', 'y']} label="Координата Y" rules={[requiredRule('Укажите Y')]}>
              <InputNumber style={FULL} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name={['person', 'passportID']} label="Паспорт" rules={[lengthRule(7, 23, 'От 7 до 23 символов')]}>
              <Input placeholder="Необязательно" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="hasLocation" valuePropName="checked" style={{ marginBottom: hasLocation ? 14 : 0 }}>
          <Checkbox>Указать местоположение</Checkbox>
        </Form.Item>
        {hasLocation && (
          <Row gutter={12}>
            <Col span={6}>
              <Form.Item name={['person', 'location', 'x']} label="X" rules={[requiredRule('Укажите X')]}>
                <InputNumber style={FULL} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name={['person', 'location', 'y']} label="Y" rules={[requiredRule('Укажите Y')]}>
                <InputNumber style={FULL} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name={['person', 'location', 'z']}
                label="Z"
                rules={[requiredRule('Укажите Z'), integerRule('Целое число')]}
              >
                <InputNumber style={FULL} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name={['person', 'location', 'name']}
                label="Организация"
                rules={[notOnlySpacesRule('Не может состоять из пробелов')]}
              >
                <Input placeholder="Необязательно" />
              </Form.Item>
            </Col>
          </Row>
        )}
      </Form>
    </Modal>
  );
}
