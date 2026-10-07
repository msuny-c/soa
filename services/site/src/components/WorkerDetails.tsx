import { Descriptions } from 'antd';
import type { Worker } from '../api';
import { formatDate, formatDateTimeFull, formatPoint, formatPosition, formatSalary, formatText } from '../format';

export function WorkerDetails({ worker }: { worker: Worker }) {
  const location = worker.person.location;
  return (
    <Descriptions bordered column={1} size="small">
      <Descriptions.Item label="ID">{worker.id}</Descriptions.Item>
      <Descriptions.Item label="Имя">{worker.name}</Descriptions.Item>
      <Descriptions.Item label="Должность">{formatPosition(worker.position)}</Descriptions.Item>
      <Descriptions.Item label="Зарплата">{formatSalary(worker.salary)}</Descriptions.Item>
      <Descriptions.Item label="Период работы">
        {formatDate(worker.startDate)} — {worker.endDate ? formatDate(worker.endDate) : 'по настоящее время'}
      </Descriptions.Item>
      <Descriptions.Item label="Координаты (X; Y)">{formatPoint(worker.coordinates.x, worker.coordinates.y)}</Descriptions.Item>
      <Descriptions.Item label="Паспорт">{formatText(worker.person.passportID)}</Descriptions.Item>
      <Descriptions.Item label="Организация">{formatText(location?.name)}</Descriptions.Item>
      <Descriptions.Item label="Локация (X; Y; Z)">
        {location ? formatPoint(location.x, location.y, location.z) : 'не указана'}
      </Descriptions.Item>
      <Descriptions.Item label="Запись создана">{formatDateTimeFull(worker.creationDate)}</Descriptions.Item>
    </Descriptions>
  );
}
