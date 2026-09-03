import i18n from '@/language/i18n';

const { t } = i18n.global as any;
const moduleName = 'user';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    register: 'register',
    getMyProfile: 'getMyProfile',
    updateUserInfo: 'updateUserInfo',
    checkPassword: 'checkPassword',
    getUserInfo: 'getUserInfo',
    getStat: 'getStat',
    getFollowings: 'getFollowings',
    getFollowers: 'getFollowers',
    sendEmailCode: 'sendEmailCode',
    getPackages: 'getPackages',
    addPackage: 'addPackage',
    editPackage: 'editPackage',
    deletePackage: 'deletePackage',
    checkEmailCode: 'checkEmailCode',
    checkEmailExist: 'checkEmailExist',
    uploadAvatar: 'uploadAvatar',
    checkHasFollow: 'checkHasFollow',
    switchFollow: 'switchFollow'
};

const constant = {
    url: {
        register: makeUrl(urlMap.register),
        getMyProfile: makeUrl(urlMap.getMyProfile),
        updateUserInfo: makeUrl(urlMap.updateUserInfo),
        checkPassword: makeUrl(urlMap.checkPassword),
        getUserInfo: makeUrl(urlMap.getUserInfo),
        stat: makeUrl(urlMap.getStat),
        followings: makeUrl(urlMap.getFollowings),
        followers: makeUrl(urlMap.getFollowers),
        sendEmailCode: makeUrl(urlMap.sendEmailCode),
        getPackages: makeUrl(urlMap.getPackages),
        addPackage: makeUrl(urlMap.addPackage),
        deletePackage: makeUrl(urlMap.deletePackage),
        editPackage: makeUrl(urlMap.editPackage),
        checkEmailCode: makeUrl(urlMap.checkEmailCode),
        checkEmailExist: makeUrl(urlMap.checkEmailExist),
        uploadAvatar: makeUrl(urlMap.uploadAvatar),
        checkHasFollow: makeUrl(urlMap.checkHasFollow),
        switchFollow: makeUrl(urlMap.switchFollow)
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
    scene: 'scene',
    checkExist: 'checkExist',
    value: 'value',
    articleCount: 'articleCount',
    following: 'following',
    follower: 'follower',
    mutual: 'mutual',
    followed: 'followed',
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
