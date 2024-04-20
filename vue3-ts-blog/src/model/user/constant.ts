import i18n from '@/language/i18n';

const { t } = i18n.global as any;
const moduleName = 'user';
const makeUrl = (str) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    login: 'login',
    logout: 'logout',
    register: 'register',
    getUserInfo: 'getUserInfo',
    stat: 'stat',
    followings: 'followings',
    followers: 'followers',
    updateUserInfo: 'updateUserInfo',
    sendEmailCode: 'sendEmailCode',
    checkPassword: 'checkPassword',
    checkEmailCode: 'checkEmailCode',
    checkEmailExist: 'checkEmailExist',
    uploadAvatar: 'uploadAvatar',
    checkHasFollow: 'checkHasFollow',
    switchFollow: 'switchFollow',
    getPackages: 'getPackages',
    addPackage: 'addPackage',
    deletePackage: 'deletePackage',
    editPackage: 'editPackage'
};

const constant =  {
    url: {
        login: makeUrl(urlMap.login),
        logout: makeUrl(urlMap.logout),
        register: makeUrl(urlMap.register),
        getUserInfo: makeUrl(urlMap.getUserInfo),
        stat: makeUrl(urlMap.stat),
        followings: makeUrl(urlMap.followings),
        followers: makeUrl(urlMap.followers),
        updateUserInfo: makeUrl(urlMap.updateUserInfo),
        sendEmailCode: makeUrl(urlMap.sendEmailCode),
        getPackages: makeUrl(urlMap.getPackages),
        addPackage: makeUrl(urlMap.addPackage),
        deletePackage: makeUrl(urlMap.deletePackage),
        editPackage: makeUrl(urlMap.editPackage),
        checkPassword: makeUrl(urlMap.checkPassword),
        checkEmailCode: makeUrl(urlMap.checkEmailCode),
        checkEmailExist: makeUrl(urlMap.checkEmailExist),
        uploadAvatar: makeUrl(urlMap.uploadAvatar),
        checkHasFollow: makeUrl(urlMap.checkHasFollow),
        switchFollow: makeUrl(urlMap.switchFollow)
    },
    userId: 'id',
    email: 'email',
    username: 'username',
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

export const viewUser = (router, userinfo) => {
    if(userinfo[constant.avatarUrl]) userinfo[constant.avatarUrl] = encodeURIComponent(userinfo[constant.avatarUrl]);
    router.push({
        name: 'viewUser',
        query: userinfo
    });
};

export const handleAvatar = (avatarUrl :string) : string => {
    if(avatarUrl === '' || !avatarUrl) return '/static/img/user-filling.svg';
    return avatarUrl;
};

export default constant;