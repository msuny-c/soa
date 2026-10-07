import { DeleteOutlined, EditOutlined, MoreOutlined } from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { App, Button, Drawer, Dropdown, Space, Tooltip } from 'antd';
import { useState } from 'react';
import { api, type Worker } from '../api';
import { ErrorAlert } from './ErrorAlert';
import { WorkerDetails } from './WorkerDetails';
import { WorkerFormModal } from './WorkerFormModal';

export function useWorkerActions() {
  const [editing, setEditing] = useState<{ worker: Worker | null } | null>(null);
  const [selected, setSelected] = useState<Worker | null>(null);
  const queryClient = useQueryClient();
  const { message, modal } = App.useApp();

  const remove = useMutation({
    mutationFn: (worker: Worker) => api.deleteWorker(worker.id),
    onSuccess: (_, worker) => {
      message.success(`Сотрудник «${worker.name}» удалён`);
      setSelected(null);
      queryClient.invalidateQueries({ queryKey: ['workers'] });
    },
  });

  const openEditor = (worker: Worker | null) => {
    setSelected(null);
    setEditing({ worker });
  };

  const confirmDelete = (worker: Worker) =>
    modal.confirm({
      title: `Удалить «${worker.name}»?`,
      content: 'Сотрудник будет удалён без возможности восстановления.',
      okText: 'Удалить',
      okButtonProps: { danger: true },
      cancelText: 'Отмена',
      onOk: () => remove.mutateAsync(worker).catch(() => undefined),
    });

  const renderActions = (worker: Worker) => (
    <Space size={0} onClick={(event) => event.stopPropagation()}>
      <Tooltip title="Изменить">
        <Button type="text" aria-label="Изменить" icon={<EditOutlined />} onClick={() => openEditor(worker)} />
      </Tooltip>
      <Dropdown
        trigger={['click']}
        menu={{
          items: [{ key: 'delete', label: 'Удалить', danger: true, icon: <DeleteOutlined /> }],
          onClick: () => confirmDelete(worker),
        }}
      >
        <Button type="text" aria-label="Ещё" icon={<MoreOutlined />} />
      </Dropdown>
    </Space>
  );

  const elements = (
    <>
      <ErrorAlert error={remove.error} onClose={remove.reset} messages={{ 404: 'Этот сотрудник уже удалён' }} />
      <WorkerFormModal open={editing !== null} worker={editing?.worker ?? null} onClose={() => setEditing(null)} />
      <Drawer
        width={520}
        open={selected !== null}
        title="Сотрудник"
        onClose={() => setSelected(null)}
        extra={
          selected && (
            <Space>
              <Button icon={<EditOutlined />} onClick={() => openEditor(selected)}>
                Изменить
              </Button>
              <Button danger icon={<DeleteOutlined />} onClick={() => confirmDelete(selected)}>
                Удалить
              </Button>
            </Space>
          )
        }
      >
        {selected && <WorkerDetails worker={selected} />}
      </Drawer>
    </>
  );

  return { openEditor, openDetails: setSelected, renderActions, elements };
}
