<script setup lang="ts">
import CitronSpamDialog from '@/components/CitronSpamDialog.vue';
import { showDialog } from '@/stores/showDialog.ts';
</script>

<template>
  <citron-spam-dialog v-if="showDialog" />
</template>
