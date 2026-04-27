import i18n from '@/language/i18n';

const { t } = i18n.global as any;
const moduleName = 'user';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};
const join = (...parts: (string | number)[]) => makeUrl(parts.join('/'));

const urlMap = {
    users: 'users',
    me: 'me',
    passwordCheck: 'password-check',
    stats: 'stats',
    packages: 'packages',
    followState: 'follow-state',
    followings: 'followings',
    followers: 'followers',
    search: 'search',
    emailExistence: 'email-existence',
    verificationCodes: 'verification-codes',
    verificationCheck: 'verification-check',
    avatar: 'avatar'
};

const constant = {
    url: {
        register: join(urlMap.users),
        getMyProfile: join(urlMap.users, urlMap.me),
        updateUserInfo: join(urlMap.users, urlMap.me),
        checkPassword: join(urlMap.users, urlMap.me, urlMap.passwordCheck),
        getUserInfo: (userId: string | number) => join(urlMap.users, userId),
        stat: join(urlMap.users, urlMap.me, urlMap.stats),
        followings: join(urlMap.users, urlMap.me, urlMap.followings, urlMap.search),
        followers: join(urlMap.users, urlMap.me, urlMap.followers, urlMap.search),
        sendEmailCode: join(urlMap.users, urlMap.verificationCodes),
        getPackages: (userId: string | number) => join(urlMap.users, userId, urlMap.packages),
        addPackage: (userId: string | number) => join(urlMap.users, userId, urlMap.packages),
        deletePackage: (userId: string | number, packageId: string | number) =>
            join(urlMap.users, userId, urlMap.packages, packageId),
        editPackage: (userId: string | number, packageId: string | number) =>
            join(urlMap.users, userId, urlMap.packages, packageId),
        checkEmailCode: join(urlMap.users, urlMap.me, urlMap.verificationCheck),
        checkEmailExist: join(urlMap.users, urlMap.emailExistence),
        uploadAvatar: join(urlMap.users, urlMap.me, urlMap.avatar),
        checkHasFollow: (targetUserId: string | number) =>
            join(urlMap.users, targetUserId, urlMap.followState),
        switchFollow: (targetUserId: string | number) =>
            join(urlMap.users, targetUserId, urlMap.followState)
    },
    userId: 'id',
    email: 'email',
    username: 'username',
    avatarAssetId: 'avatarAssetId',
    avatarUrl: 'avatarUrl',
    password: 'password',
    packageId: 'pid',
    packageName: 'pname',
    packages: 'packages',
    articles: 'articles',
    verificationCode: 'verificationCode',
    checkExist: 'checkExist',
    value: 'value',
    articleCount: 'articleCount',
    following: 'following',
    follower: 'follower',
    mutual: 'mutual',
    isExisted: 'isExisted'
};

export const defaultPackage = {
    [constant.packageId]: 0,
    [constant.packageName]: t('label.all')
};

export const viewUser = (router: any, userinfo: Record<string, any>) => {
    const id = userinfo[constant.userId];
    const pid = userinfo[constant.packageId];
    const hasPackage =
        pid != null && pid !== '' && !Number.isNaN(Number(pid)) && Number(pid) !== 0;
    const query: Record<string, string> = {
        [constant.userId]: String(id)
    };
    if (hasPackage) {
        query[constant.packageId] = String(pid);
    }
    router.push({
        name: 'viewUser',
        query
    });
};

export const handleAvatar = (avatarUrl?: string): string => {
    if (!avatarUrl) return '/static/img/user-filling.svg';
    return avatarUrl;
};

export default constant;
