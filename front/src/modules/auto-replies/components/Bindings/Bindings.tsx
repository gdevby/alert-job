import './bindings.scss';

import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Button from '@mui/material/Button';
import { FormState } from '@/lib/constants/FormState';
import { AccountTemplateBindingsApi } from '@/apis/coreApi';
import { BindingDialog } from '@/modules/auto-replies/components/Bindings/BindingDialog';
import { Switch } from '@mui/material';

const accountTemplateBindingsApi = new AccountTemplateBindingsApi();

export const Bindings = () => {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [isModalShown, setIsModalShown] = useState(false);
  const [bindingIdForEditing, setBindingIdForEditing] = useState<number>();
  const [formState, setFormState] = useState<FormState>(FormState.Creating);

  const { data } = useQuery({
    queryKey: ['accountTemplateBindingsApi.getAllBindingsForUser'],
    queryFn: () => accountTemplateBindingsApi.getAllBindingsForUser(Number(id)),
    placeholderData: data => data,
  });

  const { mutate: removeBinding } = useMutation({
    mutationFn: (id: number) => accountTemplateBindingsApi._delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountTemplateBindingsApi.getAllBindingsForUser'] });
    },
    onError: () => {},
  });

  const { mutate: changeActive } = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => accountTemplateBindingsApi.setActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountTemplateBindingsApi.getAllBindingsForUser'] });
    },
    onError: () => {},
  });

  const handleActiveSwitchChange = (id: number, active: boolean) => {
    changeActive({ id, active });
  };

  const handleCreateBindingButton = () => {
    setBindingIdForEditing(undefined);
    setFormState('creating');
    setIsModalShown(true);
  };

  const handleEditButton = (id: number) => {
    setBindingIdForEditing(id);
    setFormState('editing');
    setIsModalShown(true);
  };

  const handleRemoveButton = (id: number) => {
    removeBinding(id);
  };

  const initialFields = data?.data.find(({ id }) => id === bindingIdForEditing);

  return (
    <div className="bindings">
      <Button variant="contained" onClick={handleCreateBindingButton}>
        Добавить связку
      </Button>
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }}>
          <TableHead>
            <TableRow>
              <TableCell>Аккаунт</TableCell>
              <TableCell>Шаблон</TableCell>
              <TableCell>Промпт</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell>Активен</TableCell>
              <TableCell align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.data.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  Нет данных
                </TableCell>
              </TableRow>
            )}
            {data?.data.map(({ id, accountName, templateName, promtName, createdAt, active }) => (
              <TableRow key={id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                <TableCell>{accountName}</TableCell>
                <TableCell>{templateName}</TableCell>
                <TableCell>{promtName}</TableCell>
                <TableCell>{createdAt}</TableCell>
                <TableCell>
                  <Switch checked={active} onChange={event => handleActiveSwitchChange(id, event.target.checked)} />
                </TableCell>
                <TableCell align="right">
                  {/* TODO remove {' '} */}
                  <Button variant="outlined" disabled onClick={() => handleEditButton(id)}>
                    Изменить
                  </Button>{' '}
                  <Button variant="outlined" color="error" onClick={() => handleRemoveButton(id)}>
                    Удалить
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <BindingDialog
        isOpen={isModalShown}
        formState={formState}
        initialFields={initialFields}
        moduleId={Number(id)}
        close={() => setIsModalShown(false)}
      />
    </div>
  );
};
