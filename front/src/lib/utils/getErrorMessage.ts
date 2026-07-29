import { AxiosError } from 'axios';

export const getErrorMessage = (error: Error | AxiosError | null) => {
  if (error === null) {
    return null;
  }

  if (error instanceof AxiosError) {
    const data = error.response?.data;

    if (typeof data === 'string') {
      return data;
    }

    if (data.error) {
      return data.error;
    }
  }

  return 'Что-то пошло не так. Попробуйте снова';
};
