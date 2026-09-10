<template>
<div class="add-comment">
    <img class="avatar" :src="handleAvatar(userStore.avatarUrl)" alt="">
    <el-input
        v-show="!isMarkdown"
        v-model="newComment"
        :rows="3"
        type="textarea"
        maxlength="10000"
        show-word-limit
        :placeholder="placeholder"
    />
    <v-md-editor v-show="isMarkdown"
                 height="400px"
                 left-toolbar="undo redo clear | bold italic strikethrough | quote code | ul ol table | link image"
                 :disabled-menus="[]"
                 @upload-image="handleUploadImage"
                 v-model="newComment">
    </v-md-editor>
    <div class="a-mt-xs operation-panel">
        <div>
            <span class="a-font-body-1 a-mr-xxs">{{$t('label.openMD')}}</span>
            <el-switch v-model="isMarkdown" />
        </div>
        <el-button type="primary" @click="publish" :disabled="newComment.length===0">{{$t('label.publish')}}</el-button>
    </div>
</div>
</template>

<script lang="ts" setup name="AddComment">
import {getCurrentInstance, ref, watch} from 'vue';
import {useUserStore} from '@/stores/user';
import Constant from '@/model/comment/constant';
import i18n from '@/language/i18n';
import {ElMessage} from 'element-plus';
import {deepCopy} from '@/utils/utils';
import {handleAvatar} from '@/model/user/constant';

const { t } = i18n.global as any;
const props = defineProps({
    show: Boolean,
    mode: {
        type: String,
        default: 'add',
        validator(value: string): boolean {
            return ['add', 'edit', ''].includes(value);
        }
    },
    editComment: Object,
    parentComment: Object,
    articleId: {
        type: [Number, String],
        required: true
    },
    authorId: {
        type: [Number, String],
        required: true
    }
});
const emit = defineEmits(['finish']);
const { proxy }: any = getCurrentInstance();
const userStore = useUserStore();
const newComment = ref('');
const isMarkdown = ref(false);
const placeholder = ref('');

watch(() => props.show, (val) => {
    if(val) {
        if(props.mode === 'add') {
            if(props.parentComment) {
                placeholder.value = t('label.replyTo') + ' @' + props.parentComment[Constant.member][Constant.username] + '：';
            } else placeholder.value = t('inputMessage.inputComment');
        } else if(props.mode === 'edit'){
            newComment.value = props.editComment?.[Constant.content][Constant.msg];
            isMarkdown.value = !!props.editComment?.[Constant.isMarkdown];
            placeholder.value = props.editComment?.[Constant.content][Constant.msg];
        }
    } else {
        newComment.value = '';
        isMarkdown.value = false;
    }
});

function handleUploadImage(event, insertImage, files) {
    if (!files || !files[0]) return;
    const formData = new FormData();
    formData.append('file', files[0]);
    proxy.$request.post(Constant.url.uploadImage, formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    }).then(res => {
        insertImage({ url: res.result.url, desc: '' });
    });
}

const publish = () => {
    newComment.value = newComment.value.trim();
    if(newComment.value.length === 0) {
        ElMessage.error(t('inputMessage.invalidInput'));
        return;
    }
    // markdown 模式用 v-md-editor，没有 maxlength，这里兜一下（纯文本模式已被 maxlength 挡住）
    if(newComment.value.length > 10000) {
        ElMessage.error(t('inputMessage.commentTooLong'));
        return;
    }
    let params = {};
    if(props.mode === 'add') {
        params = {
            [Constant.userId]: userStore.id,
            [Constant.articleId]: props.articleId,
            [Constant.authorId]: props.authorId,
            [Constant.createTime]: (new Date().getTime() / 1000).toFixed(),
            [Constant.isMarkdown]: isMarkdown.value,
            [Constant.content]: {
                [Constant.msg]: newComment.value,
                [Constant.member]: {}
            }
        };
        if(props.parentComment) {
            params[Constant.parent] = props.parentComment[Constant.commentId];
            params[Constant.root] = props.parentComment[Constant.root] || props.parentComment[Constant.commentId];
            if(params[Constant.parent] !== params[Constant.root]) {
                params[Constant.content][Constant.member] = props.parentComment[Constant.member];
            }
        }
        proxy.$request.post(Constant.url.addComment, params).then((res) => {
            // 用后端返回的真实 cid，避免用本地时间戳当 id 导致后续编辑/删除报「资源不存在」
            params[Constant.commentId] = res?.result?.[Constant.commentId] ?? params[Constant.createTime];
            ElMessage.success(t('infoMessage.publishSuccess'));
            emit('finish', params);
            // 发布后重置编辑区，并切回非 markdown 模式
            newComment.value = '';
            isMarkdown.value = false;
        });
    } else {
        if (!props.editComment) return;
        params = deepCopy(props.editComment);
        params[Constant.createTime] = (new Date().getTime() / 1000).toFixed();
        params[Constant.isMarkdown] = isMarkdown.value;
        params[Constant.content][Constant.msg] = newComment.value;
        params[Constant.articleId] = props.articleId;
        params[Constant.commentId] = props.editComment[Constant.commentId];
        proxy.$request.post(Constant.url.editComment, params).then(() => {
            ElMessage.success(t('infoMessage.editSuccess'));
            emit('finish', params);
        });
    }

};
</script>

<style scoped lang="scss">
.add-comment {
    position: relative;
    padding: 24px 0 24px 80px;
    .avatar {
        position: absolute;
        left: 0;
        width: 48px;
        height: 48px;
        margin: 0 16px;
        border-radius: 50%;
    }
    .el-textarea {
        font-size: $--font-size-extra-small;
    }
}
.operation-panel {
    display: flex;
    justify-content: space-between;
}
</style>
