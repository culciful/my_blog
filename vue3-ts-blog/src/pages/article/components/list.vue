<template>
    <template v-if="totalCount>0">
        <div class="article-container">
            <article class="a-mt-md a-pb-sm" v-for="item in articleList" :key="item[ArticleConstant.articleId]">
                <h3 class="a-font-title-1 a-pos-r a-pr-lg">
                    <router-link :to="`/article/${item[ArticleConstant.articleId]}`">{{item[ArticleConstant.title]}}</router-link>
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
                <p @click="viewArticle(item[ArticleConstant.articleId])" class="a-font-body-1 a-c-p a-m-v-xs">{{item[ArticleConstant.abstract] + ' ...'}}</p>
                <footer class="inline-container">
                    <span class="clickable" @click="viewUser(router, item[ArticleConstant.author])">{{$t('label.author')+': '+item[ArticleConstant.author][ArticleConstant.username]}}</span>
                    <span class="create-time">{{transferTimestamp(item[ArticleConstant.createTime])}}</span>
                    <span>
                        <svg-icon name="comment-filling" size="16"></svg-icon>
                        {{item[ArticleConstant.commentCount]}}
                    </span>
                    <span>
                        <svg-icon name="view" size="16"></svg-icon>
                        {{item[ArticleConstant.viewCount]}}
                    </span>
                </footer>
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
        <p class="a-font-body-1 a-m-lg a-ta-c">{{$t('label.noArticle')}}</p>
    </template>
</template>

<script lang="ts" setup name="ArticleList">
import {transferTimestamp} from '@/utils/utils';
import {getCurrentInstance, ref, watch} from 'vue';
import ArticleConstant from '@/model/article/constant';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';
import {viewUser} from '@/model/user/constant';

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

const handleSizeChange = (val: number) => {
    pageSize.value = val;
    getArticleList();
};
const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getArticleList();
};
const viewArticle = (articleId) => {
    router.push(`/article/${articleId}`);
};

watch([() => props.keyword, () => props.tag, () => props.packageId], (val) => {
    currentPage.value = 1;
    getArticleList();
});

const getArticleList = () => {
    proxy.$request.post(ArticleConstant.url.getArticleList, {
        pageSize: pageSize.value,
        currentPage: currentPage.value,
        filter: {
            [ArticleConstant.keyword]: props.keyword,
            [ArticleConstant.userId]: props.userId,
            [ArticleConstant.packageId]: props.packageId,
            [ArticleConstant.tag]: props.tag,
        }
    }).then( ({result}) => {
        articleList.value = result.list;
        totalCount.value = result.total;
    });
};

const editHandler = (articleId) => {
    router.push(`/edit/${articleId}`);
};
const deleteHandler = (articleId) => {
    ElMessageBox.confirm(
        t('infoMessage.confirmDeleteArticle'),
        t('label.tip')
    ).then(() => {
        proxy.$request.post(ArticleConstant.url.delete, {
            [ArticleConstant.articleId]: articleId
        }).then(() => {
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
        border-bottom: $--border;
        h3 {
            a {
                color: $--text-color;
            }
            a:hover {
                color: $--color-primary-dark-2;
            }
        }

        &:last-child {
            border: none;
        }
    }
}
.more {
    position: absolute;
    right: 0;
    top: 0;
}
</style>
