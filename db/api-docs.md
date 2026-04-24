# vue3-ts-blog 接口文档（含传参与返回格式）

## 1. 统一响应约定

项目请求封装位于 `src/utils/request.ts`，前端统一按如下结构处理：

```json
{
  "errorCode": 0,
  "result": {}
}
```

- `errorCode === 0` 视为成功
- 文档中的“返回格式”默认表示 `result` 字段结构
- 标记“未在前端消费”的接口，表示调用后只关心成功/失败

## 2. User 模块

| 接口 | 方法 | 请求参数（Body） | 返回格式（result） | 主要调用位置 |
|---|---|---|---|---|
| `/user/login` | POST | `{ username: string, password: string }` | `{ id: number, username: string, avatarUrl: string, createTime: number }` | `src/pages/login/login.vue` |
| `/user/logout` | POST | 无 | 未在前端消费（成功即可） | `src/components/customHeader/index.vue` |
| `/user/register` | POST | `{ email: string, username: string, password: string }` | 未在前端消费（成功后跳转登录） | `src/pages/login/register.vue` |
| `/user/getUserInfo` | POST | 无 | `{ id: number, username: string, avatarUrl: string, createTime: number, email: string }` | `src/App.vue`, `src/pages/user/userCenter.vue` |
| `/user/stat` | POST | 无 | `{ articleCount: number, following: number, follower: number }` | `src/pages/user/userCenter.vue` |
| `/user/followings` | POST | `{ pageSize: number, currentPage: number, filter: { keyword: string } }` | `{ list: UserFollowItem[], total: number }` | `src/pages/user/follow.vue` |
| `/user/followers` | POST | `{ pageSize: number, currentPage: number, filter: { keyword: string } }` | `{ list: UserFollowItem[], total: number }` | `src/pages/user/follow.vue` |
| `/user/updateUserInfo` | POST | 场景1改名：`{ username: string }`；场景2改邮箱：`{ email: string, verificationCode: string }`；场景3改密码：`{ password: string }` | 未在前端消费（成功后刷新信息/重新登录） | `src/pages/user/userCenter.vue`, `src/pages/login/forgetPassword.vue` |
| `/user/sendEmailCode` | POST | `{ email: string }` | 未在前端消费（成功即可） | `src/pages/login/register.vue`, `src/pages/login/forgetPassword.vue`, `src/pages/user/userCenter.vue` |
| `/user/checkPassword` | POST | 用户中心：`{ password: string }`；校验弹窗：`{ password: string, id: number }` | 未在前端消费（成功表示密码正确） | `src/pages/user/userCenter.vue`, `src/pages/user/components/checkPwdDialog.vue` |
| `/user/checkEmailCode` | POST | `{ email: string, verificationCode: string }` | 未在前端消费（成功即可进入下一步） | `src/pages/login/forgetPassword.vue` |
| `/user/checkEmailExist` | POST | `{ email: string }` | `{ isExisted: boolean }` | `src/pages/login/register.vue`, `src/pages/login/forgetPassword.vue` |
| `/user/uploadAvatar` | POST | `multipart/form-data`：`file` | 未在前端消费（成功后刷新头像） | `src/pages/user/components/uploadAvatar.vue` |
| `/user/checkHasFollow` | POST | `{ id: number }` | `{ data: boolean }` | `src/pages/user/manageContent.vue` |
| `/user/switchFollow` | POST | `{ id: number, value: boolean }` | 未在前端消费（成功后前端本地切换状态） | `src/pages/user/manageContent.vue`, `src/pages/user/follow.vue` |
| `/user/getPackages` | POST | `{ id: number }` | `{ list: { pid: number, pname: string }[] }` | `src/pages/article/addArticle.vue`, `src/pages/user/manageContent.vue` |
| `/user/addPackage` | POST | `{ id: number, pname: string }` | `{ pid: number }` | `src/pages/article/addArticle.vue`, `src/pages/user/manageContent.vue` |
| `/user/deletePackage` | POST | `{ id: number, pid: number }` | 未在前端消费（成功即可） | `src/pages/user/manageContent.vue` |
| `/user/editPackage` | POST | `{ id: number, pid: number, pname: string }` | 未在前端消费（成功即可） | `src/pages/user/manageContent.vue` |

`UserFollowItem` 示例结构（来自 mock）：

```json
{
  "id": 1,
  "username": "张三",
  "avatarUrl": "/static/img/favicon.ico",
  "createTime": 1704034129,
  "email": "xx@xx.com",
  "mutual": true
}
```

## 3. Article 模块

| 接口 | 方法 | 请求参数（Body） | 返回格式（result） | 主要调用位置 |
|---|---|---|---|---|
| `/article/getArticleList` | POST | `{ pageSize: number, currentPage: number, filter: { keyword?: string, id?: number, pid?: number, tag?: string } }` | `{ list: ArticleListItem[], total: number }` | `src/pages/article/components/list.vue` |
| `/article/getArticleInfo` | POST | `{ aid: number \| string }` | `ArticleDetail` | `src/pages/article/index.vue`, `src/pages/article/addArticle.vue` |
| `/article/addArticle` | POST | `{ id: number, title: string, content: string, createTime: string, pid: number, tags: string[] }` | `{ aid: number }`（前端也按 `res.result.id` 取值，后端建议同时返回 `id`） | `src/pages/article/addArticle.vue` |
| `/article/editArticle` | POST | `{ id: number, title: string, content: string, createTime: string, pid: number, tags: string[] }` | `{ aid: number }`（同上建议兼容 `id`） | `src/pages/article/addArticle.vue` |
| `/article/deleteArticle` | POST | `{ aid: number }` | 未在前端消费（成功即可） | `src/pages/article/components/list.vue` |
| `/article/uploadImage` | POST | `multipart/form-data`：`file` | `{ url: string }` | `src/pages/article/addArticle.vue` |
| `/article/getTags` | GET | 无 | `{ list: string[] }` | `src/pages/article/addArticle.vue` |

`ArticleListItem` 示例结构：

```json
{
  "aid": 123,
  "member": { "username": "郎中", "id": "1231" },
  "title": "2023年终总结",
  "createTime": 1704034129,
  "viewCount": 0,
  "commentCount": 10,
  "abstract": "..."
}
```

`ArticleDetail` 示例结构：

```json
{
  "aid": 123,
  "member": { "id": "1231", "username": "郎中", "avatarUrl": "/static/img/favicon.ico" },
  "title": "水平布局",
  "createTime": 1704034129,
  "viewCount": 0,
  "commentCount": 10,
  "package": { "pid": 0, "pname": "全部" },
  "tags": ["css"],
  "content": "markdown..."
}
```

## 4. Comment 模块

| 接口 | 方法 | 请求参数（Body） | 返回格式（result） | 主要调用位置 |
|---|---|---|---|---|
| `/comment/getComments` | POST | 场景1按文章：`{ aid: number \| string, pageSize: number, currentPage: number }`；场景2按根评论：`{ root: number, pageSize: number, currentPage: number }`；场景3按用户：`{ id: number, pageSize: number, currentPage: number }` | `{ list: CommentItem[], total: number }` | `src/pages/article/index.vue`, `src/pages/article/components/singleComment.vue`, `src/pages/message/index.vue` |
| `/comment/addComment` | POST | `{ id: number, aid: number\|string, authorId: number\|string, createTime: string, useMD: boolean, content: { msg: string, member: object }, parent?: number, root?: number }` | 未在前端消费（成功后使用本地 params 更新 UI） | `src/pages/article/components/addComment.vue` |
| `/comment/editComment` | POST | 基于已有评论对象全量提交（至少包含 `cid/createTime/useMD/content.msg`） | 未在前端消费（成功后使用本地 params 更新 UI） | `src/pages/article/components/addComment.vue` |
| `/comment/deleteComment` | POST | `{ cid: number }` | 未在前端消费（成功后前端删除列表项） | `src/pages/article/components/singleComment.vue` |

`CommentItem` 示例结构（简化）：

```json
{
  "cid": 123,
  "aid": 123,
  "authorId": 432,
  "createTime": 1704034129,
  "useMD": true,
  "content": { "msg": "写得真好", "member": {} },
  "member": { "id": 1, "username": "张三", "avatarUrl": "/static/img/favicon.ico" },
  "comments": { "list": [], "total": 0 },
  "parent": 123,
  "root": 123
}
```

## 5. Api 模块

| 接口 | 方法 | 请求参数 | 返回格式（result） | 主要调用位置 |
|---|---|---|---|---|
| `/api/getConf` | GET | 无 | `{ data: string }`（RSA 公钥） | `src/utils/encrypt.ts` |

## 6. 备注（按项目现状）

- 下面接口在 mock 中未给出样例返回：`logout/register/updateUserInfo/sendEmailCode/checkPassword/checkEmailCode/uploadAvatar/switchFollow/deletePackage/editPackage/deleteArticle/uploadImage/addComment/editComment/deleteComment`。  
  前端当前都只依赖“成功/失败”，后端返回 `errorCode: 0` 即可满足现有逻辑。
- `addArticle/editArticle` 返回字段建议统一为 `{ id: number }`（或同时返回 `aid` 与 `id`），因为前端有 `res.result.id` 的读取。

