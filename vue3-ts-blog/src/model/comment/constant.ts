const moduleName = 'comment';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};
const join = (...parts: (string | number)[]) => makeUrl(parts.join('/'));

const urlMap = {
    articles: 'articles',
    comments: 'comments',
    search: 'search',
    inbox: 'inbox'
};

const constant = {
    url: {
        commentsInboxSearch: join(urlMap.comments, urlMap.inbox, urlMap.search),
        commentsByArticleSearch: (articleId: string | number) =>
            join(urlMap.articles, String(articleId), urlMap.comments, urlMap.search),
        articleComments: (articleId: string | number) =>
            join(urlMap.articles, String(articleId), urlMap.comments),
        articleComment: (articleId: string | number, commentId: string | number) =>
            join(urlMap.articles, String(articleId), urlMap.comments, String(commentId))
    },
    userId: 'id',
    authorId: 'authorId',
    title: 'title',
    username: 'username',
    avatarAssetId: 'avatarAssetId',
    avatarUrl: 'avatarUrl',
    articleId: 'aid',
    createTime: 'createTime',
    commentCount: 'commentCount',
    content: 'content',
    commentList: 'comments',
    commentId: 'cid',
    member: 'member',
    msg: 'msg',
    root: 'root',
    useMD: 'useMD',
    parent: 'parent',
    parentContent: 'parentContent',
    total: 'total'
};

export default constant;
