import './templateDialog.scss';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import TextField from '@mui/material/TextField';
import DialogContent from '@mui/material/DialogContent';

type Props = {
  isOpen: boolean;
  close: () => void;
};

export const TemplateDialog = ({ isOpen, close }: Props) => {
  const handleClose = () => {
    close();
  };

  return (
    <Dialog className="template-dialog" fullWidth maxWidth="lg" open={isOpen} onClose={handleClose}>
      <DialogTitle>Создание шаблона</DialogTitle>
      <DialogContent className="template-dialog__content">
        <TextField type="text" label="Название" variant="standard" placeholder="Название" />
        <TextField label="Текст шаблона" placeholder="Текст шаблона" multiline rows={11} maxRows={100} />
      </DialogContent>
    </Dialog>
  );
};
