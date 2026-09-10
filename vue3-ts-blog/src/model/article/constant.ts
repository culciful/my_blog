const moduleName = 'article';

// 「草稿箱」在合集列表里的哨兵 pid：字符串，不与数字分组 id / 0(全部) 冲突。
// 选中它时 articleList 改打 getDraftList 接口。
export const DRAFT_PID = 'draft';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getArticleList: 'getArticleList',
    getArticleInfo: 'getArticleInfo',
    addArticle: 'addArticle',
    editArticle: 'editArticle',
    deleteArticle: 'deleteArticle',
    saveDraft: 'saveDraft',
    getDraftList: 'getDraftList',
    getDraft: 'getDraft',
    uploadImage: 'uploadImage',
    getTags: 'getTags'
};

const constant = {
    url: {
        getArticleList: makeUrl(urlMap.getArticleList),
        getArticleInfo: makeUrl(urlMap.getArticleInfo),
        add: makeUrl(urlMap.addArticle),
        edit: makeUrl(urlMap.editArticle),
        delete: makeUrl(urlMap.deleteArticle),
        saveDraft: makeUrl(urlMap.saveDraft),
        getDraftList: makeUrl(urlMap.getDraftList),
        getDraft: makeUrl(urlMap.getDraft),
        uploadImage: makeUrl(urlMap.uploadImage),
        getTags: makeUrl(urlMap.getTags)
    },
    userId: 'id',
    username: 'username',
    avatarAssetId: 'avatarAssetId',
    avatarUrl: 'avatarUrl',
    articleId: 'aid',
    title: 'title',
    createTime: 'createTime',
    updateTime: 'updateTime',
    viewCount: 'viewCount',
    commentCount: 'commentCount',
    abstract: 'abstract',
    isCustomAbstract: 'isCustomAbstract',
    content: 'content',
    package: 'package',
    tags: 'tags',
    tag: 'tag',
    commentList: 'comments',
    commentId: 'cid',
    author: 'member',
    member: 'member',
    msg: 'msg',
    root: 'root',
    packageId: 'pid',
    packageName: 'pname',
    keyword: 'keyword',
    imgUrl: 'imgUrl'
};

export default constant;
