import i18n from '@/language/i18n';

const { t } = i18n.global as any;

export const patterns = {
    enabledStr: /^[a-zA-Z0-9\u4E00-\u9FA5~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-][a-zA-Z0-9\u4E00-\u9FA5 ~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-]*$/,
    // 1-20位 汉字/数字/字母/特殊字符；禁止 @（避免用户名与他人邮箱字面相同造成登录歧义）
    username: /^[a-zA-Z0-9\u4E00-\u9FA5~!#$%^&*()_+|}{[\]\\/?><:"`;.,'-][a-zA-Z0-9\u4E00-\u9FA5 ~!#$%^&*()_+|}{[\]\\/?><:"`;.,'-]{0,20}$/,
    // 8-64位，至少含一个字母
    password: /^(?=.*[a-zA-Z])[A-Za-z\d~!@#$%^&*()_+|}{[\]\\/?><:"`;.,'-]{8,64}$/,
    email: /[\w!#$%&'*+/=?^_`{|}~-]+(?:\.[\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\w](?:[\w-]*[\w])?\.)+[\w](?:[\w-]*[\w])?/
};

export const globalRules = {
    username: [
        { required: true, message: t('inputMessage.usernameFormat'), trigger: 'blur' },
        // { min: 1, max: 20, message: t('inputMessage.usernameFormat'), trigger: 'blur' },
        { pattern: patterns.username, message: t('inputMessage.usernameFormat'), trigger: 'blur' }
    ],
    // 登录账号（用户名或邮箱）：1–64 位，不校验格式；el-input maxlength="64"
    account: [
        { required: true, message: t('inputMessage.usernameFormat'), trigger: 'blur' }
        // { min: 1, max: 64, message: t('inputMessage.usernameFormat'), trigger: 'blur' }
    ],
    // 设置新密码：8–64 位、至少含一个字母；el-input maxlength="64"（min 8 短值能命中，保留）
    password: [
        {required: true, message: t('inputMessage.passwordFormat'), trigger: 'blur'},
        // { min: 8, max: 64, message: t('inputMessage.passwordFormat'), trigger: 'blur' },
        { pattern: patterns.password, message: t('inputMessage.passwordFormat'), trigger: 'blur' }
    ],
    // 登录 / 校验当前密码：≤64 位、只要求非空（不按新策略卡旧密码）；el-input maxlength="64"
    currentPassword: [
        { required: true, message: t('inputMessage.passwordFormat'), trigger: 'blur' }
        // { max: 64, message: t('inputMessage.passwordFormat'), trigger: 'blur' }
    ],
    email: [
        { required: true, message: t('inputMessage.inputEmail'), trigger: 'blur' },
        {pattern: patterns.email, message: t('inputMessage.emailFormat'), trigger: 'blur'}
    ],
    required: [
        { required: true, message: t('inputMessage.invalidInput'), trigger: 'blur'}
    ]
};