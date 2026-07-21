import './promptDialog.scss';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { Button, FormHelperText } from '@mui/material';
import { PromptsApi, type PromptRequest } from '@/apis/llmApi';
import { getErrorMessage } from '@/lib/utils/getErrorMessage';

type FormValues = Pick<PromptRequest, 'name' | 'text'>;

const promptsApi = new PromptsApi();

type Props = {
  isOpen: boolean;
  formState: FormState;
  initialFields?: PromptRequest;
  close: () => void;
};

export const PromptDialog = ({ isOpen, formState, initialFields, close }: Props) => {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    register,
    handleSubmit,
    reset,
  } = useForm<FormValues>();

  const {
    mutate: createOrUpdatePrompt,
    isPending,
    error,
  } = useMutation({
    mutationFn: (data: PromptRequest) => promptsApi.createOrUpdate1(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promptsApi.getPromptsByUser'] });
      close();
    },
    onError: () => {},
  });

  useEffect(() => {
    reset({ name: '', text: '' });

    if (isOpen && formState === 'editing' && initialFields) {
      reset(initialFields);
    }
  }, [isOpen, formState]);

  const handleClose = () => {
    close();
  };

  const submit = (formValues: FormValues) => {
    createOrUpdatePrompt(formValues);
  };

  const errorMessage = getErrorMessage(error);

  return (
    <Dialog className="prompt-dialog" fullWidth maxWidth="lg" open={isOpen} onClose={handleClose}>
      <form noValidate onSubmit={handleSubmit(submit)}>
        <DialogTitle>{formState === 'creating' ? 'Добавление промпта' : 'Изменение промпта'}</DialogTitle>
        <DialogContent className="prompt-dialog__content">
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
            label="Промпт"
            placeholder="Промпт"
            multiline
            rows={16}
            required
            error={Boolean(errors.text)}
            {...register('text', { required: true })}
          />

          {errorMessage && <FormHelperText error>{errorMessage}</FormHelperText>}

          <div className="prompt-dialog__submit-button">
            <Button type="submit" variant="contained" disabled={isPending}>
              {formState === 'creating' ? 'Добавить' : 'Изменить'}
            </Button>
          </div>
        </DialogContent>
      </form>
    </Dialog>
  );
};
