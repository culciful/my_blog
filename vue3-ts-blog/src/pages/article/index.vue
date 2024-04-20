<template>
<div class="a-full">
    <custom-header need-handle-search></custom-header>
    <main class="a-full a-pt-header">
        <div class="main-container">
            <div class="main">
                <div class="title">
                    <h1>{{article[ArticleConstant.title]}}</h1>
                    <div class="inline-container">
                        <span class="clickable" @click="viewUser(router, article[ArticleConstant.author])">{{$t('label.author')+': '+article[ArticleConstant.author][ArticleConstant.username]}}</span>
                        <span>{{$t('label.posted')+transferTimestamp(article[ArticleConstant.createTime])}}</span>
                        <span>
                            <svg-icon name="view" size="16"></svg-icon>
                            {{article[ArticleConstant.viewCount]}}
                        </span>
                    </div>
                </div>
                <v-md-editor
                    ref="previewRef"
                    :model-value="article[ArticleConstant.content]"
                    mode="preview">
                </v-md-editor>
                <div class="info">
                    <!--点击进入搜索页面-->
                    <div>
                        <span class="a-font-body-1 span-label a-mr-xs">{{$t('label.tag')+': '}}</span>
                        <el-tag class="a-c-p" v-for="item in article[ArticleConstant.tags]" :key="item" @click="clickTag(item)">{{item}}</el-tag>
                    </div>
                    <!--点击进入个人空间 并显示此文件夹的博文-->
                    <!--用面包屑组件-->
                    <div class="a-mt-xs">
                        <span class="a-font-body-1 span-label a-mr-xs">{{$t('label.package')+': '}}</span>
                        <el-link type="primary" :underline="false" @click="gotoPackage">{{computedPackage[ArticleConstant.packageName]}}</el-link>
                    </div>
                </div>
                <div class="comment">
                    <h3 class="a-font-title-1">{{$t('label.commentList')}}</h3>
                    <div class="a-p-lg a-ta-c login-box a-mt-lg" v-if="!userStore.isLoggedIn">
                        <router-link class="a-c-h-primary" to="/login" >{{$t('inputMessage.loginToComment')}}</router-link>
                    </div>
                    <add-comment v-else @finish="finishComment" :article-id="articleId" :author-id="article[ArticleConstant.author][ArticleConstant.userId]"></add-comment>
                    <template v-if="comments.length>0">
                        <single-comment v-for="item in comments"
                                        @delete="deleteComment"
                                        :comment="item"
                                        :key="item[ArticleConstant.commentId]">
                        </single-comment>
                        <div class="flex-center a-mt-lg" v-if="totalCount>pageSize">
                            <el-pagination
                                v-model:current-page="currentPage"
                                layout="total, prev, pager, next"
                                :total="totalCount"
                                @update:current-page="handleCurrentChange"
                            />
                        </div>
                    </template>
                    <p v-else class="no-comment a-ta-c a-bb-base">{{$t('label.noComment')}}</p>
                </div>
            </div>
            <div class="aside">
                <div class="nav">
                    <p class="tip">{{$t('label.catalogue')}}</p>
                    <ul>
                        <li v-for="anchor in titles"
                            :key="anchor.title"
                            :style="{ paddingLeft: `${anchor.indent * 20}px` }"
                            @click="handleAnchorClick(anchor)">
                            <a class="link ellipsis a-c-p">{{ anchor.title }}</a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </main>
</div>
</template>

<script lang="ts" setup name="Article">
import CustomHeader from '@/components/customHeader/index.vue';
import SingleComment from './components/singleComment.vue';
import AddComment from './components/addComment.vue';
import {computed, getCurrentInstance, nextTick, onMounted, reactive, ref} from 'vue';
import type {Ref} from 'vue';
import ArticleConstant from '@/model/article/constant';
import CommentConstant from '@/model/comment/constant';
import {setReactiveData, transferTimestamp} from '@/utils/utils';
import {useRouter} from 'vue-router';
import {useUserStore} from '@/stores/user';
import i18n from '@/language/i18n';
import {viewUser} from '@/model/user/constant';

const props = defineProps<{
    articleId: number|string
}>();
const { proxy }: any = getCurrentInstance();
const { t } = i18n.global as any;


let article = reactive({
    [ArticleConstant.articleId]: 0,
    [ArticleConstant.author]: {},
    [ArticleConstant.title]: '',
    [ArticleConstant.createTime]: 0,
    [ArticleConstant.viewCount]: 0,
    [ArticleConstant.commentCount]: 0,
    [ArticleConstant.content]: '',
    [ArticleConstant.package]: {},
    [ArticleConstant.tags]: []
});
const computedPackage = computed(() => {
    if(article[ArticleConstant.package] && article[ArticleConstant.package][ArticleConstant.packageId] > 0) {
        return article[ArticleConstant.package];
    }
    else return {
        [ArticleConstant.packageId]: 0,
        [ArticleConstant.packageName]: t('label.all')
    };
});
const getArticleInfo = () => {
    proxy.$request.post(ArticleConstant.url.getArticleInfo, {
        [ArticleConstant.articleId]: props.articleId
    }).then(({result}) => {
        setReactiveData(article, result);
        nextTick(() => {
            getAnchors();
        });
    });
};
const pageSize = 10;
const totalCount = ref(0);
const currentPage = ref(1);
const comments = ref([]);
const getComment = () => {
    proxy.$request.post(CommentConstant.url.getComments, {
        [ArticleConstant.articleId]: props.articleId,
        pageSize,
        currentPage: currentPage.value
    }).then(({result}) => {
        comments.value = result.list;
        totalCount.value = result.total;
    });
};
const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getComment();
};

const userStore = useUserStore();
const finishComment = (comment) => {
    comment[CommentConstant.commentId] = comment[CommentConstant.createTime];
    comment[CommentConstant.member] = {
        [CommentConstant.userId]: userStore.id,
        [CommentConstant.avatarUrl]: userStore.avatarUrl,
        [CommentConstant.username]: userStore.username
    };
    comments.value.unshift(comment);
};
const deleteComment = (commentId) => {
    const index = comments.value.findIndex(item => item[CommentConstant.commentId] === commentId);
    comments.value.splice(index, 1);
};
const router = useRouter();

const gotoPackage = () => {
    const data = Object.assign(article[ArticleConstant.author], article[ArticleConstant.package]);
    viewUser(router, data);
};
const clickTag = (tag) => {
    router.push({path: '/index', query: {tag}});
};

interface anchor {
    title: string,
    lineIndex: string,
    indent:number
}
const titles: Ref<[anchor]> = ref([]);
const previewRef = ref();
const getAnchors = () => {
    if(!previewRef.value) return;
    const anchors = previewRef.value.$el.querySelectorAll('h1,h2,h3,h4,h5,h6');
    const tempTitles = Array.from(anchors).filter((title) => !!title.innerText.trim());

    if (!tempTitles.length) {
        titles.value = [];
        return;
    }

    const hTags = Array.from(new Set(tempTitles.map((title) => title.tagName))).sort();

    titles.value = tempTitles.map((el) => ({
        title: el.innerText,
        lineIndex: el.getAttribute('data-v-md-line'),
        indent: hTags.indexOf(el.tagName)
    }));
};

const handleAnchorClick = (anchor) => {
    const preview = previewRef.value;
    const { lineIndex } = anchor;

    const heading = preview.$el.querySelector(`[data-v-md-line="${lineIndex}"]`);

    if (heading) {
        window.scrollTo({
            /* title高度128 */
            top: heading.offsetTop - 20 + 128,
            behavior: 'smooth'
        });
    }
};
onMounted(() => {
    getArticleInfo();
    getComment();
});
</script>

<style scoped lang="scss">
.main-container {
    display: flex;
    justify-content: center;
    .main {
        width: 900px;
        padding-bottom: 96px;
        .title {
            padding: 2rem 2.5rem 0;
            h1 {
                margin-bottom: 0.8em;
                padding-left: 10px;
                border-left: 5px solid $--color-theme;
                font-size: 2.2rem;
                font-weight: 600;
            }
        }

        .info {
            padding: 1rem 2.5rem 2.5rem;;
            span, a {
                vertical-align: top;
            }
        }
        
        .comment {
            padding: 2rem 0;
            border-top: $--border;
            .no-comment {
                padding: 80px 0 24px;
            }
            .login-box {
                border: 1px dashed $--border-color;
                border-radius: 8px;
            }
        }
    }
    .aside {
        position: relative;
        display: none;
        width: 320px;
        padding-left: 64px;
        padding-top: 32px;
        flex-shrink: 0;
        font-size: 13px;
        background: $--bg-color;
        .nav {
            position: sticky;
            width: 224px;
            top: ($--nav-height + 24px);
            bottom: 0;
        }
        .tip {
            margin-bottom: 4px;
            font-size: 11px;
            font-weight: 700;
        }
        .link {
            display: block;
            line-height: 28px;
            color: $--text-color-light;
            &:hover {
                color: $--text-color;
            }
        }
    }
    @media (min-width: 1280px) {
        & {
            margin-left: 320px;
        }
        .aside {
            display: block;
        }
    }
    @media (min-width: 1440px) {
        .aside {
            padding-left: 96px;
        }
    }
}

</style>
