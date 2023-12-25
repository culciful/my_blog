// import { useI18n } from 'vue-i18n';
// const { t } = useI18n();
//  t 只能在setup中使用

export const regExps = {
    // 以字母或下划线开头，包含字母、数字、以及下划线 4-12位
    username1: /^[a-zA-Z_]([a-zA-Z0-9_]+){3,12}$/,
    // 密码至少1个字母，1个数字和1个特殊字符
    password1: /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[~!@#$%^&*()_ +^\-={}:";'<>?,./])[A-Za-z\d~!@#$%^&*()_ +^\-={}:";'<>?,./]{6,32}$/,
    enabledChar: /^[a-zA-Z\d~!@#$%^&*()_ +^\-={}:";'<>?,./]*$/
};

/*
const validatePass = (rule: any, value: any, callback: any) => {
    if (!regExps.enabledChar.test(value)) {
        callback(new Error(t('inputMessage.passwordFormat')));
    }
    callback();
};

export const validators = {
    username: [
        { required: true, message: t("inputMessage.inputUsername"), trigger: 'blur' },
        { min: 4, max: 16, message: t("inputMessage.usernameFormat"), trigger: 'blur' },
    ],
    password: [
        {required: true, message: t("inputMessage.inputPassword"), trigger: 'change',},
        { min: 6, max: 32, message: t("inputMessage.passwordFormat"), trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
    ]
};*/
