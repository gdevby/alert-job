import './templatesTab.scss';

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
import { TemplatesApi } from '@/apis/llmApi';
import { TemplateDialog } from '@/modules/auto-replies/components/TemplatesTab/TemplateDialog';

const templatesApi = new TemplatesApi();

export const TemplatesTab = () => {
  const queryClient = useQueryClient();
  const [isModalShown, setIsModalShown] = useState(false);

  const { data, isFetching } = useQuery({
    queryKey: ['templatesApi.getTemplatesByUser'],
    queryFn: () => templatesApi.getTemplatesByUser(),
    placeholderData: data => data,
  });

  const { mutate: removeTemplate } = useMutation({
    mutationFn: (id: number) => templatesApi.deleteTemplate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templatesApi.getTemplatesByUser'] });
    },
    onError: () => {},
  });

  const handleCreateTemplateButton = () => {
    setIsModalShown(true);
  };

  const handleRemoveButton = (id: number) => {
    removeTemplate(id);
  };

  return (
    <div className="templates-tab">
      <Button onClick={handleCreateTemplateButton}>Добавить шаблон</Button>
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="simple table">
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
                  <Button variant="outlined">Изменить</Button>{' '}
                  <Button variant="outlined" color="error" onClick={() => handleRemoveButton(id)}>
                    Удалить
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TemplateDialog isOpen={isModalShown} close={() => setIsModalShown(false)} />
    </div>
  );
};
