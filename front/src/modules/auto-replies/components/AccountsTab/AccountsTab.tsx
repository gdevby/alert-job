import './accountsTab.scss';

import { useState } from 'react';
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
import { UserCredentialsApi } from '@/apis/coreApi';
import { AccountDialog } from '@/modules/auto-replies/components/AccountsTab/AccountDialog';

const userCredentialsApi = new UserCredentialsApi();

export const AccountsTab = () => {
  const queryClient = useQueryClient();
  const [isModalShown, setIsModalShown] = useState(false);
  const [accountIdForEditing, setAccountIdForEditing] = useState<number>();
  const [formState, setFormState] = useState<FormState>(FormState.Creating);

  const { data } = useQuery({
    queryKey: ['userCredentialsApi.getAllUserCredentials'],
    queryFn: () => userCredentialsApi.getAllUserCredentials(),
    placeholderData: data => data,
  });

  const { mutate: removeAccount } = useMutation({
    mutationFn: (id: number) => userCredentialsApi.deleteCredential(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userCredentialsApi.getAllUserCredentials'] });
    },
    onError: () => {},
  });

  const handleCreateAccountButton = () => {
    setAccountIdForEditing(undefined);
    setFormState('creating');
    setIsModalShown(true);
  };

  const handleEditButton = (id: number) => {
    setAccountIdForEditing(id);
    setFormState('editing');
    setIsModalShown(true);
  };

  const handleRemoveButton = (id: number) => {
    removeAccount(id);
  };

  const initialFields = data?.data.find(({ id }) => id === accountIdForEditing);

  return (
    <div className="accounts-tab">
      <Button onClick={handleCreateAccountButton}>Добавить аккаунт</Button>
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }}>
          <TableHead>
            <TableRow>
              <TableCell>Название</TableCell>
              <TableCell>Логин</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.data?.map(({ id, name, createdAt, login }) => (
              <TableRow key={id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                <TableCell>{name}</TableCell>
                <TableCell>{login}</TableCell>
                <TableCell>{createdAt}</TableCell>
                <TableCell align="right">
                  {/* TODO remove {' '} */}
                  <Button variant="outlined" onClick={() => handleEditButton(id)}>
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

      <AccountDialog
        isOpen={isModalShown}
        formState={formState}
        initialFields={initialFields}
        close={() => setIsModalShown(false)}
      />
    </div>
  );
};
