const moduleName = 'comment';
const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getCommentInbox: 'getCommentInbox',
    getComments: 'getComments',
    addComment: 'addComment',
    editComment: 'editComment',
    deleteComment: 'deleteComment'
};

const constant = {
    url: {
        commentsInboxSearch: makeUrl(urlMap.getCommentInbox),
        commentsByArticleSearch: makeUrl(urlMap.getComments),
        addComment: makeUrl(urlMap.addComment),
        editComment: makeUrl(urlMap.editComment),
        deleteComment: makeUrl(urlMap.deleteComment)
    },
    userId: 'id',
    authorId: 'authorId',
    title: 'title',
    username: 'username',
    avatarAssetId: 'avatarAssetId',
    avatarUrl: 'avatarUrl',
    articleId: 'aid',
    createTime: 'createTime',
    updateTime: 'updateTime',
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
