import { useQuery } from '@tanstack/react-query';
import { AutoReplyApi } from '@/apis/coreApi';

const autoReplyApi = new AutoReplyApi();

// TODO remove after development
export const useAutoReplyStatus = (): boolean => {
  const { data } = useQuery({
    queryKey: ['autoReplyApi.getStatus'],
    queryFn: () => autoReplyApi.getStatus(),
    placeholderData: data => data,
  });

  return data?.data ?? false;
};
