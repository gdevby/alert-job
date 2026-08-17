import { getErrorMessage } from '@/lib/utils/getErrorMessage';
import './testingTab.scss';
import { AIApi, PromptsApi, TemplatesApi, type TestAutoreplyRequest } from '@/apis/llmApi';
import { Button, FormControl, FormHelperText, InputLabel, MenuItem, Select, TextField } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';

const aiApi = new AIApi();
const templatesApi = new TemplatesApi();
const promptsApi = new PromptsApi();

type FormValues = Pick<TestAutoreplyRequest, 'promptId' | 'templateId' | 'orderDescription'> & { a: string };

export const TestingTab = () => {
  const {
    formState: { errors },
    register,
    handleSubmit,
    watch,
  } = useForm<FormValues>();

  const { data: templates, isLoading: isTemplatesLoading } = useQuery({
    queryKey: ['templatesApi.getTemplatesByUser'],
    queryFn: () => templatesApi.getTemplatesByUser(),
    placeholderData: data => data,
  });

  const { data: prompts, isLoading: isPromptsLoading } = useQuery({
    queryKey: ['promptsApi.getPromptsByUser'],
    queryFn: () => promptsApi.getPromptsByUser(),
    placeholderData: data => data,
  });

  const { mutate, isPending, error, data } = useMutation({
    mutationFn: async (data: TestAutoreplyRequest) => aiApi.testAutoreply(data),
    onSuccess: () => {},
    onError: () => {},
  });

  const submit = (formValues: FormValues) => {
    mutate(
      {
        promptId: formValues.promptId,
        templateId: formValues.templateId,
        orderDescription: formValues.orderDescription,
      },
      {},
    );
  };

  const templateId = watch('templateId') ?? '';
  const promptId = watch('promptId') ?? '';
  const errorMessage = getErrorMessage(error);

  return (
    <form noValidate onSubmit={handleSubmit(submit)}>
      <div className="testing-tab">
        <div className="testing-tab__header">
          <FormControl
            fullWidth
            variant="standard"
            size="small"
            required
            disabled={isTemplatesLoading}
            error={Boolean(errors.templateId)}
          >
            <InputLabel>Шаблон</InputLabel>
            <Select value={templateId} label="Шаблон" {...register('templateId', { required: true })}>
              {templates?.data.map(({ id, name }) => (
                <MenuItem key={id} value={id}>
                  {name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl
            fullWidth
            variant="standard"
            size="small"
            required
            disabled={isPromptsLoading}
            error={Boolean(errors.promptId)}
          >
            <InputLabel>Промпт</InputLabel>
            <Select value={promptId} label="Промпт" {...register('promptId', { required: true })}>
              {prompts?.data.map(({ id, name }) => (
                <MenuItem key={id} value={id}>
                  {name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button type="submit" variant="contained" disabled={isPending}>
            Отправить
          </Button>
        </div>

        <div className="testing-tab__order-description">
          <TextField
            fullWidth
            label="Описание заказа"
            placeholder="Описание заказа"
            multiline
            rows={14}
            required
            error={Boolean(errors.orderDescription)}
            {...register('orderDescription', { required: true })}
          />
        </div>

        <div className="testing-tab__answer">
          <TextField
            fullWidth
            value={isPending ? '' : data?.data.reply}
            label={isPending ? 'Получение ответа от нейронки...' : ''}
            placeholder={!isPending && !data?.data.reply ? 'Ответ нейронки' : ''}
            multiline
            rows={14}
            disabled
          />
        </div>

        {errorMessage && <FormHelperText error>{errorMessage}</FormHelperText>}
      </div>
    </form>
  );
};
