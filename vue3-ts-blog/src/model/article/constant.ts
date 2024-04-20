const moduleName = 'article';
const makeUrl = (str) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getArticleList: 'getArticleList',
    getArticleInfo: 'getArticleInfo',
    add: 'addArticle',
    edit: 'editArticle',
    delete: 'deleteArticle',
    uploadImage: 'uploadImage',
    getTags: 'getTags'
};

const constant =  {
    url: {
        getArticleList: makeUrl(urlMap.getArticleList),
        getArticleInfo: makeUrl(urlMap.getArticleInfo),
        add: makeUrl(urlMap.add),
        edit: makeUrl(urlMap.edit),
        delete: makeUrl(urlMap.delete),
        uploadImage: makeUrl(urlMap.uploadImage),
        getTags: makeUrl(urlMap.getTags)
    },
    userId: 'id',
    username: 'username',
    avatarUrl: 'avatarUrl',
    articleId: 'aid',
    title: 'title',
    createTime: 'createTime',
    viewCount: 'viewCount',
    commentCount: 'commentCount',
    abstract: 'abstract',
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