<template>
<header class="nav-bar">
    <div class="navbar">
        <div class="container">
            <router-link class="logo" to="/">
                <img src="/static/img/logo-no-background.png">
            </router-link>
            <nav>
                <template v-if="!hideSearch">
                    <el-input v-if="showSearchInput"
                              class="a-m-h-xs"
                              v-model="keyword"
                              @change="changeKeyword"
                              :placeholder="$t('label.search')">
                    </el-input>
                    <svg-icon v-else @click="openSearchDialog" :size="24" class="a-m-h-xs" name="search"></svg-icon>
                </template>

                <el-dialog v-model="searchDialogVisible"
                           width="100%"
                           top="0"
                           :show-close="false"
                           :before-close="handleSearchClose"
                           modal-class="modal-class"
                           class="search-dialog">
                    <template #header="{close}">
                        <div class="search-header">
                            <el-input v-model="keyword"
                                      ref="searchInputRef"
                                      clearable
                                      @change="changeKeyword"
                                      :placeholder="$t('label.search')">
                            </el-input>
                            <el-button text type="primary" @click="close">{{$t('label.cancel')}}</el-button>
                        </div>
                    </template>
                </el-dialog>


                <template v-if="userStore.isLoggedIn">
                    <svg-icon v-if="!hideWrite"
                              @click="gotoEdit"
                              :title="$t('label.write')"
                              :size="24"
                              name="edit"
                              class="a-m-h-xs">
                    </svg-icon>
                    <svg-icon @click="gotoMessage"
                             :title="$t('label.message')"
                             :size="24"
                              name="bell"
                             class="a-m-h-xs">
                    </svg-icon>
                </template>

                <translate-icon class="a-m-h-xs"></translate-icon>

                <svg-icon v-if="!userStore.isLoggedIn"
                          @click="gotoLogin"
                          :title="$t('label.login')"
                          class="a-m-h-xs"
                          :size="24"
                          name="user-filling">
                </svg-icon>

                <el-dropdown v-else placement="bottom-end">
                    <img class="a-m-h-xs user-icon" :src="handleAvatar(userStore.avatarUrl)" />
                    <template #dropdown>
                        <el-dropdown-menu >
                            <el-dropdown-item 
                                v-for="(func, key) in userOptions"
                                :key="key"
                                @click="func">
                                {{$t(`label.${key}`)}}
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </nav>
        </div>
    </div>
</header>
</template>

<script lang="ts" setup name="CustomHeader">
import TranslateIcon from '@/components/translateIcon/index.vue';
import {useUserStore} from '@/stores/user';
import { useRouter } from 'vue-router';
import {computed, ref} from 'vue';
import {useWindowStore} from '@/stores/window';
import {storeToRefs} from 'pinia';
import {useRoute} from 'vue-router';
import {needLoginPathReg} from '@/router/index.ts';
import {getCurrentInstance} from 'vue';
import Constant, {handleAvatar}  from '@/model/user/constant';

const props = defineProps({
    needHandleSearch: {
        type: Boolean,
        default: false
    },
    hideSearch: {
        type: Boolean,
        default: false
    },
    hideWrite: {
        type: Boolean,
        default: false
    }
});
const emit = defineEmits<{
    'keyword-change': [value: string] // 具名元组语法
}>();
const {proxy} = getCurrentInstance();
const router = useRouter();
const userStore = useUserStore();
const searchInputRef = ref();
const {screenWidth} = storeToRefs(useWindowStore());
const searchDialogVisible = ref(false);
const showSearchInput = computed(() => screenWidth.value > 520);

const keyword = ref('');
const changeKeyword = () => {
    emit('keyword-change', keyword.value);
    if(props.needHandleSearch) {
        router.push({path: '/index', query: {keyword: keyword.value}});
    }
};
function openSearchDialog() {
    searchDialogVisible.value = true;
    setTimeout(() => {
        searchInputRef.value.focus();
    }, 100);
}
const handleSearchClose = (done: () => void) => {
    keyword.value = '';
    done();
};

const gotoLogin = () => {
    router.push({ name: 'login' });
};
const gotoMessage = () => {
    router.push({ name: 'message' });
};
const gotoEdit = () => {
    router.push({ name: 'write' });
};

const userCenterHandler = () => {
    router.push({ name: 'userCenter' });
};
const manageContentHandler = () => {
    router.push({ name: 'manageContent' });
};

const route = useRoute();
const logoutHandler = () => {
    proxy.$request.post(Constant.url.logout);
    userStore.clear();
    if(needLoginPathReg.test(route.path)) {
        router.push({name: 'home'});
    }
};
const userOptions = {
    userCenter: userCenterHandler,
    manageContent: manageContentHandler,
    logout: logoutHandler
};
</script>

<style scoped lang="scss">
.nav-bar {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: $--z-index-nav;
}
.navbar {
    position: relative;
    border-bottom: 1px solid $--border-color;
    padding: 0 12px 0 24px;
    height: $--nav-height;
    background-color: $--bg-color;
    white-space: nowrap;
    transition: border-color .5s,background-color .5s;
}
@media (min-width: 520px) {
    .navbar {
        padding: 0 12px 0 32px;
    }
}
@media (min-width: 1280px) {
    .navbar {
        padding: 0 32px;
    }
}
.container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 0 auto;
    max-width: $--screen-max-width;
    height: $--nav-height;

    .logo img{
        height: 24px;
    }

    nav {
        display: flex;
        align-items: center;

        :deep(svg, img) {
            cursor: pointer;
        }
    }
}

.user-icon {
    width: 24px;
    height: 24px;
    border-radius: 50%;
}
:deep(.modal-class) {
    background-color: rgba(255, 255, 255, .8);
}
:deep(.search-dialog) {
    .el-dialog__header {
        margin-right: 0;
        padding: 10px 0 20px 10px;
        .el-button.is-text {
            padding: 0 10px;
        }
    }
    .el-dialog__body {display: none}
    .search-header {
        display: flex;
    }
}

</style>
