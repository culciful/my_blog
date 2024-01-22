<script setup lang="ts">
import {useOnResize} from '@/mixins/event.ts';
import {LOGIN_STATE} from '@/utils/localStoreItem';
import {getCurrentInstance} from 'vue';
const { proxy }: any = getCurrentInstance();
import Constant from '@/model/user/constant';
import {useUserStore} from '@/stores/user';

useOnResize();
const userStore = useUserStore();
if(localStorage.getItem(LOGIN_STATE)) {
    proxy.$request.post(Constant.url.getUserInfo).then(res => {
        userStore.setUserData(res.result);
    }).catch(err => {
        localStorage.removeItem(LOGIN_STATE);
    });
}
</script>

<template>
  <RouterView />
</template>

<style scoped>

</style>
