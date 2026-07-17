import './bindingDialog.scss';

import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, FormControl, FormHelperText, InputLabel, MenuItem, Select } from '@mui/material';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { AccountTemplateBindingsApi, UserCredentialsApi, type BindingCreateRequest } from '@/apis/coreApi';
import { PromptsApi, TemplatesApi } from '@/apis/llmApi';

type FormValues = Pick<BindingCreateRequest, 'accountId' | 'templateId' | 'promtId' | 'active' | 'moduleId'>;

const accountTemplateBindingsApi = new AccountTemplateBindingsApi();
const userCredentialsApi = new UserCredentialsApi();
const templatesApi = new TemplatesApi();
const promptsApi = new PromptsApi();

type Props = {
  isOpen: boolean;
  formState: FormState;
  initialFields?: BindingCreateRequest;
  moduleId: number;
  close: () => void;
};

export const BindingDialog = ({ isOpen, formState, initialFields, moduleId, close }: Props) => {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    register,
    handleSubmit,
    reset,
  } = useForm<FormValues>({ defaultValues: { moduleId, active: false } });

  const { data: accounts, isLoading: isAccountsLoading } = useQuery({
    queryKey: ['userCredentialsApi.getAllUserCredentials'],
    queryFn: () => userCredentialsApi.getAllUserCredentials(),
    placeholderData: data => data,
  });

  const { data: templates, isLoading: isTemplatesLoading } = useQuery({
    queryKey: ['templatesApi.getTemplatesByUser'],
    queryFn: () => templatesApi.getTemplatesByUser(),
    placeholderData: data => data,
  });

  const { data: prompts, isLoading: isPromptsLoading } = useQuery({
    queryKey: ['promptsApi.getPromptsByUser'],
    queryFn: () => promptsApi.getPromptsByUser(),
    placeholderData: data => data,
  });

  const { mutate: createOrUpdateTemplate, isPending } = useMutation({
    mutationFn: (data: BindingCreateRequest) => accountTemplateBindingsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountTemplateBindingsApi.getAllBindingsForUser'] });
      close();
    },
    onError: () => {},
  });

  useEffect(() => {
    if (isOpen && formState === 'editing' && initialFields) {
      console.log('initialFields', initialFields);
      reset(initialFields);
    }
  }, [isOpen, formState]);

  const handleClose = () => {
    close();
    reset({
      accountId: undefined,
      templateId: undefined,
      promtId: undefined,
      active: false,
    });
  };

  const submit = (formValues: FormValues) => {
    createOrUpdateTemplate(formValues);
  };

  return (
    <Dialog className="binding-dialog" fullWidth maxWidth="sm" open={isOpen} onClose={handleClose}>
      <form noValidate onSubmit={handleSubmit(submit)}>
        <DialogTitle>{formState === 'creating' ? 'Добавление связки' : 'Изменение связки'}</DialogTitle>
        <DialogContent className="binding-dialog__content">
          {!isAccountsLoading && (
            <FormControl variant="standard" size="small" required error={Boolean(errors.accountId)}>
              <InputLabel>Аккаунт</InputLabel>
              <Select defaultValue="" label="Аккаунт" {...register('accountId', { required: true })}>
                {accounts?.data.map(({ id, name }) => (
                  <MenuItem key={id} value={id}>
                    {name}
                  </MenuItem>
                ))}
              </Select>
              {accounts?.data.length === 0 && (
                <FormHelperText required>
                  Нет добавленных<Link to="/page/auto-replies?tab=accounts">аккаунтов</Link>
                </FormHelperText>
              )}
            </FormControl>
          )}
          {!isTemplatesLoading && (
            <FormControl variant="standard" size="small" required error={Boolean(errors.templateId)}>
              <InputLabel>Шаблон</InputLabel>
              <Select defaultValue="" label="Шаблон" {...register('templateId', { required: true })}>
                {templates?.data.map(({ id, name }) => (
                  <MenuItem key={id} value={id}>
                    {name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          {!isPromptsLoading && (
            <FormControl variant="standard" size="small" required error={Boolean(errors.promtId)}>
              <InputLabel>Промпт</InputLabel>
              <Select defaultValue="" label="Промпт" {...register('promtId', { required: true })}>
                {prompts?.data.map(({ id, name }) => (
                  <MenuItem key={id} value={id}>
                    {name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          <div className="binding-dialog__submit-button">
            <Button type="submit" variant="contained" disabled={isPending}>
              {formState === 'creating' ? 'Добавить' : 'Изменить'}
            </Button>
          </div>
        </DialogContent>
      </form>
    </Dialog>
  );
};
