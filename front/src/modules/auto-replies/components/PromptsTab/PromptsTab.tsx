import './promptsTab.scss';

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
import { PromptsApi } from '@/apis/llmApi';
import { FormState } from '@/lib/constants/FormState';
import { PromptDialog } from '@/modules/auto-replies/components/PromptsTab/PromptDialog';

const promptsApi = new PromptsApi();

export const PromptsTab = () => {
  const queryClient = useQueryClient();
  const [isModalShown, setIsModalShown] = useState(false);
  const [promptIdForEditing, setPromptIdForEditing] = useState<number>();
  const [formState, setFormState] = useState<FormState>(FormState.Creating);

  const { data } = useQuery({
    queryKey: ['promptsApi.getPromptsByUser'],
    queryFn: () => promptsApi.getPromptsByUser(),
    placeholderData: data => data,
  });

  const { mutate: removePrompt } = useMutation({
    mutationFn: (id: number) => promptsApi.deletePrompt(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promptsApi.getPromptsByUser'] });
    },
    onError: () => {},
  });

  const handleCreatePromptButton = () => {
    setPromptIdForEditing(undefined);
    setFormState('creating');
    setIsModalShown(true);
  };

  const handleEditButton = (id: number) => {
    setPromptIdForEditing(id);
    setFormState('editing');
    setIsModalShown(true);
  };

  const handleRemoveButton = (id: number) => {
    removePrompt(id);
  };

  const initialFields = data?.data.find(({ id }) => id === promptIdForEditing);

  return (
    <div className="prompts-tab">
      <Button onClick={handleCreatePromptButton}>Добавить промпт</Button>
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }}>
          <TableHead>
            <TableRow>
              <TableCell>Название</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.data?.map(({ id, name, createdAt }) => (
              <TableRow key={id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                <TableCell>{name}</TableCell>
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

      <PromptDialog
        isOpen={isModalShown}
        formState={formState}
        initialFields={initialFields}
        close={() => setIsModalShown(false)}
      />
    </div>
  );
};
