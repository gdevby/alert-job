import './templateDialog.scss';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { Button, FormHelperText } from '@mui/material';
import { TemplatesApi, type TemplateRequest } from '@/apis/llmApi';
import { getErrorMessage } from '@/lib/utils/getErrorMessage';

type FormValues = Pick<TemplateRequest, 'name' | 'text'>;

const templatesApi = new TemplatesApi();

type Props = {
  isOpen: boolean;
  formState: FormState;
  initialFields?: TemplateRequest;
  close: () => void;
};

export const TemplateDialog = ({ isOpen, formState, initialFields, close }: Props) => {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    register,
    handleSubmit,
    reset,
  } = useForm<FormValues>();

  const {
    mutate: createOrUpdateTemplate,
    isPending,
    error,
  } = useMutation({
    mutationFn: (data: TemplateRequest) => templatesApi.createOrUpdate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templatesApi.getTemplatesByUser'] });
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
    createOrUpdateTemplate(formValues);
  };

  const errorMessage = getErrorMessage(error);

  return (
    <Dialog className="template-dialog" fullWidth maxWidth="lg" open={isOpen} onClose={handleClose}>
      <form noValidate onSubmit={handleSubmit(submit)}>
        <DialogTitle>{formState === 'creating' ? 'Добавление шаблона' : 'Изменение шаблона'}</DialogTitle>
        <DialogContent className="template-dialog__content">
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
            label="Текст шаблона"
            placeholder="Текст шаблона"
            multiline
            rows={16}
            required
            error={Boolean(errors.text)}
            {...register('text', { required: true })}
          />

          {errorMessage && <FormHelperText error>{errorMessage}</FormHelperText>}

          <div className="template-dialog__submit-button">
            <Button type="submit" variant="contained" disabled={isPending}>
              {formState === 'creating' ? 'Добавить' : 'Изменить'}
            </Button>
          </div>
        </DialogContent>
      </form>
    </Dialog>
  );
};
