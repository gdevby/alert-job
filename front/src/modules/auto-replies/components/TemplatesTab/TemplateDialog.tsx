import './templateDialog.scss';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { Button } from '@mui/material';
import { TemplatesApi, type TemplateRequest } from '@/apis/llmApi';

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

  const { mutate: createOrUpdateTemplate, isPending } = useMutation({
    mutationFn: (data: TemplateRequest) => templatesApi.createOrUpdate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templatesApi.getTemplatesByUser'] });
      close();
    },
    onError: () => {},
  });

  useEffect(() => {
    if (isOpen && formState === 'editing' && initialFields) {
      const { name, text } = initialFields;
      reset({ name, text });
    }
  }, [isOpen, formState]);

  const handleClose = () => {
    close();
    reset({ name: '', text: '' });
  };

  const submit = (formValues: FormValues) => {
    createOrUpdateTemplate(formValues);
  };

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
            defaultValue={initialFields?.name}
            error={Boolean(errors.name)}
            {...register('name', { required: true })}
          />
          <TextField
            label="Текст шаблона"
            placeholder="Текст шаблона"
            multiline
            rows={16}
            required
            defaultValue={initialFields?.text}
            error={Boolean(errors.text)}
            {...register('text', { required: true })}
          />

          <div className="template-dialog__submit-button">
            <Button type="submit" variant="contained" disabled={isPending}>
              {formState === 'creating' ? 'Создать' : 'Изменить'}
            </Button>
          </div>
        </DialogContent>
      </form>
    </Dialog>
  );
};
