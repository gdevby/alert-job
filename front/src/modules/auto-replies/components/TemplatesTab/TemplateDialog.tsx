import './templateDialog.scss';

import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import { FormState } from '@/lib/constants/FormState';
import { Button } from '@mui/material';
import { TemplatesApi, type TemplateRequest } from '@/apis/llmApi';

const templatesApi = new TemplatesApi();

type Props = {
  isOpen: boolean;
  formState: FormState;
  initialFields?: TemplateRequest;
  close: () => void;
};

export const TemplateDialog = ({ isOpen, formState, initialFields, close }: Props) => {
  const queryClient = useQueryClient();
  const [name, setName] = useState<string>();
  const [text, setText] = useState<string>();

  const { mutate: createOrUpdateTemplate } = useMutation({
    mutationFn: (data: TemplateRequest) => templatesApi.createOrUpdate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templatesApi.getTemplatesByUser'] });
      close();
    },
    onError: () => {},
  });

  useEffect(() => {
    if (isOpen && formState === 'editing' && initialFields) {
      console.log('initialFields', initialFields);
      const { name, text } = initialFields;

      setName(name);
      setText(text);
    }
  }, [isOpen, formState]);

  const handleClose = () => {
    setName(undefined);
    setText(undefined);
    close();
  };

  const handleCreateOrUpdateButton = () => {
    createOrUpdateTemplate({ name, text });
  };

  return (
    <Dialog className="template-dialog" fullWidth maxWidth="lg" open={isOpen} onClose={handleClose}>
      <DialogTitle>{formState === 'creating' ? 'Добавление шаблона' : 'Изменение шаблона'}</DialogTitle>
      <DialogContent className="template-dialog__content">
        <TextField
          type="text"
          label="Название"
          variant="standard"
          placeholder="Название"
          defaultValue={initialFields?.name}
          disabled={formState === 'editing'}
          onChange={e => setName(e.target.value)}
          required
        />
        <TextField
          label="Текст шаблона"
          placeholder="Текст шаблона"
          multiline
          rows={16}
          defaultValue={initialFields?.text}
          onChange={e => setText(e.target.value)}
          required
        />

        <div className="template-dialog__submit-button">
          <Button variant="contained" onClick={handleCreateOrUpdateButton}>
            {formState === 'creating' ? 'Создать' : 'Изменить'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
