import { Alert } from 'antd';
import { ServiceError, type ErrorBody } from '../api';

export type ErrorMessages = Partial<Record<number, string>>;
export type ErrorDetails = NonNullable<ErrorBody['details']>;

const DEFAULT_MESSAGES: ErrorMessages = {
  400: 'Проверьте введённые данные',
  404: 'Ничего не найдено',
  409: 'Такие данные уже есть',
  422: 'Проверьте введённые данные',
  502: 'Сервис временно недоступен. Попробуйте позже',
};

function errorTitle(error: unknown, messages: ErrorMessages = {}): string {
  if (!(error instanceof ServiceError)) {
    return 'Нет связи с сервером';
  }
  return (
    messages[error.status] ??
    DEFAULT_MESSAGES[error.status] ??
    (error.status >= 500 ? 'Что-то пошло не так. Попробуйте ещё раз' : 'Не удалось выполнить действие')
  );
}

interface Props {
  error: unknown;
  messages?: ErrorMessages;
  details?: ErrorDetails;
  onClose?: () => void;
}

export function ErrorAlert({ error, messages, details, onClose }: Props) {
  if (!error) {
    return null;
  }
  const network = !(error instanceof ServiceError);
  const items = (details ?? []).filter((d) => d.issue);
  return (
    <Alert
      type="error"
      showIcon
      closable={onClose !== undefined}
      onClose={onClose}
      style={{ marginBottom: 16 }}
      message={errorTitle(error, messages)}
      description={
        network ? (
          'Проверьте подключение и попробуйте ещё раз. Если открываете приложение впервые, откройте адрес сервиса в браузере и подтвердите сертификат.'
        ) : items.length > 0 ? (
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {items.map((d, i) => (
              <li key={i}>{d.issue}</li>
            ))}
          </ul>
        ) : undefined
      }
    />
  );
}
