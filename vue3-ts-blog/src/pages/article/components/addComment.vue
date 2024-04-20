<template>
<div class="add-comment">
    <img class="avatar" :src="handleAvatar(userStore.avatarUrl)" alt="">
    <el-input
        v-show="!useMD"
        v-model="newComment"
        :rows="3"
        type="textarea"
        :placeholder="placeholder"
    />
    <v-md-editor v-show="useMD"
                 height="400px"
                 left-toolbar="undo redo clear | h bold italic strikethrough quote | ul ol table hr | link image code"
                 v-model="newComment">
    </v-md-editor>
    <div class="a-mt-xs operation-panel">
        <div>
            <span class="a-font-body-1 a-mr-xxs">{{$t('label.openMD')}}</span>
            <el-switch v-model="useMD" />
        </div>
        <el-button type="primary" @click="publish" :disabled="newComment.length===0">{{$t('label.publish')}}</el-button>
    </div>
</div>
</template>

<script lang="ts" setup name="AddComment">
import {getCurrentInstance, ref, watch} from 'vue';1;
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
const useMD = ref(false);
const placeholder = ref('');

watch(() => props.show, (val) => {
    if(val) {
        if(props.mode === 'add') {
            if(props.parentComment) {
                placeholder.value = t('label.replyTo') + ' @' + props.parentComment[Constant.member][Constant.username] + '：';
            } else placeholder.value = t('inputMessage.inputComment');
        } else if(props.mode === 'edit'){
            newComment.value = props.editComment?.[Constant.content][Constant.msg];
            useMD.value = !!props.editComment?.[Constant.useMD];
            placeholder.value = props.editComment?.[Constant.content][Constant.msg];
        }
    } else {
        newComment.value = '';
        useMD.value = false;
    }
});

const publish = () => {
    newComment.value = newComment.value.trim();
    if(newComment.value.length === 0) {
        ElMessage.error(t('inputMessage.invalidInput'));
        return;
    }
    let url = '';
    let params = {};
    if(props.mode === 'add') {
        url = Constant.url.addComment;
        params = {
            [Constant.userId]: userStore.id,
            [Constant.articleId]: props.articleId,
            [Constant.authorId]: props.authorId,
            [Constant.createTime]: (new Date().getTime() / 1000).toFixed(),
            [Constant.useMD]: useMD.value,
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
        proxy.$request.post(url, params).then(() => {
            ElMessage.success(t('infoMessage.publishSuccess'));
            emit('finish', params);
        });
    } else {
        url = Constant.url.editComment;
        params = deepCopy(props.editComment);
        params[Constant.createTime] = (new Date().getTime() / 1000).toFixed();
        params[Constant.useMD] = useMD.value;
        params[Constant.content][Constant.msg] = newComment.value;
        proxy.$request.post(url, params).then(() => {
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
