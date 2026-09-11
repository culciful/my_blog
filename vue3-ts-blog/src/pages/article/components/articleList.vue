<template>
    <template v-if="totalCount>0">
        <div class="article-container">
            <article class="a-mt-md a-pb-sm" v-for="item in articleList" :key="item[ArticleConstant.articleId]">
                <div class="content">
                    <h3 class="a-font-title-1 a-pos-r a-pr-lg">
                        <router-link :to="`${isDraft ? '/draft/' : '/article/'}${item[ArticleConstant.articleId]}`">{{item[ArticleConstant.title]}}</router-link>
                        <el-dropdown placement="bottom-end" class="more" v-if="enableOperate">
                            <svg-icon name="more" size="16"></svg-icon>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-for="(value, key) in operateOptions" :key="key" @click="()=>{value(item[ArticleConstant.articleId])}">
                                        {{$t('label.'+key)}}
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </h3>
                    <p v-if="item[ArticleConstant.abstract]" @click="viewArticle(item[ArticleConstant.articleId])" class="a-font-body-1 a-c-p a-m-v-xs">{{item[ArticleConstant.abstract]}}</p>
                    <footer class="inline-container">
                        <span class="clickable" @click="item[ArticleConstant.author] && viewUser(router, item[ArticleConstant.author])">{{$t('label.author')+': '+authorName(item)}}</span>
                        <span class="create-time">{{transferTimestamp(item[ArticleConstant.createTime])}}</span>
                        <span>
                            <svg-icon name="comment-filling" size="16"></svg-icon>
                            {{item[ArticleConstant.commentCount] ?? 0}}
                        </span>
                        <span>
                            <svg-icon name="view" size="16"></svg-icon>
                            {{item[ArticleConstant.viewCount] ?? 0}}
                        </span>
                    </footer>
                </div>
                <img v-if="item[ArticleConstant.coverUrl]"
                     class="cover a-c-p"
                     :src="item[ArticleConstant.coverUrl]"
                     alt=""
                     @click="viewArticle(item[ArticleConstant.articleId])">
            </article>
        </div>
        <div class="flex-center a-bt-base a-pt-lg" v-if="totalCount>pageSize">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalCount"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
        </div>
    </template>
    <template v-else>
        <p class="a-font-body-1 a-m-lg a-ta-c">{{$t('label.noContent')}}</p>
    </template>
</template>

<script lang="ts" setup name="ArticleList">
import {transferTimestamp} from '@/utils/utils';
import {computed, getCurrentInstance, onMounted, ref, watch} from 'vue';
import ArticleConstant, {DRAFT_PID} from '@/model/article/constant';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';
import UserConstant, {viewUser} from '@/model/user/constant';

const props = defineProps<{
    keyword?: string,
    tag?: string,
    userId?: number|string,
    packageId?: number|string
    enableOperate?: boolean
}>();

const { proxy }: any = getCurrentInstance();
const router = useRouter();
const { t } = i18n.global as any;

const pageSize = ref(10);
const totalCount = ref(0);
const currentPage = ref(1);
let articleList = ref([]);

// 选中「草稿箱」时整个列表切成草稿：换接口 + 链接指向 /draft/:id
const isDraft = computed(() => props.packageId === DRAFT_PID);

// 作者账号可能已经注销（后端 memberCard 仍会把历史 username 带回来，但注销了就不拿真名show，
// 统一显示「该用户已注销」；点击还是能跳去 ta 的内容管理页看历史文章，member.id 还在）
const authorName = (item) => {
    const member = item[ArticleConstant.author];
    if (!member) return '';
    if (member[UserConstant.isDeleted]) return t('label.userDeactivated');
    return member[UserConstant.username] ?? '';
};

const handleSizeChange = (val: number) => {
    pageSize.value = val;
    getArticleList();
};
const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getArticleList();
};
const viewArticle = (articleId) => {
    router.push(`${isDraft.value ? '/draft/' : '/article/'}${articleId}`);
};

// 列表组件自己负责首次加载：靠父组件 prop 变化触发太脆弱
// （切到「草稿箱」再切回来时本组件会重新挂载，prop 值没变 → watch 不触发 → 空白）
onMounted(() => getArticleList());
watch([() => props.keyword, () => props.tag, () => props.packageId, () => props.userId], () => {
    currentPage.value = 1;
    getArticleList();
});

const getArticleList = () => {
    const url = isDraft.value ? ArticleConstant.url.getDraftList : ArticleConstant.url.getArticleList;
    // 草稿列表后端按当前登录用户作用域，不吃 id/pid/tag，只吃关键词
    const filter = isDraft.value
        ? { [ArticleConstant.keyword]: props.keyword }
        : {
            [ArticleConstant.keyword]: props.keyword,
            [ArticleConstant.userId]: props.userId,
            [ArticleConstant.packageId]: props.packageId,
            [ArticleConstant.tag]: props.tag,
        };
    proxy.$request.post(url, {
        pageSize: pageSize.value,
        currentPage: currentPage.value,
        filter
    }).then( ({result}) => {
        articleList.value = result.list;
        totalCount.value = result.total;
    });
};

const editHandler = (articleId) => {
    router.push(`${isDraft.value ? '/draft/' : '/edit/'}${articleId}`);
};
const deleteHandler = (articleId) => {
    ElMessageBox.confirm(
        t('infoMessage.confirmDeleteArticle'),
        t('label.tip')
    ).then(() => {
        proxy.$request.post(ArticleConstant.url.delete, { [ArticleConstant.articleId]: articleId }).then(() => {
            ElMessage.success(t('infoMessage.deleteSuccess'));
            currentPage.value = 1;
            getArticleList();
        });
    }).catch(() => {});
};
const operateOptions = {
    edit: editHandler,
    delete: deleteHandler
};
</script>

<style scoped lang="scss">
.article-container {
    article {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 16px;
        border-bottom: $--border;
        .content {
            flex: 1;
            min-width: 0;
        }
        h3 {
            a {
                color: $--text-color;
            }
            a:hover {
                color: $--color-primary-dark-2;
            }
        }
        .cover {
            flex-shrink: 0;
            width: 120px;
            height: 80px;
            object-fit: cover;
            border-radius: 6px;
            background: $--bg-color-soft;
        }
    }
}
.more {
    position: absolute;
    right: 0;
    top: 0;
}
@media (max-width: 480px) {
    .article-container article .cover {
        width: 72px;
        height: 72px;
    }
}
</style>
