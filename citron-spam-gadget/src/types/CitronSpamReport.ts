export interface CitronSpamReport {
  version: number;
  updatedAt: string;
  hostnames: Array<{
    hostname: string;
    time: string;
    score: number;
    revisionIds: number[];
  }>;
  revisions: Record<number, { page: string; user: string }>;
  feedbacks: Array<{
    createdAt: string;
    createdBy: number;
    hostname: string;
    status: number;
    hash: string;
    user: string;
    synced: boolean;
  }>;
}

export const defaultReport: CitronSpamReport = {
  version: 0,
  updatedAt: '2025-01-01T00:00:00.000Z',
  hostnames: [],
  revisions: {},
  feedbacks: [],
};
