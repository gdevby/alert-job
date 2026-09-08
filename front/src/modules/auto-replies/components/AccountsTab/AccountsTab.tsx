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
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { FormState } from '@/lib/constants/FormState';
import { UserCredentialsApi, type UserSiteCredentialShortResponse } from '@/apis/coreApi';
import { AccountDialog } from '@/modules/auto-replies/components/AccountsTab/AccountDialog';
import { FormattedDate } from '@/components/FormattedDate';
import { CircularProgress, Tooltip } from '@mui/material';

const userCredentialsApi = new UserCredentialsApi();

type Props = {
  index: number;
  data: UserSiteCredentialShortResponse;
  onEditButton: (id: number) => void;
  onRemoveButton: (id: number) => void;
};

const Row = ({ index, data, onEditButton, onRemoveButton }: Props) => {
  const { id, name, createdAt, login, site } = data;

  const {
    mutate,
    isPending,
    data: response,
  } = useMutation({
    mutationFn: (id: number) => userCredentialsApi.validateCredential(id),
  });

  const handleCheckAvailabilityButton = (id: number) => {
    mutate(id);
  };

  return (
    <TableRow key={id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
      <TableCell>{index + 1}</TableCell>
      <TableCell>{name}</TableCell>
      <TableCell>{login}</TableCell>
      <TableCell>{site.name}</TableCell>
      <TableCell>
        <FormattedDate date={createdAt} />
      </TableCell>
      <TableCell style={{ width: 120 }} align="center">
        {isPending && <CircularProgress size={20} />}
        {!isPending && response?.data.success && <CheckCircleIcon color="primary" />}
        {!isPending && response?.data.errorMessage && (
          <Tooltip placement="top" title={response?.data.errorMessage}>
            <ErrorIcon color="error" />
          </Tooltip>
        )}
      </TableCell>
      <TableCell align="right">
        <div style={{ width: '100%' }}>
          <Button variant="outlined" onClick={() => handleCheckAvailabilityButton(id)} disabled={isPending}>
            Проверить доступность
          </Button>
          <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
            <Button variant="outlined" onClick={() => onEditButton(id)}>
              Изменить
            </Button>
            <Button variant="outlined" color="error" onClick={() => onRemoveButton(id)}>
              Удалить
            </Button>
          </div>
        </div>
      </TableCell>
    </TableRow>
  );
};

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
              <TableCell>№</TableCell>
              <TableCell>Название</TableCell>
              <TableCell>Логин</TableCell>
              <TableCell>Сайт</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell>Доступность</TableCell>
              <TableCell width={256} align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.data.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  Нет данных
                </TableCell>
              </TableRow>
            )}
            {data?.data.map((data, index) => (
              <Row
                key={data.id}
                index={index}
                data={data}
                onEditButton={handleEditButton}
                onRemoveButton={handleRemoveButton}
              />
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
