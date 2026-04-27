const moduleName = 'article';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};
const join = (...parts: (string | number)[]) => makeUrl(parts.join('/'));

const urlMap = {
    articles: 'articles',
    search: 'search',
    tags: 'tags',
    images: 'images'
};

const constant = {
    url: {
        getArticleList: join(urlMap.articles, urlMap.search),
        getArticleInfo: (articleId: string | number) => join(urlMap.articles, String(articleId)),
        add: join(urlMap.articles),
        edit: (articleId: string | number) => join(urlMap.articles, String(articleId)),
        delete: (articleId: string | number) => join(urlMap.articles, String(articleId)),
        uploadImage: join(urlMap.articles, urlMap.images),
        getTags: join(urlMap.tags)
    },
    userId: 'id',
    username: 'username',
    avatarAssetId: 'avatarAssetId',
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
