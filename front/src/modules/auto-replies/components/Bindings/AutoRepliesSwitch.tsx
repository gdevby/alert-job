import './AutoRepliesSwitch.scss';
import { UserFiltersApi } from '@/apis/coreApi';
import { Switch } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

const userFiltersApi = new UserFiltersApi();

export const AutoRepliesSwitch = () => {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['userFiltersApi.getAutoReplyStatus'],
    queryFn: () => userFiltersApi.getAutoReplyStatus(Number(id)),
    placeholderData: data => data,
  });

  const { mutate } = useMutation({
    mutationFn: (enabled: boolean) => userFiltersApi.setAutoReplyStatus(Number(id), enabled),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userFiltersApi.getAutoReplyStatus'] });
      close();
    },
  });

  const handleSwitch = (checked: boolean) => {
    mutate(checked);
  };

  return (
    <div className="auto-replies-switch">
      {!isLoading && (
        <>
          <Switch checked={data?.data ?? false} onChange={event => handleSwitch(event.target.checked)} />
          <span>Автоответы {data?.data ? 'включены' : 'отключены'}</span>
        </>
      )}
    </div>
  );
};
