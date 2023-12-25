<template>
<el-dropdown placement="bottom-end">
    <el-button circle type="primary">
    <svg-icon name="translate" size="24px" class="icon-translation"></svg-icon></el-button>
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
import {useI18n} from "vue-i18n";

const { locale } = useI18n({ useScope: 'global' });

function languageChange(target) {
    locale.value = target;
    localStorage.setItem('lang', target);
}
</script>

<style scoped lang="scss">

</style>
