<template>
<div class="comment-container">
    <img class="avatar" :src="handleAvatar(comment[Constant.member][Constant.avatarUrl])" alt="">
    <div class="content-container">
        <div class="root a-pos-r">
            <p class="username a-c-p a-c-h-lighter" @click="viewUser(router, comment[Constant.member])">{{comment[Constant.member][Constant.username]}}</p>
            <p v-if="!comment[Constant.useMD]">
                {{comment[Constant.content][Constant.msg]}}
            </p>
            <v-md-editor
                v-else
                :model-value="comment[Constant.content][Constant.msg]"
                mode="preview">
            </v-md-editor>
            <div class="inline-container info">
                <span>{{transferTimestamp(comment[Constant.createTime])}}</span>
                <span v-if="isEdited(comment)" class="edited-mark">{{$t('label.edited')}}</span>
                <el-button link
                           size="small"
                           class="clickable"
                           :disabled="!userStore.isLoggedIn"
                           @click="reply(comment)">
                    {{$t('label.reply')}}
                </el-button>
            </div>
            <el-dropdown placement="bottom-end" class="more" v-if="comment[Constant.member][Constant.userId]===userStore.id">
                <svg-icon name="more" size="16"></svg-icon>
                <template #dropdown>
                    <el-dropdown-menu >
                        <el-dropdown-item v-for="(value, key) in optionsFor(comment)" :key="key" @click="()=>{value(comment[Constant.commentId], comment)}">
                            {{$t('label.'+key)}}
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
        <div class="comment-container child-comment"
             v-for="child in childCommentList"
             :key="child[Constant.commentId]">
            <img class="avatar" :src="handleAvatar(child[Constant.member][Constant.avatarUrl])" alt="">
            <div class="root a-pos-r">
                <p class="username">
                    <span class="a-c-p a-c-h-lighter" @click="viewUser(router, child[Constant.member])">
                        {{child[Constant.member][Constant.username]}}
                    </span>
                    <span class="a-ml-sm" v-if="child[Constant.content][Constant.member]">
                        {{$t('label.replyTo')}}
                        <span class="a-c-primary a-c-p">@{{child[Constant.content][Constant.member][Constant.username]}}</span>
                        ：
                    </span>
                </p>
                <p v-if="!child[Constant.useMD]">
                    {{child[Constant.content][Constant.msg]}}
                </p>
                <v-md-editor
                    v-else
                    :model-value="child[Constant.content][Constant.msg]"
                    mode="preview">
                </v-md-editor>
                <div class="inline-container info">
                    <span>{{transferTimestamp(child[Constant.createTime])}}</span>
                    <span v-if="isEdited(child)" class="edited-mark">{{$t('label.edited')}}</span>
                    <el-button link
                               size="small"
                               class="clickable"
                               :disabled="!userStore.isLoggedIn"
                               @click="reply(child)">
                        {{$t('label.reply')}}
                    </el-button>
                </div>
                <el-dropdown placement="bottom-end" class="more" v-if="child[Constant.member][Constant.userId]===userStore.id">
                    <svg-icon name="more" size="16"></svg-icon>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item v-for="(value, key) in optionsFor(child)" :key="key" @click="($event)=>{value(child[Constant.commentId], child)}">
                                {{$t('label.'+key)}}
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
        <div v-if="!expanded && childTotal > childCommentList.length"
             class="expand-replies">
            <el-button link @click="expandChildren">
                {{$t('label.expandReplies', { count: childTotal - childCommentList.length })}}
            </el-button>
        </div>
        <el-pagination
            v-if="expanded"
            v-model:current-page="currentPage"
            hide-on-single-page
            layout="total, prev, pager, next"
            :total="childTotal"
            @update:current-page="handleCurrentChange"
        />
        <add-comment v-show="showAddComment"
                     :mode="mode"
                     :edit-comment="newComment"
                     :parent-comment="parentComment"
                     :article-id="comment[Constant.articleId]"
                     :show="showAddComment"
                     :author-id="comment[Constant.authorId]"
                     @finish="finishComment">
        </add-comment>
    </div>
</div>
</template>

<script lang="ts" setup name="SingleComment">
import AddComment from './addComment.vue';
import Constant from '@/model/comment/constant';
import {transferTimestamp} from '@/utils/utils';
import {useUserStore} from '@/stores/user';
import {getCurrentInstance, onMounted, reactive, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';
import {viewUser, handleAvatar} from '@/model/user/constant';
import {useRouter} from 'vue-router';

const userStore = useUserStore();
const router = useRouter();
const { t } = i18n.global as any;
const { proxy }: any = getCurrentInstance();
const props = defineProps({
    comment: {
        type: Object,
        required: true,
        default: function () {
            return {
                [Constant.commentId]: 0,
                [Constant.createTime]: 0,
                [Constant.content]: {
                    [Constant.msg]: '',
                    [Constant.member]: undefined
                },
                [Constant.member]: {
                    [Constant.userId]: 0,
                    [Constant.username]: '',
                    [Constant.avatarUrl]: ''
                },
                [Constant.root]: undefined,
                [Constant.commentList]: []
            };
        }
    }
});
const emit = defineEmits(['delete']);

const pageSize = 10;
const childTotal = ref(0);
const currentPage = ref(1);
const childCommentList = ref([]);
const expanded = ref(false);
// 「展开 N 条回复」：拉全量（分页），此后子回复走完整列表
const expandChildren = () => {
    expanded.value = true;
    currentPage.value = 1;
    getChildComment();
};
const getChildComment = () => {
    proxy.$request.post(Constant.url.commentsByArticleSearch, {
        [Constant.articleId]: props.comment[Constant.articleId],
        [Constant.root]: props.comment?.[Constant.commentId],
        pageSize,
        currentPage: currentPage.value
    }).then(({result}) => {
        childCommentList.value = result.list;
        childTotal.value = result.total;
    });
};
const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getChildComment();
};

const showAddComment = ref(false);
const mode = ref('');
let newComment = reactive({});
let parentComment = reactive({});
const editHandler = (id, comment) => {
    if(showAddComment.value === true) {
        showAddComment.value = false;
        return;
    }
    mode.value = 'edit';
    newComment = comment;
    showAddComment.value = true;
};
const deleteHandler = (id, row) => {
    ElMessageBox.confirm(
        t('infoMessage.confirmDeleteComment'),
        t('label.tip')
    ).then(() => {
        proxy.$request.post(Constant.url.deleteComment, {
            [Constant.articleId]: row[Constant.articleId],
            [Constant.commentId]: id
        }).then(() => {
            ElMessage.success(t('infoMessage.deleteSuccess'));
            if(id === props.comment?.[Constant.commentId]) {
                emit('delete', id);
            } else {
                const index = childCommentList.value.findIndex(item => item[Constant.commentId] === id);
                if (index > -1) childCommentList.value.splice(index, 1);
                childTotal.value = Math.max(0, childTotal.value - 1);
            }
        });
    }).catch(() => {});
};
// 仅当后端标记 canEdit（本人 + 发布5分钟内 + 无回复）才给「编辑」，「删除」始终有
const optionsFor = (c) =>
    c?.[Constant.canEdit]
        ? { edit: editHandler, delete: deleteHandler }
        : { delete: deleteHandler };
const reply = (comment) => {
    if(showAddComment.value === true) {
        showAddComment.value = false;
        return;
    }
    mode.value = 'add';
    parentComment = comment;
    showAddComment.value = true;
};
const finishComment = (params) => {
    showAddComment.value = false;
    if(mode.value === 'edit') {
        newComment[Constant.useMD] = params[Constant.useMD];
        newComment[Constant.content][Constant.msg] = params[Constant.content][Constant.msg];
        newComment[Constant.updateTime] = Math.floor(Date.now() / 1000);
    } else {
        params[Constant.commentId] = params[Constant.commentId] || params[Constant.createTime];
        params[Constant.canEdit] = true;   // 刚发的评论：本人、窗口内、无回复
        params[Constant.member] = {
            [Constant.userId]: userStore.id,
            [Constant.avatarUrl]: userStore.avatarUrl,
            [Constant.username]: userStore.username
        };
        childCommentList.value.push(params);
        childTotal.value++;
    }
};
// 编辑时间比发布时间晚 60s 以上，视为「编辑过」
const isEdited = (comment) =>
    Number(comment?.[Constant.updateTime]) - Number(comment?.[Constant.createTime]) > 60;

onMounted(() => {
    // 前 2 条子回复由父级 getComments 内联返回，不在挂载时再发请求；更多走「展开」
    childTotal.value = props.comment[Constant.commentList]?.total || 0;
    childCommentList.value = [...(props.comment[Constant.commentList]?.list || [])];
});
</script>

<style scoped lang="scss">
.comment-container {
    position: relative;
    padding: 24px 0 0 80px;
    .avatar {
        position: absolute;
        left: 0;
        width: 48px;
        height: 48px;
        margin: 0 16px;
        border-radius: 50%;
    }
    .content-container {
        border-bottom: $--border;
        padding-bottom: 4px;
    }
    .username {
        margin-bottom: 4px;
        line-height: 30px;
        font-size: $--font-size-base;
    }
    .info {
        padding-top: 2px;
        span {
            font-size: $--font-size-small;
            color: $--text-color-light;
            &.clickable:hover {
                color: $--color-primary;
            }
            &.edited-mark {
                margin-left: -12px;
                color: $--text-color-secondary;
            }
        }
    }
}

.comment-container.child-comment {
    position: relative;
    padding: 8px 0 8px 42px;
    .avatar {
        position: absolute;
        left: 0;
        width: 30px;
        height: 30px;
        margin: 0 6px;
        border-radius: 50%;
    }
}

.root .more {
    position: absolute;
    right: 0;
    top: 0;
}

.expand-replies {
    margin: 4px 0 4px 42px;
}

:deep(.vuepress-markdown-body:not(.custom)) {
    padding: 0;
}
</style>
