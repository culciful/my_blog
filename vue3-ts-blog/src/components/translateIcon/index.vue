<template>
<el-dropdown placement="bottom-end">
    <svg-icon name="translate" :color="color" :size="24"></svg-icon>
    <template #dropdown>
        <el-dropdown-menu >
            <el-dropdown-item v-for="(value, key) in languageOptions" :key="key" :disabled="key===locale" @click="languageChange(key)">
                {{value}}
            </el-dropdown-item>
        </el-dropdown-menu>
    </template>
</el-dropdown>
</template>

<script lang="ts" setup name="TranslateIcon">
import {languageOptions} from '@/language/i18n';
import {useI18n} from 'vue-i18n';
import {LANG} from '@/utils/localStoreItem';
defineProps({
    color: {
        type: String,
        default: ''
    }
});
const { locale } = useI18n({ useScope: 'global' });

function languageChange(target) {
    locale.value = target;
    localStorage.setItem(LANG, target);
    location.reload();
}
</script>

<style scoped lang="scss">
</style>
