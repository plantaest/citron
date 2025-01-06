import { createApp } from 'vue';
import App from './App.vue';
import { showDialog } from '@/stores/showDialog.ts';
import { api } from '@/api/api.ts';

api.loadMessagesIfMissing([
  'popups-settings-save',
  'popups-settings-cancel',
  'ooui-dialog-process-error',
  'postedit-confirmation-published',
]);

const root = document.body.appendChild(document.createElement('div'));
createApp(App).mount(root);

const showDialogEvent = (event: JQuery.Event) => {
  event.preventDefault();
  showDialog.value = true;
};

mw.hook('wikipage.content').add(() => {
  $('.citron-spam-support').on('click', showDialogEvent);

  if ($('#citron-spam-gadget').length === 0) {
    mw.util.addPortletLink(
      mw.config.get('skin') === 'minerva' ? 'p-personal' : 'p-cactions',
      '/wiki/Project:Citron/Spam',
      'Citron/Spam',
      'citron-spam-gadget',
      'Citron/Spam',
    );
    $('#citron-spam-gadget').on('click', showDialogEvent);
  }
});
