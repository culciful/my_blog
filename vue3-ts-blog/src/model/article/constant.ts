const moduleName = 'article';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getArticleList: 'getArticleList',
    getArticleInfo: 'getArticleInfo',
    addArticle: 'addArticle',
    editArticle: 'editArticle',
    deleteArticle: 'deleteArticle',
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
