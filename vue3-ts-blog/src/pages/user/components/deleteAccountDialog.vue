<template>
<el-dialog
    class="delete-account-dialog"
    v-model="show"
    top="30vh"
    :title="$t('label.tip')"
    :close-on-click-modal="false"
    :before-close="handleClose">
    <template v-if="step==='warning'">
        <p>{{$t('inputMessage.deleteAccountWarning')}}</p>
    </template>
    <template v-else>
        <label for="delete-account-password">{{$t('inputMessage.deleteAccountConfirmPrompt')}}</label>
        <el-form ref="formRef" :rules="rules" :model="form" class="a-mt-sm">
            <el-form-item prop="password">
                <el-input
                    id="delete-account-password"
                    type="password"
                    show-password
                    maxlength="64"
                    v-model="form.password"
                    :placeholder="$t('inputMessage.inputPassword')"
                    @keyup.enter="confirm" />
            </el-form-item>
        </el-form>
    </template>
    <template #footer>
        <el-button @click="close" :disabled="isQuerying">{{$t('label.cancel')}}</el-button>
        <el-button type="danger" :loading="isQuerying" @click="confirm">
            {{$t('label.confirm')}}
        </el-button>
    </template>
</el-dialog>
</template>

<script lang="ts" setup name="DeleteAccountDialog">
import {getCurrentInstance, ref, reactive, watch} from 'vue';
import {FormInstance, FormRules} from 'element-plus';
import {globalRules} from '@/utils/validate';
import Constant from '@/model/user/constant';

const {proxy} = getCurrentInstance();

const props = defineProps({
    modelValue: Boolean
});
const emit = defineEmits(['update:modelValue', 'success']);

const show = ref(false);
// warning：先过一遍「不可恢复」的警告文案；password：真正输密码确认。
// 两步都在这一个弹窗里、走同一对取消/确定按钮 —— 不用 ElMessageBox，
// 按钮就是模板里写死的 <el-button type="danger">，不会有跟 ElMessageBox
// 内部 type="primary" 叠 class 抢 hover 样式那种问题。
const step = ref<'warning' | 'password'>('warning');

watch(() => props.modelValue, val => {
    show.value = val;
    if(val) step.value = 'warning';
});

interface DeleteAccountForm {
    password: string,
}
const formRef = ref<FormInstance>();
const form = reactive<DeleteAccountForm>({
    password: ''
});
const rules = reactive<FormRules<DeleteAccountForm>>({
    password: globalRules.currentPassword
});
let isQuerying = ref(false);

const confirm = () => {
    if(step.value === 'warning') {
        step.value = 'password';
        return;
    }
    formRef.value.validate(valid => {
        if(valid) {
            isQuerying.value = true;
            proxy.$request.post(Constant.url.deleteAccount, {
                [Constant.password]: form.password
            }).then(() => {
                isQuerying.value = false;
                emit('success');
                close();
            }).catch(err => {
                isQuerying.value = false;
            });
        }
    });
};

const close = () => {
    show.value = false;
    isQuerying.value = false;
    step.value = 'warning';
    form.password = '';
    emit('update:modelValue', false);
    // ElDialog 关闭时会把焦点还给触发它的按钮，el-button 的 :focus/:hover 是合并样式，
    // 不主动失焦的话按钮会一直显示「悬停」的样子
    (document.activeElement as HTMLElement | null)?.blur();
};

const handleClose = (done: () => void) => {
    if(isQuerying.value) return;
    close();
    done();
};
</script>

<style lang="scss">
.delete-account-dialog {
    max-width: 418px;
    min-width: 320px;
    .el-dialog__body {
        padding: 10px 15px;
    }
}
</style>
