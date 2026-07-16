import './accountDialog.scss';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { Button, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { AutoreplySitesSupportingApi, UserCredentialsApi, type UserCredentialRequest } from '@/apis/coreApi';

type FormValues = Pick<UserCredentialRequest, 'name' | 'login' | 'password' | 'siteId'>;

const userCredentialsApi = new UserCredentialsApi();
const autoreplySitesSupportingApi = new AutoreplySitesSupportingApi();

type Props = {
  isOpen: boolean;
  formState: FormState;
  initialFields?: UserCredentialRequest;
  close: () => void;
};

export const AccountDialog = ({ isOpen, formState, initialFields, close }: Props) => {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    register,
    handleSubmit,
    reset,
  } = useForm<FormValues>();

  const { data: sites, isLoading } = useQuery({
    queryKey: ['autoreplySitesSupportingApi.getSupportedSites'],
    queryFn: () => autoreplySitesSupportingApi.getSupportedSites(),
    placeholderData: data => data,
  });

  const { mutate: createOrUpdateAccount, isPending } = useMutation({
    mutationFn: (data: UserCredentialRequest) => userCredentialsApi.createOrUpdate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userCredentialsApi.getAllUserCredentials'] });
      close();
    },
    onError: () => {},
  });

  useEffect(() => {
    if (isOpen && formState === 'editing' && initialFields) {
      reset(initialFields);
    }
  }, [isOpen, formState]);

  const handleClose = () => {
    close();
    reset({ name: '', login: '', password: '', siteId: undefined });
  };

  const submit = (formValues: FormValues) => {
    createOrUpdateAccount(formValues);
  };

  return (
    <Dialog className="account-dialog" fullWidth maxWidth="sm" open={isOpen} onClose={handleClose}>
      <form noValidate onSubmit={handleSubmit(submit)}>
        <DialogTitle>{formState === 'creating' ? 'Добавление аккаунта' : 'Изменение аккаунта'}</DialogTitle>
        <DialogContent className="account-dialog__content">
          <TextField
            type="text"
            label="Название"
            variant="standard"
            placeholder="Название"
            disabled={formState === 'editing'}
            required
            error={Boolean(errors.name)}
            {...register('name', { required: true })}
          />
          <TextField
            type="text"
            label="Логин"
            variant="standard"
            placeholder="Логин"
            required
            error={Boolean(errors.login)}
            {...register('login', { required: true })}
          />
          <TextField
            type="password"
            label="Пароль"
            variant="standard"
            placeholder="Пароль"
            required
            error={Boolean(errors.password)}
            {...register('password', { required: true })}
          />
          {!isLoading && (
            <FormControl size="small" required error={Boolean(errors.siteId)}>
              <InputLabel>Сайт</InputLabel>
              <Select defaultValue="" label="Сайт" {...register('siteId', { required: true })}>
                {sites?.data.map(({ id, name }) => (
                  <MenuItem key={id} value={id}>
                    {name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          <div className="account-dialog__submit-button">
            <Button type="submit" variant="contained" disabled={isPending}>
              {formState === 'creating' ? 'Добавить' : 'Изменить'}
            </Button>
          </div>
        </DialogContent>
      </form>
    </Dialog>
  );
};
