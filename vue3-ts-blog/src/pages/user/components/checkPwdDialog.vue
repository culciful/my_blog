<template>
<el-dialog
    class="check-dialog"
    v-model="show"
    top="30vh"
    :title="$t('label.tip')"
    :before-close="handleClose">
    <label for="check">{{$t('inputMessage.verifyPassword')}}</label>
    <el-form ref="checkFormRef" :rules="rules" :model="checkForm" class="a-mt-sm">
        <el-form-item prop="password">
            <el-input
                id="check"
                type="password"
                show-password
                maxlength="64"
                minlength="6"
                v-model="checkForm.password" />
        </el-form-item>
    </el-form>
    <template #footer>
        <el-button @click="close" :disabled="isQuerying">{{$t('label.cancel')}}</el-button>
        <el-button type="primary" @click="confirm" :loading="isQuerying">
          {{$t('label.confirm')}}
        </el-button>
    </template>
</el-dialog>
</template>

<script lang="ts" setup name="CheckPwdDialog">
import {getCurrentInstance, ref, reactive, watch} from 'vue';
import {useUserStore} from '@/stores/user';
import {FormInstance, FormRules} from 'element-plus';
import {globalRules} from '@/utils/validate';
import Constant from '@/model/user/constant';

const {proxy} = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
    modelValue: Boolean
});
const emit = defineEmits(['update:modelValue', 'success']);

const show = ref(false);

watch(() => props.modelValue, val => {
    show.value = val;
});

interface CheckPwdForm {
    password: string,
}
const checkFormRef = ref<FormInstance>();
const checkForm = reactive<CheckPwdForm>({
    password: '',
});
const rules = reactive<FormRules<CheckPwdForm>>({
    password: globalRules.password,
});
let isQuerying = ref(false);

const confirm = () => {
    checkFormRef.value.validate(valid => {
        if(valid) {
            isQuerying.value = true;
            proxy.$request.post(Constant.url.checkPassword, {
                [Constant.password]: checkForm.password,
                [Constant.userId]: userStore.id
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
    emit('update:modelValue', false);
};

const handleClose = (done: () => void) => {
    if(isQuerying.value) return;
    close();
    done();
};
</script>

<style lang="scss">
.check-dialog {
    max-width: 418px;
    min-width: 320px;
    .el-dialog__body {
        padding: 10px 15px;
    }
}
</style>
