<template>
<div class="a-full">
    <custom-header need-handle-search></custom-header>
    <main class="a-full a-pt-header a-bg-gray a-p-h-10">
        <div class="main-container a-p-lg">
            <p class="a-font-title-1">{{$t('label.replyToMe')}}</p>
            <template v-if="total>0">
                <div v-for="item in commentList"
                     :key="item[Constant.commentId]"
                     class="comment-container a-bb-base">
                    <img class="avatar" :src="handleAvatar(item[Constant.member][Constant.avatarUrl])" alt="">
                    <div>
                        <div class="username">
                            <span class="a-c-p a-c-h-primary" @click="viewUser(router, item[Constant.member])">
                                {{item[Constant.member][Constant.username]}}
                            </span>
                            <span class="a-ml-sm">
                                {{item[Constant.parent]?$t('label.replyToMyComment'):$t('label.replyToMyBlog')}}
                            </span>
                        </div>
                        <p v-if="!item[Constant.isMarkdown]">
                            {{item[Constant.content][Constant.msg]}}
                        </p>
                        <v-md-editor
                            v-else
                            :model-value="item[Constant.content][Constant.msg]"
                            mode="preview">
                        </v-md-editor>
                        <div class="source-reply ellipsis a-c-p"
                             @click="router.push('/article/' + item[Constant.articleId])">
                            {{sourceText(item)}}
                        </div>
                        <div class="inline-container">
                            <span>{{transferTimestamp(item[Constant.createTime])}}</span>
                            <el-button link @click="reply(item)">
                                <svg-icon style="margin-right: 2px;" name="comment-filling" size="16"></svg-icon>
                                {{$t('label.reply')}}
                            </el-button>
                        </div>
                    </div>
                    <add-comment v-show="replyToCommentId===item[Constant.commentId]"
                                 :parent-comment="item"
                                 :article-id="item[Constant.articleId]"
                                 :show="replyToCommentId===item[Constant.commentId]"
                                 :author-id="item[Constant.authorId]"
                                 @finish="replyToCommentId=0">
                    </add-comment>
                </div>
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    class="a-mt-lg"
                    hide-on-single-page
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                />
            </template>

        </div>
    </main>
</div>
</template>

<script lang="ts" setup name="MessageIndex">
import {getCurrentInstance, onMounted, ref} from 'vue';
import CustomHeader from '@/components/customHeader/index.vue';
import Constant from '@/model/comment/constant';
import {viewUser, handleAvatar} from '@/model/user/constant';
import {useRouter} from 'vue-router';
import {transferTimestamp} from '@/utils/utils';
import i18n from '@/language/i18n';
import AddComment from '@/pages/article/components/addComment.vue';

const {proxy} = getCurrentInstance();
const router = useRouter();
const { t } = i18n.global as any;

const commentList = ref([]);
const total = ref(0);
const pageSize = ref(20);
const currentPage = ref(1);

const handleSizeChange = (val: number) => {
    pageSize.value = val;
    getByUserId();
};
const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getByUserId();
};

const getByUserId = () => {
    proxy.$request.post(Constant.url.commentsInboxSearch, {
        pageSize: pageSize.value,
        currentPage: currentPage.value,
    }).then(res => {
        commentList.value = res.result.list;
        total.value = res.result.total;
    });
};

// 来源行文案：回复某评论 → 那条评论内容（已删则「评论已删除」）；评论博文 → 文章标题
const sourceText = (item) => {
    if (!item[Constant.parent]) return item[Constant.title];
    const pc = item[Constant.parentContent] || {};
    if (pc[Constant.deleted]) return t('label.commentDeleted');
    return pc[Constant.msg] || item[Constant.title];
};

const replyToCommentId = ref(0);
const reply = (comment) => {
    replyToCommentId.value = comment[Constant.commentId];
};
onMounted(() => {
    getByUserId();
});
</script>

<style scoped lang="scss">
// 见 index.vue：内容超一屏时灰底随内容撑满
main {
    height: auto;
    min-height: 100%;
}
.main-container {
    max-width: 900px;
    margin: 24px auto;
    background: $--bg-color;
}
.comment-container {
    position: relative;
    padding: 24px 0 16px 80px;
    .avatar {
        position: absolute;
        left: 0;
        width: 48px;
        height: 48px;
        margin: 0 16px;
        border-radius: 50%;
    }
    .username {
        margin-bottom: 4px;
        line-height: 30px;
        font-size: $--font-size-base;
    }
}
.source-reply {
    border-left: 2px solid  $--border-color-light;
    padding-left: 4px;
    margin: 8px 0 10px;
    line-height: 16px;
    color: $--text-color-secondary;
    font-size: $--font-size-extra-small;
    &:hover {
        color: $--color-primary;
    }
}
:deep(.vuepress-markdown-body:not(.custom)) {
    padding: 0;
}

.el-pagination {
    justify-content: center;
}
</style>
