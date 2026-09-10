<template>
<div class="a-full">
    <custom-header need-handle-search></custom-header>
    <main class="a-full a-pt-header">
        <div class="main-container">
            <div class="main">
                <el-scrollbar ref="contentScrollRef" always>
                <div class="title">
                    <h1>{{article[ArticleConstant.title]}}</h1>
                    <div class="inline-container">
                        <span class="clickable" @click="viewUser(router, article[ArticleConstant.author])">{{$t('label.author')+': '+article[ArticleConstant.author][ArticleConstant.username]}}</span>
                        <span>{{$t('label.posted')+transferTimestamp(article[ArticleConstant.createTime])}}</span>
                        <span>
                            <svg-icon name="view" size="16"></svg-icon>
                            {{article[ArticleConstant.viewCount]}}
                        </span>
                        <el-dropdown v-if="isAuthor" placement="bottom-end" trigger="click" class="article-more">
                            <svg-icon name="more" size="16"></svg-icon>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-for="(fn, key) in articleOptions" :key="key" @click="fn">
                                        {{$t('label.'+key)}}
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
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
                    <div v-if="isEdited" class="a-mt-xs a-font-body-1">
                        {{$t('label.editedAt')+transferTimestamp(article[ArticleConstant.updateTime])}}
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
                </el-scrollbar>
            </div>
            <div class="aside">
                <el-scrollbar>
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
                </el-scrollbar>
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
import {ElMessage, ElMessageBox} from 'element-plus';
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
    [ArticleConstant.updateTime]: 0,
    [ArticleConstant.viewCount]: 0,
    [ArticleConstant.commentCount]: 0,
    [ArticleConstant.content]: '',
    [ArticleConstant.package]: {},
    [ArticleConstant.tags]: []
});
// 编辑时间比发布时间晚 60s 以上，视为「编辑过」
const isEdited = computed(() =>
    Number(article[ArticleConstant.updateTime]) - Number(article[ArticleConstant.createTime]) > 60
);
const isAuthor = computed(() =>
    userStore.isLoggedIn && String(article[ArticleConstant.author]?.[ArticleConstant.userId]) === String(userStore.id)
);
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
    proxy.$request.get(ArticleConstant.url.getArticleInfo, { [ArticleConstant.articleId]: props.articleId }).then(({result}) => {
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
    proxy.$request.post(CommentConstant.url.commentsByArticleSearch, {
        [CommentConstant.articleId]: props.articleId,
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
    comment[CommentConstant.commentId] = comment[CommentConstant.commentId] || comment[CommentConstant.createTime];
    comment[CommentConstant.canEdit] = true;   // 刚发的评论：本人、窗口内、无回复
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

const editArticle = () => router.push('/edit/' + props.articleId);
const deleteArticle = () => {
    ElMessageBox.confirm(t('infoMessage.confirmDeleteArticle'), t('label.tip')).then(() => {
        proxy.$request.post(ArticleConstant.url.delete, {
            [ArticleConstant.articleId]: props.articleId
        }).then(() => {
            ElMessage.success(t('infoMessage.deleteSuccess'));
            router.push('/');
        });
    }).catch(() => {});
};
const articleOptions = { edit: editArticle, delete: deleteArticle };

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
const contentScrollRef = ref();
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
    const wrap = contentScrollRef.value?.wrapRef;   // el-scrollbar 的滚动容器

    if (heading && wrap) {
        const delta = heading.getBoundingClientRect().top - wrap.getBoundingClientRect().top;
        contentScrollRef.value.scrollTo({
            top: wrap.scrollTop + delta - 20,
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
// 让正文自己滚（滚动条落在正文列右侧，不在窗口最右），头部和目录不跟着动。
// a-full / main 都夹死 overflow，保证唯一能滚的是里面的 el-scrollbar，不会出现「窗口 + 内容」两条滚动条
.a-full {
    overflow: hidden;
}
main {
    overflow: hidden;
}
.main-container {
    display: flex;
    justify-content: center;
    height: 100%;
    position: relative;   // 目录绝对定位的锚点
    .main {
        width: 100%;
        max-width: 900px;   // 大屏封顶 900，窄屏跟着收
        min-width: 0;        // 允许 flex 压缩到内容宽度以下（否则代码块会把它撑住）
        height: 100%;
        // 正文用 el-scrollbar 撑满这一列；padding-right 让滚动条离正文一段距离
        padding-right: 16px;
        :deep(.el-scrollbar) {
            height: 100%;
        }
        :deep(.el-scrollbar__view) {
            padding-right: 8px;
            padding-bottom: 96px;
        }
        // v-md-editor 预览态内部还套了一层自己的 scrollbar 组件。某些情况下它自己会拿到
        // 固定高度、内部滚动（标题不动、正文单独滚）+ 渲染一条满高灰条 → 就是「两条滚动条」。
        // 容器结构留着，强制它 auto 高、不滚、藏掉 bar，只让外面这个 el-scrollbar 滚整篇。
        :deep(.v-md-editor--preview) {
            height: auto !important;
        }
        :deep(.v-md-editor__preview-wrapper),
        :deep(.v-md-editor__preview-wrapper .scrollbar__wrap) {
            height: auto !important;
            overflow: visible !important;
            // 上面把默认的 overflow:hidden 摘了 → flex item 的自动最小尺寸不再是 0，
            // 会被最宽的不可折行内容（代码块 / KaTeX nowrap 公式）撑破这一列。补回 min-width:0。
            min-width: 0 !important;
        }
        :deep(.v-md-editor__preview-wrapper .scrollbar__bar) {
            display: none;
        }
        // KaTeX 块级公式：katex.min.css 只给了 text-align:center，没给 overflow。
        // 宽公式会顶破列宽（连带整篇不居中）。这里补上：超宽时公式自己横向滚，不撑正文。
        :deep(.vuepress-markdown-body .katex-display) {
            overflow-x: auto;
            overflow-y: hidden;
            padding: 2px 0;
        }
        // 正文滚动条加粗一点，好点
        :deep(.el-scrollbar__bar.is-vertical) {
            width: 10px;
            .el-scrollbar__thumb {
                background-color: rgba(0, 0, 0, 0.24);
                &:hover {
                    background-color: rgba(0, 0, 0, 0.36);
                }
            }
        }
        .title {
            padding: 32px 40px 0;
            h1 {
                margin-bottom: 28px;
                padding-left: 10px;
                border-left: 5px solid $--color-theme;
                font-size: 35px;
                font-weight: 600;
            }
        }

        .info {
            padding: 16px 40px 40px;
            span, a {
                vertical-align: top;
            }
        }

        .article-more {
            cursor: pointer;
            color: $--text-color-secondary;
            outline: none;
            &:hover { color: $--color-primary; }
        }

        .comment {
            padding: 32px 0;
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
        // 绝对定位钉在右侧：正文由 justify-content 居中在视口，目录浮在右边空白里
        display: none;
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        width: 240px;
        padding: 24px;   // 滚动条别贴窗口边
        font-size: 13px;
        background: $--bg-color;
        // 目录用 el-scrollbar 撑满，比视口高时自己滚
        :deep(.el-scrollbar) {
            height: 100%;
        }
        :deep(.el-scrollbar__bar.is-vertical) {
            width: 8px;
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
    // 窄屏：正文两侧留点边距，别贴到窗口
    @media (max-width: 768px) {
        .main :deep(.el-scrollbar__view) {
            padding-left: 12px;
        }
        .main .title,
        .main .info {
            padding-left: 12px;
            padding-right: 12px;
        }
    }
    // ≥1280：两侧对称留出 240 的槽（正文仍视口居中），右槽放绝对定位的目录，不会压正文
    @media (min-width: 1280px) {
        padding: 0 240px;
        .aside {
            display: block;
        }
    }
}

</style>
