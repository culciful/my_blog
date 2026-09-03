# vue3-ts-blog API 文档（RPC 命名版）

> 依据前端 `src/model/*/constant.ts` 与后端 controller 整理。
> 命名约定：`/<模块>/<动词短语>`，扁平路径不含 id；纯查询用 `GET`（参数走 query），其余一律 `POST`（参数走 body）。

## 1) 统一响应约定

前端请求封装在 `src/utils/request.ts`，统一按以下结构处理：

```json
{ "errorCode": 0, "result": {} }
```

- `errorCode === 0` 视为成功
- 文档中的“返回”均表示 `result` 字段结构
- 标注“仅成功/失败”的接口，前端不消费 `result` 内容
- **ID 字段是字符串**（`id`/`aid`/`cid`/`pid`…，雪花 ID 防精度丢失）；`createTime`/`updateTime` 是**绝对时间戳（秒）**，前端按浏览器时区展示

错误码见后端 `common/enums/ResultCodeEnum`：`-10004` 未登录、`-10005` 用户名错误、`-10006` 密码错误、`-10007` 用户名已用、`-10008` 邮箱已用、`-10009` 无权限、`-10010` 资源不存在、`-10011` 业务错误、`-10012` 参数校验失败、`-10013` 系统错误。

## 2) 路由前缀

- 通用与认证：`/api/**`（`/api/getConf`、`/api/auth/login`、`/api/auth/logout`）
- 用户域：`/user/**`
- 文章域：`/article/**`
- 评论域：`/comment/**`

需前端 RSA 加密后以 `text/plain` 传输的接口（`request.ts` 拦截器处理）：
`/api/auth/login`、`/user/register`、`/user/updateUserInfo`、`/user/checkPassword`。

## 3) API 列表

### 3.1 Common / Auth

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/api/getConf` | GET | 获取 RSA 公钥（登录/注册加密用） | `utils/encrypt.ts` |
| `/api/auth/login` | POST（text/plain 加密） | 账号登录，写入 HttpOnly cookie | `login.vue` |
| `/api/auth/logout` | POST | 注销登录态 | `customHeader/index.vue` |

`/api/getConf` 返回：`{ "data": "-----BEGIN PUBLIC KEY-----..." }`

`/api/auth/login` 参数（加密前的 JSON）：

| 参数 | 类型 | 必填 | 规则 |
|---|---|---|---|
| username | string | 是 | 1~16 位；`patterns.username` |
| password | string | 是 | 6~64 位；至少含字母 |

返回：`{ id, username, avatarUrl, createTime, email }`

### 3.2 User（`/user`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/user/register` | POST（text/plain 加密） | 注册 | `register.vue` |
| `/user/checkEmailExist` | POST | 检查邮箱是否已注册 → `{ isExisted }` | `register.vue`、`forgetPassword.vue` |
| `/user/getUserInfo` | GET `?id=` | 指定用户公开资料 | `manageContent.vue` |
| `/user/getMyProfile` | GET | 当前登录用户完整资料（含 email） | `App.vue`、`userCenter.vue` |
| `/user/updateUserInfo` | POST（json 或 text/plain 加密） | 改用户名/邮箱/密码；未登录时走邮箱重置密码 | `userCenter.vue`、`forgetPassword.vue` |
| `/user/checkPassword` | POST（json 或 text/plain 加密） | 校验当前用户密码 | `userCenter.vue`、`checkPwdDialog.vue` |
| `/user/getStat` | GET | 文章数/关注/粉丝 → `{ articleCount, following, follower }` | `userCenter.vue` |
| `/user/getFollowings` | POST | 分页查询关注列表 | `follow.vue` |
| `/user/getFollowers` | POST | 分页查询粉丝列表 | `follow.vue` |
| `/user/sendEmailCode` | POST | 发送邮箱验证码（`scene`: register/reset/update_email） | `register.vue`、`forgetPassword.vue`、`userCenter.vue` |
| `/user/checkEmailCode` | POST | 校验邮箱验证码 | `forgetPassword.vue` |
| `/user/uploadAvatar` | POST（form-data） | 上传头像（`file` 或 base64 `image`） | `uploadAvatar.vue` |
| `/user/checkHasFollow` | GET `?id=` | 查询对目标用户的关注状态 → `{ data: boolean }` | `manageContent.vue` |
| `/user/switchFollow` | POST | 关注/取关，body `{ id, value }` | `manageContent.vue`、`follow.vue` |
| `/user/getPackages` | GET `?id=` | 用户的文章分组列表 → `{ list: [{ pid, pname }] }` | `addArticle.vue`、`manageContent.vue` |
| `/user/addPackage` | POST | 新建分组，body `{ id, pname }` → `{ pid }` | `addArticle.vue`、`manageContent.vue` |
| `/user/editPackage` | POST | 改分组名，body `{ id, pid, pname }` | `manageContent.vue` |
| `/user/deletePackage` | POST | 删分组，body `{ id, pid }` | `manageContent.vue` |

分页查询（`getFollowings` / `getFollowers`）body：`{ pageSize, currentPage, filter: { keyword? } }`
返回：`{ list: [{ id, username, avatarUrl, createTime, email, mutual }], total }`

`updateUserInfo` body（按场景传字段）：`username` / `email` + `verificationCode` / `password`。
> ⚠ 安全整改（见 `项目完成度评估与计划书.md` A7/A8）：此接口后续将拆为
> `POST /user/updatePassword`（登录 + 校验当前密码）与 `POST /user/resetPassword`（匿名 + 邮箱验证码）。

`register` body：`{ email, username, password, verificationCode }`（正则同登录 + 邮箱格式）。

### 3.3 Article（`/article`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/article/getArticleList` | POST | 分页检索文章 | `article/components/list.vue` |
| `/article/getArticleInfo` | GET `?aid=` | 单篇详情（并浏览量 +1） | `article/index.vue`、`addArticle.vue` |
| `/article/addArticle` | POST | 新建文章 → `{ id }` | `addArticle.vue` |
| `/article/editArticle` | POST | 编辑文章，body 含 `aid` → `{ id }` | `addArticle.vue` |
| `/article/deleteArticle` | POST | 删除文章，body `{ aid }` | `article/components/list.vue` |
| `/article/uploadImage` | POST（form-data） | 上传正文图片 → `{ url, imgUrl }` | `addArticle.vue` |
| `/article/getTags` | GET | 标签列表 → `{ list: string[] }` | `addArticle.vue` |

`getArticleList` body：`{ pageSize, currentPage, filter: { keyword?, id?（作者）, pid?（分组）, tag? } }`
返回：`{ list: [{ aid, id, member:{id,username,avatarUrl}, title, createTime, viewCount, commentCount, abstract }], total }`

`getArticleInfo` 返回（在列表项基础上多）：`{ ...列表项, updateTime, content, pid, package:{pid,pname}, tags:string[], comments:{list,total} }`
> `updateTime` = 文章最后编辑时间（秒）。浏览量自增不会改动它；仅 `editArticle` 会。
> 前端判定「编辑过」：`updateTime - createTime > 60`。

`addArticle` / `editArticle` body：`{ id（作者，忽略）, aid（仅 edit）, title, content, createTime, pid, tags: string[] }`

### 3.4 Comment（`/comment`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/comment/getCommentInbox` | POST | 当前用户评论收件箱分页 | `message/index.vue` |
| `/comment/getComments` | POST | 文章评论 / 某根评论的回复分页 | `article/index.vue`、`singleComment.vue` |
| `/comment/addComment` | POST | 发布评论或回复 | `addComment.vue` |
| `/comment/editComment` | POST | 编辑评论，body 含 `aid`、`cid` | `addComment.vue` |
| `/comment/deleteComment` | POST | 删除评论，body `{ aid, cid }` | `singleComment.vue` |

`getCommentInbox` body：`{ pageSize, currentPage }`（作者身份取自登录态）
`getComments` body：`{ aid, root?, pageSize, currentPage }`（有 `root` 时查该根评论的回复，否则查根评论）
`addComment` body：`{ aid, authorId, useMD, content, parent?, root? }`
返回项结构：`{ cid, aid, authorId, title, createTime, updateTime, useMD, content:{msg}, member:{id,username,avatarUrl}, parent, parentContent:{msg}, root, comments:{list,total} }`
> `updateTime` = 评论最后编辑时间（秒）；前端 `updateTime - createTime > 60` 时显示「(已编辑)」。

> ⚠ 已知契约问题：
> 1. **未修**：前端 `addComment`/`editComment` 目前把 `content` 作为对象 `{msg, member}` 传输，后端 `CommentRequest.content` 为 `String` → 真实后端会反序列化失败。
> 2. **已修（2026-08-28）**：雪花 ID 精度 —— 后端 `JacksonConfiguration` 把超出 JS 安全范围（2^53）的 `Long` 序列化为**字符串**。因此所有 `id`/`aid`/`cid`/`pid` 等在响应里是字符串；`createTime`/`updateTime`/`total`/`viewCount` 等小数值仍是数字。请求侧发字符串或数字均可（后端 `parseId` 兼容）。

## 4) 与 mock 的对应规则

开发环境 mock 由 `src/utils/mockServer.ts` 处理：按 `mock/<模块>/<动词>/<method>.json` 读取。

示例：

- `GET /article/getArticleInfo?aid=123` → `src/mock/article/getArticleInfo/get.json`
- `POST /user/editPackage` → `src/mock/user/editPackage/post.json`

## 5) 建议

- 新增/编辑文章统一返回 `{ id: number }`，前端跳转逻辑已兼容 `id/aid`。
- 安全整改项（JWT 失效、登录限流、验证码加固、文件上传校验、A7/A8 改密拆分等）见 `项目完成度评估与计划书.md`。
