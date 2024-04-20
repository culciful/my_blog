const moduleName = 'comment';
const makeUrl = (str) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getComments: 'getComments',
    addComment: 'addComment',
    editComment: 'editComment',
    deleteComment: 'deleteComment'
};

const constant =  {
    url: {
        getComments: makeUrl(urlMap.getComments),
        addComment: makeUrl(urlMap.addComment),
        editComment: makeUrl(urlMap.editComment),
        deleteComment: makeUrl(urlMap.deleteComment)
    },
    userId: 'id',
    authorId: 'authorId',
    title: 'title',
    username: 'username',
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