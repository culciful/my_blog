<template>
<div class="container a-full a-p-lg">
    <p v-if="userNotFound" class="a-ta-c a-m-lg a-font-body-1">{{$t('label.userNotFound')}}</p>
    <div v-else class="main-container a-h-f">
        <div class="aside a-d-ib a-va-t a-mr-lg a-h-f">
            <div v-if="isVisitMode" class="user-panel a-pb-md a-bb-base">
                <img :src="handleAvatar(userInfo[Constant.avatarUrl])" alt="">
                <span class="a-font-label-1 a-m-h-xxs ellipsis">
                    {{isTargetDeleted ? $t('label.userDeactivated') : userInfo[Constant.username]}}
                </span>
                <!-- 注销账号不能被新关注（后端也拦了）；已经关注的还能取消，方便清理历史关注关系 -->
                <el-button v-if="!isTargetDeleted || isFollowing"
                           :type="isFollowing?'info':'primary'"
                           @click="switchFollow"
                           :loading="isQuerying">
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
                <div v-if="!isVisitMode"
                     :class="{'selected': selectedPackageId===DRAFT_PID}"
                     @click="changePackage(DRAFT_PID)"
                     class="package-item a-m-v-xs a-c-p flex-between">
                    <span class="a-font-label-1">{{$t('label.draftBox')}}</span>
                </div>
                <div v-for="item in packageList"
                     :key="item[Constant.packageId]"
                     :class="{'selected': item[Constant.packageId]===selectedPackageId}"
                     @click="changePackage(item[Constant.packageId])"
                     class="package-item a-m-v-xs a-c-p flex-between">
                    <span class="a-font-label-1">{{item[Constant.packageName]}}</span>
                    <span v-if="!isVisitMode && item[Constant.packageId] !== 0" class="span-label">
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
                <div class="search flex-end">
                    <el-select class="select-package a-mr-sm" v-model="selectedPackageId">
                        <el-option v-if="!isVisitMode"
                                   :label="$t('label.draftBox')"
                                   :value="DRAFT_PID">
                        </el-option>
                        <el-option v-for="item in packageList"
                                   :label="item[Constant.packageName]"
                                   :value="item[Constant.packageId]"
                                   :key="item[Constant.packageId]">
                        </el-option>
                    </el-select>
                    <el-input v-model="keyword" @change="changeWord" maxlength="64" :placeholder="$t('label.search')" class="a-w-200"></el-input>
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
import ArticleList from '@/pages/article/components/articleList.vue';
import {DRAFT_PID} from '@/model/article/constant';
import {ElMessage, ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';
import {useRoute} from 'vue-router';
import {LOGIN_STATE} from '@/utils/localStoreItem';

const {query} = useRoute();

const {proxy} = getCurrentInstance();
const { t } = i18n.global as any;
const userStore = useUserStore();

const isVisitMode = ref(false);
const userNotFound = ref(false);
const isTargetDeleted = ref(false);
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

// 「草稿箱」不进 packageList（那数组还要喂 article-list / :class / el-option，塞个假分组太脆）
// 而是在左栏和移动端下拉框里各自单独渲染一条，排在最前；选中它 = selectedPackageId === DRAFT_PID
const packageList = ref<Array<Record<string, any>>>([defaultPackage]);
let selectedPackageId = ref<number | string>(0);

const getPackages = () => {
    proxy.$request.get(Constant.url.getPackages, { [Constant.userId]: userInfo[Constant.userId] }).then((res) => {
        packageList.value = [defaultPackage, ...res.result.list];
    });
};
const changePackage = (id) => {
    selectedPackageId.value = id;
    searchKeyword.value = keyword.value = '';
};
const addPackage = () => {
    // ElMessageBox 没有 inputAttrs 选项，长度只能靠 inputPattern 卡（1-64 字、非空格开头）
    ElMessageBox.prompt(t('inputMessage.inputPackageName'), t('label.tip'), {
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.packageNameLimit')
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
        inputValue: item[Constant.packageName],
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.packageNameLimit')
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
// 也用来罩住"初始关注状态还没查回来"这段时间：checkFollow 是 onMounted 里 fire-and-forget
// 发出去的（不 await，不挡 getPackages），按钮渲染出来早于这个请求落地。真实点了一下确实
// 复现过：这段时间内点关注，isFollowing 还是默认值 false，switchFollow 拿着这个旧值算
// shouldFollow；如果 checkFollow 的响应比 switchFollow 晚回来，还会把刚切换好的状态覆盖回去
// ——是真会发生的竞态，不是测试假象（真实用户手速慢，窗口期很窄不容易踩到；但自动化测试
// 网络几乎零延迟，两个请求前后脚发出，稳定复现）。loading 状态把按钮罩住，状态没落地之前
// 不能点。
const isQuerying = ref(false);
const checkFollow = () => {
    isQuerying.value = true;
    proxy.$request.get(Constant.url.checkHasFollow, { [Constant.userId]: userInfo[Constant.userId] }).then(res => {
        isFollowing.value = res.result.isFollowing;
    }).finally(() => {
        isQuerying.value = false;
    });
};
const switchFollow = () => {
    isQuerying.value = true;
    proxy.$request.post(Constant.url.switchFollow, {
        [Constant.userId]: userInfo[Constant.userId],
        [Constant.shouldFollow]: !isFollowing.value
    }).then(res => {
        isFollowing.value = !isFollowing.value;
        isQuerying.value = false;
    }).catch(err => {
        isQuerying.value = false;
    });
};

onMounted(() => {
    void (async () => {
        const raw = query[Constant.userId] as string | string[] | undefined;
        // 雪花 ID 超出 JS Number 安全范围，保持字符串，勿转 Number
        const queriedId = (Array.isArray(raw) ? raw[0] : raw) ?? '';
        // 带 ?id= 且不是自己 → 访客视角；是自己（从自己文章的合集链接点进来）→ 当作自己的内容管理
        isVisitMode.value = !!queriedId && String(queriedId) !== String(userStore.id);

        // pid 不管访客还是自己都尊重（合集链接带过来的）；草稿箱只有自己能进
        const rawPid = query[Constant.packageId] as string | string[] | undefined;
        const pidStr = Array.isArray(rawPid) ? rawPid[0] : rawPid;
        const validPid = pidStr != null && pidStr !== '' && pidStr !== '0'
            && !(pidStr === DRAFT_PID && isVisitMode.value);
        selectedPackageId.value = validPid ? pidStr : 0;

        if (isVisitMode.value) {
            userInfo[Constant.userId] = queriedId;
            // 同步地、在任何 await 之前就把关注按钮罩住：checkFollow() 本身在下面 await
            // getUserInfo() 之后才会被调用，光靠 checkFollow 内部设 isQuerying=true 关不住
            // "组件刚挂载、getUserInfo 还没返回"这段窗口期——这段时间按钮已经渲染成可点的
            // 默认态（isFollowing 的初始值 false），手速快或者自动化测试点这个窗口就会拿着
            // 过期的 isFollowing 值算 shouldFollow，off-by-one 拍不准。
            if (localStorage.getItem(LOGIN_STATE)) {
                isQuerying.value = true;
            }
            try {
                // 已注销的用户 getUserInfo 也查得到（isDeleted:true），只有 id 压根不存在才会 404 到 catch；
                // 历史文章/合集仍然可以浏览，所以这里不能把「已注销」和「真不存在」当同一种情况处理
                const res: { result: Record<string, unknown> } = await proxy.$request.get(
                    Constant.url.getUserInfo,
                    { [Constant.userId]: userInfo[Constant.userId] }
                );
                const r = res.result;
                userInfo[Constant.username] = r[Constant.username] as string;
                userInfo[Constant.avatarUrl] = r[Constant.avatarUrl] as string;
                isTargetDeleted.value = !!r[Constant.isDeleted];
            } catch {
                // id 真的查不到人：整页换成提示，不再往下拉 package/article/关注状态。
                // 走不到 checkFollow 了，上面提前设的 isQuerying 也要解开，不然永远卡 loading
                isQuerying.value = false;
                userNotFound.value = true;
                return;
            }
            if (localStorage.getItem(LOGIN_STATE)) {
                checkFollow();
            }
        }
        getPackages();
    })();
});
</script>

<style scoped lang="scss">
.main-container {
    margin: 0 auto;
    max-width: $--screen-max-width;
}
.aside {
    width: 320px;
    // 给内容右侧留白，编辑/删除图标不贴着滚动条
    :deep(.el-scrollbar__view) {
        padding-right: 10px;
    }
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
        margin: 24px 24px 0;
        .select-package {
            display: none;   // 桌面端用左侧 .aside 分组栏；这个下拉框只在 ≤800px 手机端出现
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
