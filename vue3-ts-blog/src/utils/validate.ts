import i18n from '@/language/i18n';

const { t } = i18n.global as any;

export const patterns = {
    enabledStr: /^[a-zA-Z0-9\u4E00-\u9FA5~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-][a-zA-Z0-9\u4E00-\u9FA5 ~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-]*$/,
    // 1-64位 包含汉字数字字母特殊字符
    username: /^[a-zA-Z0-9\u4E00-\u9FA5~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-][a-zA-Z0-9\u4E00-\u9FA5 ~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-]{0,63}$/,
    // Minimum 6 and maximum 64 characters, at least one letter,
    password: /^(?=.*[a-zA-Z])[A-Za-z\d~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-]{6,64}$/,
    email: /[\w!#$%&'*+/=?^_`{|}~-]+(?:\.[\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\w](?:[\w-]*[\w])?\.)+[\w](?:[\w-]*[\w])?/
};

export const globalRules = {
    username: [
        { required: true, message: t('inputMessage.usernameFormat'), trigger: 'change' },
        { min: 1, max: 16, message: t('inputMessage.usernameFormat'), trigger: 'blur' },
        { pattern: patterns.username, message: t('inputMessage.usernameFormat'), trigger: 'blur' }
    ],
    password: [
        {required: true, message: t('inputMessage.passwordFormat'), trigger: 'change'},
        { min: 6, max: 32, message: t('inputMessage.passwordFormat'), trigger: 'blur' },
        { pattern: patterns.password, message: t('inputMessage.passwordFormat'), trigger: 'blur' }
    ],
    email: [
        { required: true, message: t('inputMessage.inputEmail'), trigger: 'change' },
        {pattern: patterns.email, message: t('inputMessage.emailFormat'), trigger: 'blur'}
    ],
    required: [
        { required: true, message: t('inputMessage.invalidInput'), trigger: 'change'}
    ]
};