import { reactive } from 'vue';
import type { Mutation } from '@/types/Mutation.ts';
import { api } from '@/api/api.ts';
import type { CitronSpamReport } from '@/types/CitronSpamReport.ts';
import type { EditResponse } from '@/types/EditResponse.ts';

export function saveReport(title: string, content: CitronSpamReport) {
  const mutation = reactive<Mutation<EditResponse, string>>({ isSuccess: false });
  const params = {
    format: 'json',
    formatversion: 2,
    action: 'edit',
    title: title,
    text: JSON.stringify(content),
    summary: 'Update feedbacks for Citron/Spam report',
    contentmodel: 'json',
    watchlist: 'unwatch',
  };

  api
    .postWithToken('csrf', params)
    .done((data) => {
      mutation.data = data as EditResponse;
      mutation.isSuccess = true;
    })
    .fail((error) => {
      mutation.error = error;
    });

  return mutation;
}
