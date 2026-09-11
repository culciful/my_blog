<template>
<div class="main-container a-p-lg">
    <div class="user a-pb-lg a-bb-base">
        <div class="user-avatar a-mr-md">
            <img :src="handleAvatar(originUser.avatarUrl)" alt="">
            <div class="avatar-hover" @click="showUploadAvatar=true">
                <svg-icon name="camera" :size="32" color="#fff"></svg-icon>
            </div>
        </div>
        <div class="info">
            <p class="username a-mb-xs">{{originUser.username}}</p>
            <p>{{$t('label.registerAt')+transferTimestamp(originUser.createTime, 'yyyy-MM-dd')}}</p>
        </div>
        <div class="show a-m-h-xs a-w-200">
            <router-link to="/user/following" class="a-ta-c">
                <div class="a-font-title-1">{{state[Constant.followingCount]}}</div>
                <div class="a-font-body-3">{{$t('label.follow')}}</div>
            </router-link>
            <router-link to="/user/followers" class="a-ta-c">
                <div class="a-font-title-1">{{state[Constant.followerCount]}}</div>
                <div class="a-font-body-3">{{$t('label.followers')}}</div>
            </router-link>
            <router-link to="/user/manageContent" class="a-ta-c">
                <div class="a-font-title-1">{{state[Constant.articleCount]}}</div>
                <div class="a-font-body-3">{{$t('label.blog')}}</div>
            </router-link>
        </div>
    </div>
    <div class="a-m-v-lg">
        <p class="a-font-title-1 a-mb-xxl">
            {{$t('label.userinfo')}}
        </p>
        <el-form
            :model="userForm"
            :rules="rules"
            label-width="96px"
            ref="userFormRef">
            <template v-if="editContent==='username'">
                <el-form-item prop="username" :label="$t('label.username')">
                    <el-input
                        :disabled="isQuerying"
                        v-model="userForm.username"
                        maxlength="20" />
                </el-form-item>
            </template>
            <el-form-item v-else prop="username" :label="$t('label.username')">
                <span class="span-label">
                    {{originUser.username}}
                    <svg-icon @click="editContent='username'" class="a-c-h-primary a-ml-xs" name="edit-pen"></svg-icon>
                </span>
            </el-form-item>
            <template v-if="editContent==='email'">
                <el-form-item prop="email" :label="$t('label.email')">
                    <el-input type="email" v-model="userForm.email" maxlength="64" :disabled="isQuerying"/>
                </el-form-item>
                <el-form-item prop="verificationCode">
                    <el-input v-model="userForm.verificationCode" maxlength="6" :disabled="!isCheckedPwd||isQuerying" :placeholder="$t('label.verificationCode')" />
                    <el-button v-if="!hasSendCode" @click="sendVeriCode" class="a-ml-xxs" link type="primary">{{$t('label.getVerificationCode')}}</el-button>
                    <el-button v-else disabled class="a-ml-xxs" link type="primary">{{timeToGet}}s</el-button>
                </el-form-item>
            </template>
            <el-form-item v-else prop="email" :label="$t('label.email')">
                <span class="span-label">
                    {{originUser.email}}
                    <svg-icon @click="editContent='email'" class="a-c-h-primary a-ml-xs" name="edit-pen"></svg-icon>
                </span>
            </el-form-item>
            <template v-if="editContent==='password'">
                <el-form-item prop="password" :label="$t('label.oldPassword')">
                    <el-input
                        type="password"
                        show-password
                        maxlength="64"
                        minlength="8"
                        v-model="userForm.password"
                        :disabled="isQuerying"
                        :placeholder="$t('inputMessage.inputPassword')" />
                    <el-link class="a-ml-xs" type="primary" :underline="false" href="/forgetPassword">{{$t('label.forgetPwd')}}</el-link>
                </el-form-item>
                <el-form-item prop="newPassword" :label="$t('label.newPassword')">
                    <el-input
                        type="password"
                        show-password
                        minlength="8"
                        maxlength="64"
                        v-model="userForm.newPassword"
                        :disabled="isQuerying"
                        :placeholder="$t('inputMessage.inputNewPassword')" />
                </el-form-item>
                <el-form-item prop="confirmNewPwd">
                    <el-input
                        type="password"
                        show-password
                        minlength="8"
                        maxlength="64"
                        v-model="userForm.confirmNewPwd"
                        :disabled="isQuerying"
                        :placeholder="$t('inputMessage.confirmNewPassword')" />
                </el-form-item>
            </template>
            <el-form-item v-else prop="password" :label="$t('label.password')">
                <span class="span-label">
                    ******
                    <svg-icon @click="editContent='password'" class="a-c-h-primary a-ml-xs" name="edit-pen"></svg-icon>
                </span>
            </el-form-item>
            <el-form-item v-show="editContent.length>0">
                <el-button
                    :disabled="isQuerying"
                    @click="editContent=''">
                    {{$t('label.cancel')}}
                </el-button>
                <el-button
                    :loading="isQuerying"
                    type="primary"
                    @click="onSubmit(userFormRef)">
                    {{$t('label.confirm')}}
                </el-button>
            </el-form-item>
        </el-form>
    </div>
    <div class="a-p-v-lg a-bt-base">
        <p class="a-font-title-1 a-mb-xxl">{{$t('label.dangerZone')}}</p>
        <div class="flex-center" >
            <el-button type="danger" plain @click="onDeleteAccount">
                {{$t('label.deleteAccount')}}
            </el-button>
        </div>
    </div>
</div>
<check-pwd-dialog v-model="showCheckPwdDialog" @success="send"></check-pwd-dialog>
<upload-avatar v-model="showUploadAvatar" @success="loadMyProfile"></upload-avatar>
<delete-account-dialog v-model="showDeleteAccountDialog" @success="onAccountDeleted"></delete-account-dialog>
</template>

<script lang="ts" setup name="UserCenter">
import CheckPwdDialog from './components/checkPwdDialog.vue';
import UploadAvatar from './components/uploadAvatar.vue';
import DeleteAccountDialog from './components/deleteAccountDialog.vue';
import {getCurrentInstance, ref, reactive, onMounted} from 'vue';
import {useUserStore} from '@/stores/user';
import {ElMessage, ElMessageBox, FormInstance, FormRules} from 'element-plus';
import i18n from '@/language/i18n';
import {setReactiveData, transferTimestamp} from '@/utils/utils';
import {globalRules} from '@/utils/validate';
import Constant, {handleAvatar} from '@/model/user/constant';

const {proxy} = getCurrentInstance();
const { t } = i18n.global as any;
const userStore = useUserStore();
interface OriginUser {
    email: string;
    username: string;
    avatarUrl: string;
    createTime: number;
}
const originUser = ref<OriginUser>({
    email: '',
    username: '',
    avatarUrl: '',
    createTime: 0
});
const editContent = ref('');
const showUploadAvatar = ref(false);

interface UserForm {
    email: string,
    username: string,
    password: string,
    newPassword: string,
    confirmNewPwd: string,
    verificationCode: string
}
const userFormRef = ref<FormInstance>();
const userForm = reactive<UserForm>({
    email: '',
    verificationCode: '',
    username: '',
    password: '',
    newPassword: '',
    confirmNewPwd: ''
});
let isQuerying = ref(false);

const validateUsername = (rule: any, value: any, callback: any) => {
    if (value === originUser.value.username) {
        callback(new Error(t('inputMessage.usernameSame')));
    } else callback();
};
const validateEmail = (rule: any, value: any, callback: any) => {
    if (value === originUser.value.email) {
        callback(new Error(t('inputMessage.emailSame')));
    } else callback();
};
const validateNewPass = (rule: any, value: any, callback: any) => {
    if (value && value === userForm.password) {
        callback(new Error(t('inputMessage.pwdSame')));
    } else callback();
};
const validatePass2 = (rule: any, value: any, callback: any) => {
    if (value !== userForm.newPassword) {
        callback(new Error(t('inputMessage.inputNotMatch')));
    } else callback();
};
const rules = reactive<FormRules<UserForm>>({
    username: [...globalRules.username, { validator: validateUsername, trigger: 'blur' }],
    password: globalRules.currentPassword,
    newPassword: [...globalRules.password, { validator: validateNewPass, trigger: 'blur' }],
    confirmNewPwd: [{ validator: validatePass2, trigger: 'blur' }],
    email: [...globalRules.email, { validator: validateEmail, trigger: 'blur' }],
    verificationCode: globalRules.required
});

const hasSendCode = ref(false);
const timeToGet = ref(60);
const showCheckPwdDialog = ref(false);
let interval = null;
let isCheckedPwd = ref(false);
const sendVeriCode = () => {
    userFormRef.value.validateField('email', async (valid) => {
        if (valid) {
            if(!isCheckedPwd.value) {
                showCheckPwdDialog.value = true;
            }
            else send();
        }
    });
};
const send = () => {
    isCheckedPwd.value = true;
    hasSendCode.value = true;
    setCountdownTimer();
    proxy.$request.post(Constant.url.sendEmailCode, {
        [Constant.email]: userForm.email,
        [Constant.scene]: 'update_email'
    }).catch(err => {
        hasSendCode.value = false;
        if(interval) clearInterval(interval);
    });
};
const setCountdownTimer = () => {
    timeToGet.value = 60;
    interval = setInterval(() => {
        timeToGet.value = timeToGet.value - 1;
        if(timeToGet.value === 0) {
            hasSendCode.value = false;
            clearInterval(interval);
        }
    }, 1000);
};

const onSubmit = async (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    if(editContent.value === 'username') {
        await formEl.validateField('username', (valid) => {
            if(valid) {
                isQuerying.value = true;
                proxy.$request.post(Constant.url.updateUserInfo, {
                    [Constant.username]: userForm.username
                }).then(res => {
                    isQuerying.value = false;
                    loadMyProfile();
                }).catch(err => {
                    isQuerying.value = false;
                });
            }
        });
    }
    else if(editContent.value === 'email') {
        await formEl.validateField(['email', 'verificationCode'], (valid) => {
            if(valid) {
                isQuerying.value = true;
                proxy.$request.post(Constant.url.updateUserInfo, {
                    [Constant.email]: userForm.email,
                    [Constant.verificationCode]: userForm.verificationCode
                }).then(res => {
                    isQuerying.value = false;
                    reLogin();
                }).catch(err => {
                    isQuerying.value = false;
                });
            }
        });
    }
    else if(editContent.value === 'password') {
        await formEl.validateField(['password', 'newPassword', 'confirmNewPwd'], (valid) => {
            if(valid) {
                isQuerying.value = true;
                proxy.$request.post(Constant.url.updatePassword, {
                    [Constant.currentPassword]: userForm.password,
                    [Constant.newPassword]: userForm.newPassword
                }).then(() => {
                    isQuerying.value = false;
                    reLogin();
                }).catch(err => {
                    isQuerying.value = false;
                });
            }
        });
    }
};

const reLogin = () => {
    userStore.clear();
    ElMessageBox.alert(t('infoMessage.reLogin'), t('label.tip'), {
        confirmButtonText: t('label.confirm'),
        showClose: false,
        callback: () => {
            location.href = '/login';
        }
    });
};

const showDeleteAccountDialog = ref(false);

// 警告 + 输密码两步都挪进 DeleteAccountDialog 组件自己的 template 了（不再借 ElMessageBox），
// 这里只管开弹窗
const onDeleteAccount = () => {
    showDeleteAccountDialog.value = true;
};

// 密码校验 + 调 deleteAccount 都在 DeleteAccountDialog 组件里，这里只管「确认后要做什么」
const onAccountDeleted = () => {
    userStore.clear();
    ElMessage.success(t('infoMessage.accountDeleted'));
    location.href = '/';
};

const state = reactive({
    [Constant.followingCount]: 0,
    [Constant.followerCount]: 0,
    [Constant.articleCount]: 0
});
const getStat = () => {
    proxy.$request.get(Constant.url.stat).then(({result}) => {
        setReactiveData(state, result);
    });
};
const loadMyProfile = () => {
    proxy.$request.get(Constant.url.getMyProfile).then(({result}) => {
        originUser.value = result;
        userStore.setUserData(result);
        userForm.username = result[Constant.username];
        userForm.email = result[Constant.email];
    });
};

onMounted(() => {
    loadMyProfile();
    getStat();
});
</script>

<style>
body {
    min-width: 600px;
}
</style>
<style scoped lang="scss">
.main-container {
    max-width: 900px;
    margin: 24px auto;
    background: $--bg-color;
}

.user {
    display: flex;
    align-items: center;
    .user-avatar {
        flex-shrink: 0;
        position: relative;
        width: 92px;
        height: 92px;
        border-radius: 50%;
        border: 2px solid $--color-white;
        cursor: pointer;
        img {
            width: 100%;
            height: 100%;
            border-radius: 50%;
        }
        &:hover {
            .avatar-hover {
                display: flex;
            }
        }
    }
    .avatar-hover {
        display: none;
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,.5);
        justify-content: center;
        align-items: center;
        border-radius: 50%;
    }
    .username {
        font-size: $--font-size-extra-large;
        font-weight: 600;
        line-height: 24px;
        color: $--text-color;
    }
    .info {
        flex-grow: 1;
    }
    .show {
        display: inline-flex;
        align-items: center;
        justify-content: space-between;
    }
}
.el-input {
    width: 200px;
}

</style>
