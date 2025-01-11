<script setup lang="ts">
import { toPercentage } from '@/helpers/toPercentage.ts';
import { cdxIconAlert, cdxIconArrowNext } from '@wikimedia/codex-icons';
import { createContribsUri } from '@/helpers/createContribsUri.ts';
import { createDiffUri } from '@/helpers/createDiffUri.ts';
import { CdxDialog, CdxField, CdxIcon, CdxRadio, type PrimaryDialogAction } from '@wikimedia/codex';
import { getReport } from '@/api/getReport.ts';
import { computed, reactive, watch, watchEffect } from 'vue';
import { type CitronSpamReport, defaultReport } from '@/types/CitronSpamReport.ts';
import { showDialog } from '@/stores/showDialog.ts';
import { hashSha1 } from '@/helpers/hashSha1.ts';
import { sortObjectByKey } from '@/helpers/sortObjectByKey.ts';
import { saveReport } from '@/api/saveReport.ts';
import { getCurrentDate } from '@/helpers/getCurrentDate.ts';

// Initial variables
const userId = mw.config.get('wgUserId')!;
const username = mw.config.get('wgUserName')!;
const wikiId = mw.config.get('wgWikiID')!;
const reportDate = getCurrentDate();
const reportPageTitle = `Project:Citron/Spam/${reportDate}.json`;

// Fetch report
const reportQuery = getReport(reportPageTitle);
const report = computed(() => reportQuery.data ?? defaultReport);

// Helper
const getRevisions = (revisionIds: number[]) =>
  revisionIds.map((revisionId) => ({
    id: revisionId,
    ...report.value.revisions[revisionId],
  }));

// Radios
const radioOptions = ['good', 'bad'] as const;
type RadioOptions = (typeof radioOptions)[number];
const initialRadios = computed<Record<string, RadioOptions>>(() =>
  Object.fromEntries(
    report.value.feedbacks
      .filter((feedback) => feedback.createdBy === userId)
      .map((feedback) => [feedback.hostname, feedback.status === 0 ? 'good' : 'bad']),
  ),
);
const radios: Record<string, RadioOptions> = reactive({});
watch(initialRadios, (payload) => Object.assign(radios, payload));

const toggleRadio = (hostname: string, option: RadioOptions) => {
  if (radios[hostname] === option) {
    delete radios[hostname];
  }
};

// Config dialog
const primaryAction = reactive<PrimaryDialogAction>({
  label: mw.message('popups-settings-save').text(),
  actionType: 'progressive',
  disabled: true,
});
watchEffect(() => {
  primaryAction.disabled =
    JSON.stringify(sortObjectByKey(initialRadios.value)) ===
    JSON.stringify(sortObjectByKey(radios));
});

const defaultAction = {
  label: mw.message('popups-settings-cancel').text(),
};

const onPrimaryAction = async () => {
  primaryAction.disabled = true;
  const now = new Date().toISOString().slice(0, 19) + 'Z';
  const feedbacks: CitronSpamReport['feedbacks'] = [];

  for (const [hostname, status] of Object.entries(radios)) {
    feedbacks.push({
      createdAt: now,
      createdBy: userId,
      hostname: hostname,
      status: status === 'good' ? 0 : 1,
      hash: await hashSha1(now, String(userId), wikiId, reportDate, hostname),
      user: username,
      synced: false,
    });
  }

  const reportQuery = getReport(reportPageTitle);
  watch(reportQuery, (payload) => {
    if (payload.data) {
      const updatedReport = {
        ...payload.data,
        feedbacks: [
          ...payload.data.feedbacks.filter((feedback) => feedback.createdBy !== userId),
          ...feedbacks,
        ],
      };

      const saveReportMutation = saveReport(reportPageTitle, updatedReport);
      watch(saveReportMutation, (payload) => {
        if (payload.isSuccess && payload.data) {
          mw.notify(
            $(
              '<div class="oo-ui-labelElement oo-ui-flaggedElement-success oo-ui-iconElement oo-ui-messageWidget">' +
                '<span class="oo-ui-iconElement-icon oo-ui-icon-success oo-ui-image-success"></span>' +
                '<span class="oo-ui-labelElement-label">' +
                mw
                  .message('postedit-confirmation-published', null, payload.data.edit.newrevid)
                  .parse() +
                '</span>' +
                '</div>',
            ),
          );
          showDialog.value = false;
        }

        if (payload.error) {
          mw.notify(mw.message('ooui-dialog-process-error').text(), { type: 'error' });
          primaryAction.disabled = false;
        }
      });
    }

    if (payload.error) {
      mw.notify(mw.message('ooui-dialog-process-error').text(), { type: 'error' });
      primaryAction.disabled = false;
    }
  });
};

const onDefaultAction = () => {
  showDialog.value = false;
};
</script>

<template>
  <cdx-dialog
    class="citron-spam-dialog"
    v-model:open="showDialog"
    title="Citron · Spam"
    :subtitle="reportDate"
    :use-close-button="true"
    :primary-action="primaryAction"
    :default-action="defaultAction"
    @primary="onPrimaryAction"
    @default="onDefaultAction"
  >
    <div class="cdx-progress-bar" role="progressbar" v-if="reportQuery.isLoading">
      <div class="cdx-progress-bar__bar" />
    </div>
    <div class="citron-spam-empty" v-if="reportQuery.error === 'missing'">404</div>
    <div class="citron-spam-content" v-else>
      <div
        class="citron-spam-hostname"
        :data-selected="radios[hostname.hostname]"
        v-for="(hostname, index) in report.hostnames"
        :key="hostname.hostname"
      >
        <div class="citron-spam-hostname-indicator">
          <div class="citron-spam-hostname-index">{{ index + 1 }}</div>
          <div class="citron-spam-hostname-time">{{ hostname.time }}</div>
        </div>
        <div class="citron-spam-hostname-inner">
          <div
            class="citron-spam-hostname-inner-top"
            :style="{ '--percentage': toPercentage(hostname.score) }"
          >
            <a
              role="button"
              :href="'//' + hostname.hostname"
              target="_blank"
              class="citron-spam-hostname-inner-top-left"
              :class="{
                'citron-spam-very-bad': hostname.score >= 0.8,
                'citron-spam-bad': hostname.score >= 0.5 && hostname.score < 0.8,
                'citron-spam-uncertain': hostname.score < 0.5,
              }"
            >
              <div class="citron-spam-hostname-hostname">
                {{ hostname.hostname }}
              </div>
              <div class="citron-spam-hostname-score">{{ toPercentage(hostname.score) }}</div>
            </a>

            <div class="citron-spam-hostname-inner-top-right">
              <cdx-field :is-fieldset="true" :hide-label="true">
                <cdx-radio
                  :inline="true"
                  v-for="option in radioOptions"
                  :key="option"
                  :name="hostname.hostname"
                  :input-value="option"
                  v-model="radios[hostname.hostname]"
                  @click="toggleRadio(hostname.hostname, option)"
                >
                  <cdx-icon
                    :icon="option === 'good' ? cdxIconArrowNext : cdxIconAlert"
                    :style="{
                      color:
                        option === 'good'
                          ? 'var(--color-progressive, #88a3e8)'
                          : 'var(--color-icon-error, #f54739)',
                    }"
                  />
                </cdx-radio>
              </cdx-field>
            </div>
          </div>

          <div class="citron-spam-hostname-inner-bottom">
            <div
              class="citron-spam-revision"
              v-for="revision in getRevisions(hostname.revisionIds)"
              :key="revision.id"
            >
              <span>
                <a :href="createContribsUri(revision.user)" target="_blank">{{ revision.user }}</a>
                (<a :href="createDiffUri(revision.id)" target="_blank"> {{ revision.page }} </a>)
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </cdx-dialog>
</template>

<style scoped>
:global(.citron-spam-dialog) {
  max-width: 60rem !important;
}

:global(.citron-spam-dialog .cdx-dialog__header__subtitle) {
  font-family: monospace;
}

.citron-spam-empty {
  text-align: center;
  font-size: 5rem;
  font-family: monospace;
  color: var(--color-error, #bf3c2c);
}

.citron-spam-content {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  font-size: 0.875rem;
}

.citron-spam-content * {
  box-sizing: border-box;
}

.citron-spam-hostname {
  display: flex;
  justify-content: space-between;
  gap: 0.625rem;
  flex-direction: column;

  &[data-selected='good'] {
    background-color: var(--background-color-progressive-subtle, #1b223d);
  }

  &[data-selected='bad'] {
    background-color: var(--background-color-error-subtle, #ffe9e5);
  }
}

.citron-spam-hostname-indicator {
  display: flex;
  gap: 0.625rem;
}

.citron-spam-hostname-index {
  height: 1.75rem;
  width: 1.75rem;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--color-base--subtle, #54595d);
  border: 1px dashed var(--border-color-base, #a2a9b1);
}

.citron-spam-hostname-time {
  padding-inline: 0.5rem;
  display: flex;
  align-items: center;
  height: 1.75rem;
  font-size: 0.75rem;
  font-family: monospace;
  background-color: var(--background-color-neutral, #eaecf0);
  color: var(--color-base--subtle, #54595d);
  border: 1px solid var(--border-color-base, #a2a9b1);
  width: max-content;
}

.citron-spam-hostname-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  min-width: 0;
  border-inline-start: 0.3125rem solid var(--background-color-interactive--hover, #dadde3);
  padding-inline-start: 0.625rem;
}

.citron-spam-hostname-inner-top {
  display: flex;
  justify-content: space-between;
  gap: 0.625rem;
  flex-direction: column;
}

.citron-spam-hostname-inner-top-left {
  padding-inline: 0.5rem;
  height: 1.75rem;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  gap: 0.625rem;
  border-radius: 0;
  text-decoration: none;

  &:hover {
    filter: brightness(1.1);
  }

  &:active {
    filter: brightness(0.9);
  }
}

.citron-spam-hostname-hostname {
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
}

.citron-spam-hostname-score {
  white-space: nowrap;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.citron-spam-hostname-inner-top-right {
  display: flex;
  gap: 0.625rem;
  align-items: center;

  & fieldset {
    display: flex;
  }
}

.citron-spam-hostname-inner-bottom {
  display: flex;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.citron-spam-revision {
  padding-block: 0.3rem;
  padding-inline: 0.5rem;
  font-size: 0.75rem;
  background-color: var(--background-color-warning-subtle, #fdf2d5);
  color: var(--color-warning, #886425);
  border: 1px dotted var(--border-color-warning, #ab7f2a);
  word-break: break-word;
  display: flex;
  align-items: center;
  line-height: 1.4;
}

/* Responsive */

@media (min-width: 576px) {
  .citron-spam-hostname,
  .citron-spam-hostname-inner-top {
    flex-direction: row;
  }

  .citron-spam-hostname-inner-top {
    gap: 1rem;
  }

  .citron-spam-hostname-inner-top-left {
    flex: 1;
  }
}

/* Utilities  */

.citron-spam-very-bad {
  background-image: linear-gradient(
    to right,
    var(--background-color-destructive-subtle--hover, #ffdad3) var(--percentage),
    var(--background-color-destructive-subtle, #ffe9e5) var(--percentage)
  );
  color: var(--color-destructive, #bf3c2c);
  border: 1px solid var(--border-color-destructive, #f54739);
}

.citron-spam-bad {
  background-image: linear-gradient(
    to right,
    light-dark(#ffdcb8, #572c19) var(--percentage),
    light-dark(#ffead4, #361d13) var(--percentage)
  );
  color: light-dark(#a95226, #f97f26);
  border: 1px solid #d46926;
}

.citron-spam-uncertain {
  background-image: linear-gradient(
    to right,
    var(--background-color-progressive-subtle--hover, #dce3f9) var(--percentage),
    var(--background-color-progressive-subtle, #f1f4fd) var(--percentage)
  );
  color: var(--color-progressive, #36c);
  border: 1px solid var(--border-color-progressive, #6485d1);
}
</style>
