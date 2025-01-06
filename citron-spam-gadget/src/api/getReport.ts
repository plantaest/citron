import { api } from '@/api/api.ts';
import { reactive } from 'vue';
import type { Query } from '@/types/Query.ts';
import type { CitronSpamReport } from '@/types/CitronSpamReport.ts';

export function getReport(title: string) {
  const query = reactive<Query<CitronSpamReport, string>>({ isLoading: true });
  const params = {
    format: 'json',
    formatversion: 2,
    action: 'query',
    prop: 'revisions',
    titles: title,
    rvslots: '*',
    rvprop: 'content',
  };

  api
    .get(params)
    .done((data) => {
      const page = data.query.pages[0];

      if ('missing' in page) {
        query.error = 'missing';
      }

      if ('revisions' in page) {
        query.data = JSON.parse(page.revisions[0].slots.main.content);
      }

      query.isLoading = false;
    })
    .fail((error) => {
      query.error = error;
      query.isLoading = false;
    });

  return query;
}
