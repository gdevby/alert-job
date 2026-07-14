import './templateDialog.scss';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';
import type { FormState } from '@/lib/constants/FormState';
import { Button } from '@mui/material';
import { TemplatesApi, type TemplateRequest } from '@/apis/llmApi';

const templatesApi = new TemplatesApi();

type Fields = Pick<TemplateRequest, 'name' | 'text'>;

type Props = {
  isOpen: boolean;
  close: () => void;
};

export const TemplateDialog = ({ isOpen, close }: Props) => {
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

  const handleClose = () => {
    close();
  };

  const handleCreateOrUpdateButton = () => {
    createOrUpdateTemplate({ name, text });
  };

  return (
    <Dialog className="template-dialog" fullWidth maxWidth="lg" open={isOpen} onClose={handleClose}>
      <DialogTitle>Добавление шаблона</DialogTitle>
      <DialogContent className="template-dialog__content">
        <TextField
          type="text"
          label="Название"
          variant="standard"
          placeholder="Название"
          onChange={e => setName(e.target.value)}
        />
        <TextField
          label="Текст шаблона"
          placeholder="Текст шаблона"
          multiline
          rows={16}
          onChange={e => setText(e.target.value)}
        />

        <div className="template-dialog__submit-button">
          <Button variant="contained" onClick={handleCreateOrUpdateButton}>
            Создать
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
