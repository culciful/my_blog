<template>
<div class="container a-full a-p-lg">
    <div class="main-container a-h-f">
        <div class="aside a-d-ib a-va-t a-mr-lg a-h-f">
            <div v-if="isVisitMode" class="user-panel a-pb-md a-bb-base">
                <img :src="handleAvatar(userInfo[Constant.avatarUrl])" alt="">
                <span class="a-font-label-1 a-m-h-xxs ellipsis">{{userInfo[Constant.username]}}</span>
                <el-button :type="isFollowing?'info':'primary'" @click="switchFollow" :loading="isQuerying">
                    <svg-icon class="a-mr-xxs" :name="isFollowing?'checkmark':'plus'"></svg-icon>
                    {{isFollowing?$t('label.following'):$t('label.follow')}}
                </el-button>
            </div>
            <p class="a-m-v-sm flex-between a-font-title-1">
                <span>{{$t('label.packageList')}}</span>
                <svg-icon v-if="!isVisitMode"
                          class="a-c-p a-c-h-primary"
                          :title="$t('label.addPackage')"
                          name="add-folder"
                          @click="addPackage"
                          :size="18">
                </svg-icon>
            </p>
            <el-scrollbar :style="isVisitMode?'height: calc(100% - 113px)':'height: calc(100% - 48px)'">
                <div v-for="item in packageList"
                     :key="item[Constant.packageId]"
                     :class="{'selected': item[Constant.packageId]===selectedPackageId}"
                     @click="changePackage(item[Constant.packageId])"
                     class="package-item a-m-v-xs a-c-p flex-between">
                    <span class="a-font-label-1">{{item[Constant.packageName]}}</span>
                    <span v-if="!isVisitMode" class="span-label">
                        <svg-icon class="a-mr-xxs a-c-h-primary"
                                  name="edit-pen"
                                  @click="editPackage(item)"
                                  :title="$t('label.edit')"
                                  :size="18"></svg-icon>
                        <svg-icon class="a-c-h-primary"
                                  name="close"
                                  @click="deletePackage(item)"
                                  :title="$t('label.delete')"
                                  :size="18"></svg-icon>
                    </span>
                </div>
            </el-scrollbar>
        </div>
        <div class="main a-d-ib a-va-t a-h-f">
            <div class="panel">
                <div class="user">
                    <img src="" alt="">
                </div>
                <div class="search">
                    <el-select class="select-package a-mr-sm" v-model="selectedPackageId">
                        <el-option v-for="item in packageList"
                                   :label="item[Constant.packageName]"
                                   :value="item[Constant.packageId]"
                                   :key="item[Constant.packageId]">
                        </el-option>
                    </el-select>
                    <el-input v-model="keyword" @change="changeWord" :placeholder="$t('label.search')" class="a-w-200"></el-input>
                </div>
            </div>
            <el-scrollbar class="a-h-f a-p-h-lg">
                <article-list :package-id="selectedPackageId"
                              :user-id="userInfo[Constant.userId]"
                              :keyword="searchKeyword"
                              :enable-operate="!isVisitMode">
                </article-list>
            </el-scrollbar>
        </div>
    </div>
</div>
</template>

<script lang="ts" setup name="ManageContent">
import {onMounted, getCurrentInstance, ref, reactive} from 'vue';
import Constant, {defaultPackage, handleAvatar} from '@/model/user/constant';
import {useUserStore} from '@/stores/user';
import ArticleList from '@/pages/article/components/list.vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';
import {useRoute} from 'vue-router';

const {query} = useRoute();

const {proxy} = getCurrentInstance();
const { t } = i18n.global as any;
const userStore = useUserStore();

const isVisitMode = ref(false);
let userInfo = reactive({
    [Constant.userId]: userStore.id,
    [Constant.username]: userStore.username,
    [Constant.avatarUrl]: userStore.avatarUrl
});

let keyword = ref('');
let searchKeyword = ref('');
const changeWord = () => {
    searchKeyword.value = keyword.value;
};

const packageList = ref([defaultPackage]);
let selectedPackageId = ref();

const getPackages = () => {
    proxy.$request.post(Constant.url.getPackages, {
        [Constant.userId]: userInfo[Constant.userId]
    }).then((res) => {
        packageList.value = [defaultPackage, ...res.result.list];
    });
};
const changePackage = (id) => {
    selectedPackageId.value = id;
    searchKeyword.value = keyword.value = '';
};
const addPackage = () => {
    ElMessageBox.prompt(t('inputMessage.inputPackageName'), t('label.tip'), {
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.titleFormat')
    }).then(({ value }) => {
        if(packageList.value.some(item => item[Constant.packageName] === value)) {
            ElMessage.error(t('infoMessage.duplicateName'));
            return;
        }
        proxy.$request.post(Constant.url.addPackage, {
            [Constant.userId]: userInfo[Constant.userId],
            [Constant.packageName]: value
        }).then(res => {
            ElMessage({
                type: 'success',
                message: t('infoMessage.addSuccess')
            });
            packageList.value.push({
                [Constant.packageId]: res.result[Constant.packageId],
                [Constant.packageName]: value
            });
        });
    });
};

const editPackage = (item) => {
    ElMessageBox.prompt(t('inputMessage.editPackageName'), t('label.tip'), {
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.invalidInput')
    }).then(({ value }) => {
        proxy.$request.post(Constant.url.editPackage, {
            [Constant.userId]: userInfo[Constant.userId],
            [Constant.packageId]: item[Constant.packageId],
            [Constant.packageName]: value
        }).then(res => {
            ElMessage({
                type: 'success',
                message: t('infoMessage.editSuccess')
            });
            item[Constant.packageName] = value;
        });
    });
};

const deletePackage = (item) => {
    ElMessageBox.confirm(
        t('infoMessage.confirmDeletePackage', {name: item[Constant.packageName]}),
        t('label.tip')
    ).then(() => {
        proxy.$request.post(Constant.url.deletePackage, {
            [Constant.userId]: userInfo[Constant.userId],
            [Constant.packageId]: item[Constant.packageId]
        }).then(() => {
            ElMessage.success(t('infoMessage.deleteSuccess'));
            let index = packageList.value.findIndex(i => i[Constant.packageId] === item[Constant.packageId]);
            packageList.value.splice(index, 1);
        });
    }).catch(() => {});
};

const isFollowing = ref(false);
const isQuerying = ref(false);
const checkFollow = () => {
    proxy.$request.post(Constant.url.checkHasFollow, {
        [Constant.userId]: userInfo[Constant.userId]
    }).then(res => {
        isFollowing.value = res.result.data;
    });
};
const switchFollow = () => {
    isQuerying.value = true;
    proxy.$request.post(Constant.url.switchFollow, {
        [Constant.userId]: userInfo[Constant.userId],
        [Constant.value]: !isFollowing.value
    }).then(res => {
        isFollowing.value = !isFollowing.value;
        isQuerying.value = false;
    }).catch(err => {
        isQuerying.value = false;
    });
};

onMounted(() => {
    isVisitMode.value = !!query?.[Constant.userId];
    selectedPackageId.value = 0;
    if(isVisitMode.value) {
        userInfo[Constant.userId] = query[Constant.userId];
        userInfo[Constant.username] = query[Constant.username];
        userInfo[Constant.avatarUrl] = decodeURIComponent(query[Constant.avatarUrl] as string);
        if(query[Constant.packageId]) selectedPackageId.value = parseInt(query[Constant.packageId] as string);
        checkFollow();
    }
    getPackages();
});
</script>

<style scoped lang="scss">
.main-container {
    margin: 0 auto;
    max-width: $--screen-max-width;
}
.aside {
    width: 320px;
    .user-panel {
        display: flex;
        align-items: center;
        .a-font-label-1 {
            flex-grow: 1;
        }
        img {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            border: 2px solid $--color-white;
        }

    }
    .package-item {
        height: 32px;
        line-height: 32px;
        span {
            padding: 2px 8px;
        }
        .span-label {
            opacity: 0;
        }
        &:first-of-type {
            .span-label {
                display: none !important;
            }
        }
        &.selected {
            background: $--bg-color-page;
            span.a-font-label-1 {
                color: $--color-primary;
                font-weight: 600;
            }
        }
        &:hover {
            background: $--bg-color-page;
            .span-label {
                opacity: 1;
            }
        }
    }
}
.main {
    width: calc(100% - 344px);
    background: $--bg-color;
    .search {
        display: flex;
        justify-content: flex-end;
        margin: 24px 24px 0;
        .select-package {
            display: none;
        }
    }
    .el-scrollbar {
        height: calc(100% - 56px);
    }
}
@media (max-width: 1424px) {
    .aside {
        width: 240px;
    }
    .main {
        width: calc(100% - 264px);
    }
}

@media (max-width: 800px) {
    .container.a-full {
        padding: 0;
    }
    .aside {
        display: none;
    }
    .main {
        width: 100%;
        .search {
            margin: 16px 12px 0;
            .select-package {
                display: inline-flex;
            }
        }
        .el-scrollbar {
            padding: 0 12px;
        }
    }
}
</style>
