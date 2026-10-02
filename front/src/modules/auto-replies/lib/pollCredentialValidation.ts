/**
 * Двухшаговая проверка аккаунта: POST/GET validate → jobId, затем опрос GET validate/jobs/{jobId}.
 * Нужна, чтобы не упираться в таймаут nginx при долгом Playwright.
 */
import type { AxiosResponse } from 'axios';
import {
  UserCredentialsApi,
  type CredentialValidationJobResponse,
  type CredentialValidationResult,
} from '@/apis/coreApi';

/** Интервал опроса статуса job, мс */
const POLL_INTERVAL_MS = 3000;
/** Максимальное ожидание завершения проверки, мс */
const MAX_POLL_MS = 600_000;

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

const api = new UserCredentialsApi();

export async function runCredentialValidationWithPolling(
  credentialId: number,
): Promise<AxiosResponse<CredentialValidationResult>> {
  const start = await api.startCredentialValidation(credentialId);
  const jobId = start.data.jobId;
  if (!jobId) {
    throw new Error('Сервер не вернул идентификатор задачи проверки');
  }

  const deadline = Date.now() + MAX_POLL_MS;
  while (Date.now() < deadline) {
    const statusResponse = await api.getCredentialValidationJob(jobId);
    const job: CredentialValidationJobResponse = statusResponse.data;
    if (job.status === 'PENDING') {
      await sleep(POLL_INTERVAL_MS);
      continue;
    }
    const result = job.result ?? { success: false, errorMessage: 'Пустой результат проверки' };
    return { ...statusResponse, data: result };
  }

  throw new Error('Проверка аккаунта заняла слишком много времени. Повторите позже.');
}
