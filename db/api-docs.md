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

错误码见后端 `common/enums/ResultCodeEnum`：`-10004` 未登录、`-10005` 用户名错误、`-10006` 密码错误、`-10007` 用户名已用、`-10008` 邮箱已用、`-10009` 无权限、`-10010` 资源不存在、`-10011` 业务错误、`-10012` 参数校验失败、`-10013` 系统错误、`-10014` 登录失败（用户名或密码错误，不区分「用户不存在」以防枚举）、`-10015` 评论超过可编辑时间或已有回复、`-10016` 改密码时新密码与当前密码相同、`-10017` 登录失败次数过多被临时锁定、`-10018` 请求过于频繁（发验证码触发限流）。

## 2) 路由前缀

- 通用与认证：`/api/**`（`/api/auth/login`、`/api/auth/logout`）
- 用户域：`/user/**`
- 文章域：`/article/**`
- 评论域：`/comment/**`

> 所有接口都是明文 `application/json`。传输安全依赖 HTTPS —— **生产环境必须启用 TLS**（2026-09-07 移除了传输层 RSA，见 `项目完成度评估与计划书.md` A3）。
>
> **请求体大小**：非 multipart 请求 body 超 1MB（`blog.request-limit.max-body-size` 可调）直接 413 + `-10012`；multipart（图片上传）另见上传加固说明。文章正文 ≤ 10 万字符，评论正文 ≤ 1 万字符，标题 ≤ 64，超限 `-10012`。

## 3) API 列表

### 3.1 Common / Auth

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/api/auth/login` | POST（json） | 账号登录，写入 HttpOnly cookie | `login.vue` |
| `/api/auth/logout` | POST | 注销登录态 | `customHeader/index.vue` |

`/api/auth/login` 参数（JSON）：

| 参数 | 类型 | 必填 | 规则 |
|---|---|---|---|
| username | string | 是 | 用户名或邮箱，1~64 位，不校验格式（`globalRules.account`） |
| password | string | 是 | 非空、≤64 位（登录不校验密码格式） |

返回：`{ id, username, avatarUrl, createTime, email }`
登录失败统一返回 `-10014`（不区分用户不存在 / 密码错误）。`username` 含 `@` 按邮箱匹配，否则按用户名匹配。
限流：同一 `用户名+客户端IP` 连续失败 5 次锁定 15 分钟（内存计数，`blog.login-rate-limit.*` 可调），锁定期间返回 `-10017`；成功登录清零。

### 3.2 User（`/user`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/user/register` | POST（json） | 注册 | `register.vue` |
| `/user/checkEmailExist` | POST | 检查邮箱是否已注册 → `{ isRegistered }` | `register.vue`、`forgetPassword.vue` |
| `/user/getUserInfo` | GET `?id=` | 指定用户公开资料 | `manageContent.vue` |
| `/user/getMyProfile` | GET | 当前登录用户完整资料（含 email） | `App.vue`、`userCenter.vue` |
| `/user/updateUserInfo` | POST（json） | **需登录**，改用户名 / 邮箱（改邮箱需验证码）；不再处理密码 | `userCenter.vue` |
| `/user/checkPassword` | POST（json） | 校验当前用户密码 | `userCenter.vue`、`checkPwdDialog.vue` |
| `/user/updatePassword` | POST（json） | **需登录**，校验当前密码后改密；改完 `token_version+1`，所有旧 token 失效 | `userCenter.vue` |
| `/user/resetPassword` | POST（json） | **匿名**，邮箱验证码（scene=reset）+ 新密码；改完 `token_version+1` | `forgetPassword.vue` |
| `/user/deleteAccount` | POST（json） | **需登录**，注销账号：校验密码 → 软删（释放 email/username 给以后重新注册）→ `token_version+1` 全设备登出 → 清 cookie。不级联删除已发布文章/评论 | `deleteAccountDialog.vue` |
| `/user/getStat` | GET | 文章数/关注/粉丝 → `{ articleCount, followingCount, followerCount }` | `userCenter.vue` |
| `/user/getFollowings` | POST | 分页查询关注列表 | `follow.vue` |
| `/user/getFollowers` | POST | 分页查询粉丝列表 | `follow.vue` |
| `/user/sendEmailCode` | POST | 发送邮箱验证码（`scene`: register/reset/update_email） | `register.vue`、`forgetPassword.vue`、`userCenter.vue` |
| `/user/checkEmailCode` | POST | 校验邮箱验证码（不消费） | `forgetPassword.vue` |
| `/user/uploadAvatar` | POST（form-data） | 上传头像（`file` 或 base64 `image`），需登录；成功后清掉旧头像的 `file_asset` 行 + 磁盘文件 | `uploadAvatar.vue` |
| `/user/checkHasFollow` | GET `?id=` | 查询对目标用户的关注状态 → `{ isFollowing }` | `manageContent.vue` |
| `/user/switchFollow` | POST | 关注/取关，body `{ id, shouldFollow }` | `manageContent.vue`、`follow.vue` |
| `/user/getPackages` | GET `?id=` | 用户的文章分组列表 → `{ list: [{ pid, pname }] }` | `addArticle.vue`、`manageContent.vue` |
| `/user/addPackage` | POST | 新建分组，body `{ id, pname }` → `{ pid }` | `addArticle.vue`、`manageContent.vue` |
| `/user/editPackage` | POST | 改分组名，body `{ id, pid, pname }` | `manageContent.vue` |
| `/user/deletePackage` | POST | 删分组，body `{ id, pid }` | `manageContent.vue` |

分页查询（`getFollowings` / `getFollowers`）body：`{ pageSize, currentPage, filter: { keyword? } }`
返回：`{ list: [{ id, username, avatarUrl, createTime, isFollowing, isMutual }], total }`
> 不返回他人 `email`。`isFollowing` = 当前用户是否关注了 ta；`isMutual` = 互相关注。

`updateUserInfo` body（按场景传字段，需登录）：`username` / `email` + `verificationCode`。
改邮箱成功后 `token_version+1`（当前设备也需重新登录）。

`updatePassword` body：`{ currentPassword, newPassword }`（需登录）。当前密码错 → `-10006`；新密码与当前密码相同 → `-10016`。
`resetPassword` body：`{ email, verificationCode, newPassword }`（匿名）。验证码错 → `-10012`；邮箱不存在 → `-10005`。
> 改密 / 重置成功后 `token_version+1`，改密前签发的所有 JWT 立即失效 → 前端统一 `reLogin()`。

`deleteAccount` body：`{ password }`（需登录）。密码错 → `-10006`。`user_info.deleted_token` 设成自己的雪花 id
（天然唯一），`(email/username, 0)` 这个"活跃"槽位就空出来给以后重新注册。已注销用户 `getUserInfo`/`memberCard`
仍能查到（`isDeleted:true`），前端据此显示"该用户已注销"而不是直接 404——历史文章仍可浏览，只是账号本身
不可再登录/管理，也不能被新关注（`switchFollow` 会拒；取消已有关注不受影响）。

**安全审计日志**（`audit_log` 表 + `AuditLogService`，2026-09-11）：登录成功/失败、改密（含重置）、注销账号
都会落一行（`{ userId?, action, ip, detail?, createdAt }`），同时打一条 SLF4J INFO（`audit: action=... userId=...`）。
`action` 取值：`LOGIN_SUCCESS` / `LOGIN_FAILED` / `PASSWORD_CHANGED` / `PASSWORD_RESET` / `ACCOUNT_DELETED`。
`detail` 目前只在 `LOGIN_FAILED` 时填（提交的用户名/邮箱，不含密码）。写审计失败不影响主流程，只打 ERROR 日志。
没有配套的查询接口/管理页，现在只能直接查库。

**邮箱验证码加固**（`EmailVerificationCodeServiceImpl`，`blog.email-code.*` 可调）：
> - 6 位数字、10 分钟有效、bcrypt 存哈希
> - 单个验证码校验失败 5 次即作废（需重新获取）
> - 发送限流：同邮箱+场景 60s 冷却；同邮箱 ≤10 次/日；同 IP ≤10 次/时、≤30 次/日；命中返回 `-10018`
> - `email_verification_code` 表新增 `attempt_count`、`request_ip`
> - 发信：配了 `spring.mail.host` 就真发（dev 可指向 Mailhog:1025），否则打日志 `dev email verification code for ... : 123456`
> - `checkEmailCode` 只校验不消费；实际消费在 `register` / `updateUserInfo`(改邮箱) / `resetPassword` 里 `consumeCode`

**`token_version` 机制**：`user_info.token_version`（BIGINT，默认 0）。签发 JWT 时写入 `tv` claim；
`JwtAuthenticationFilter` 每次鉴权按 `userId` 查库比对 `tv`，不一致即拒绝（401 / `-10004`）。
改密码、改邮箱时 `token_version+1`，等效于「全设备登出」。滑动续期：token 剩余有效期 < 一半时自动重签 cookie。

`register` body：`{ email, username, password, verificationCode }`。username 1~20 位、不含 `@`；password 8~64 位、至少含字母；改用户名 / 改密码同此规则。

### 3.3 Article（`/article`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/article/getArticleList` | POST | 分页检索文章（仅 `status=published`） | `article/components/list.vue` |
| `/article/getArticleInfo` | GET `?aid=` | 单篇详情（并浏览量 +1，同一 IP 对同一篇文章 10 分钟内去重只计一次，`blog.article-view.*` 可调）；草稿一律 404 | `article/index.vue`、`addArticle.vue` |
| `/article/addArticle` | POST | 新建文章（直接发布）→ `{ id }` | `addArticle.vue` |
| `/article/editArticle` | POST | 编辑文章，body 含 `aid` → `{ id }`。目标是草稿行时＝发布该草稿（`status→published`、发布时间取现在） | `addArticle.vue` |
| `/article/saveDraft` | POST | 保存草稿：无 `aid` 新建、有 `aid` 更新自己的草稿 → `{ id }`。需登录。只校验标题非空（≤64），正文/分组/标签/摘要都可空。草稿不计入文章数统计 | `addArticle.vue` |
| `/article/getDraftList` | POST | 当前用户的草稿列表（按 `updated_at` 倒序），body `{ pageSize, currentPage, filter:{ keyword? } }`（keyword 匹配 title/abstract）→ 与 `getArticleList` 同形（`{ list:[{aid,id,member,title,createTime,viewCount:0,commentCount:0,abstract}], total }`），复用同一个 `articleList.vue` 组件渲染。需登录 | `article/components/articleList.vue`（`packageId==='draft'`） |
| `/article/getDraft` | GET `?aid=` | 取自己某篇草稿供编辑回填 → `{ aid, title, content, abstract, isCustomAbstract, pid, package:{pid,pname}, tags, updateTime }`。非本人 / 非草稿 → 404/403。不自增浏览量 | `addArticle.vue` |
| `/article/deleteArticle` | POST | 删除文章 / 草稿，body `{ aid }`（软删）。文章数是实时 COUNT，软删后自然不再计入 | `article/components/articleList.vue` |
| `/article/uploadImage` | POST（form-data） | 上传正文图片 → `{ url, imgUrl }`，需登录 | `addArticle.vue` |
| `/comment/uploadImage` | POST（form-data） | 上传评论插图 → `{ url, imgUrl }`，需登录（`asset_type=comment_image`） | `addComment.vue` |
| `/article/getTags` | GET | 标签列表 → `{ list: string[] }` | `addArticle.vue` |

`getArticleList` body：`{ pageSize, currentPage, filter: { keyword?, id?（作者）, pid?（分组）, tag? } }`
返回：`{ list: [{ aid, id, member:{id,username,avatarUrl}, title, createTime, viewCount, commentCount, abstract, coverUrl }], total }`
排序：带 `filter.id`（内容管理 / 个人空间）→ 按 `created_at` 倒序；不带（主页 feed）→ 按 `last_active_at` 倒序（发布 / 编辑正文 / 收到新评论都会刷新 `last_active_at`，把文章顶上来）。
`coverUrl`：正文第一张图（`ArticleCover.firstImage`，只认 markdown `![](url)`，跳过代码块/公式块），发布/编辑正文时重算；没图为 `null`。主页列表用它渲染缩略图。

`getArticleInfo` 返回（在列表项基础上多）：`{ ...列表项, updateTime, content, isCustomAbstract, pid, package:{pid,pname}, tags:string[] }`（评论列表走独立的 `POST /comment/getComments`，不在这个接口里，`commentCount` 已经在列表项字段里）
> `updateTime` = 文章最后编辑时间（秒）。浏览量自增不会改动它；仅 `editArticle` 会。
> 前端判定「编辑过」：`updateTime - createTime > 60`。
> `isCustomAbstract` = `abstract` 是否作者自填。前端编辑页据此决定是否把 `abstract` 回填到输入框（自动生成的不回填，留空 = 继续自动）。

`addArticle` / `editArticle` / `saveDraft` body：`{ id（作者，忽略）, aid（edit / 更新草稿时）, title, abstract?, content, pid, tags: string[] }`（`createTime` 后端忽略，发布时间由后端取 `now`）

**草稿（`status`）**：`blog.status` = `draft` / `published`（默认 `published`，老数据即已发布）。草稿仅作者可见（不进公开列表 / 详情、不计入文章数统计）。前端路由 `/write` 新建、`/draft/:aid` 编辑草稿、`/edit/:aid` 编辑已发布文章；`addArticle.vue` 底部「保存」→ `saveDraft`，「发布」→ `addArticle`（新建）或 `editArticle`（草稿提升 / 文章改动）。内容管理页「草稿箱」是左栏 / 移动端下拉框里单独渲染的一条（哨兵 `pid='draft'`，排「全部」前、仅非访客，不进 `packageList` 数组），选中时 `articleList.vue` 改打 `getDraftList` 并把行内链接指向 `/draft/:aid`。

**摘要 `abstract` 生成规则**（`ArticleAbstract`，只有一个概念、一个字段名 `abstract`）：
> 1. 请求带 `abstract`（非空，≤200）→ 存作者原文（剥 markdown，`is_custom_abstract=1`），**不加省略号**
> 2. 未带 → 正文里有 `<!-- more -->` → 取它之前的（截 200），**补「…」**
> 3. 未带、也没 `<!-- more -->` → 整篇正文自动截取（120，词边界断开）；**只有真被截断才补「…」**，短正文原样
>
> 2/3 都会先剥 markdown（`#`、`**`、`[]()`、`![]()`、代码块、`$$公式块$$`、引用、列表符…）+ 压缩空白，`is_custom_abstract=0`。
> 编辑时改正文,`is_custom_abstract=0` 的会重算,`=1` 的不动(作者说了算)。
> 省略号由后端决定；前端直接渲染 `abstract`，为空则列表不显示摘要行。
> DB：`blog.abstract`（展示串）+ `blog.is_custom_abstract`。改了生成逻辑后老文章的自动摘要不会自动更新 —— 用 `AbstractBackfill`（test 目录，一次性脚本）重算。

### 3.4 Comment（`/comment`）

| 接口 | 方法 | 说明 | 主要调用 |
|---|---|---|---|
| `/comment/getCommentInbox` | POST | 当前用户评论收件箱分页 | `message/index.vue` |
| `/comment/getComments` | POST | 文章评论 / 某根评论的回复分页 | `article/index.vue`、`singleComment.vue` |
| `/comment/addComment` | POST | 发布评论或回复 → `{ cid }` | `addComment.vue` |
| `/comment/editComment` | POST | 编辑评论，body 含 `aid`、`cid`；**仅本人、发布 5 分钟内、且无回复**可编辑，否则 `-10015` | `addComment.vue` |
| `/comment/deleteComment` | POST | 删除评论，body `{ aid, cid }` | `singleComment.vue` |

`getCommentInbox` body：`{ pageSize, currentPage }`（作者身份取自登录态）
`getComments` body：`{ aid, root?, pageSize, currentPage }`
> 无 `root`（列根评论）：每条根评论的 `comments` 内联返回**前 2 条子回复** + `total`（`PREVIEW_REPLIES`）。前端不再逐条挂载请求；`total > 已展示数` 时显示「展开 N 条回复」，点击才带 `root` 拉全量（分页）。
> 有 `root`（列某根评论的回复）：返回该根下的全部子回复（分页），子项不再嵌套 `comments`。
`addComment` body：`{ aid, authorId, isMarkdown, content:{msg}, parent?, root? }`
> 评论插图走独立的 `POST /comment/uploadImage`（`asset_type=comment_image`），与文章插图区分。

**图片上传加固**（`ImageStorageService`，头像 / 文章图 / 评论图共用）：
> - 大小上限：头像 2MB、文章 / 评论图 5MB（`blog.upload.*` 可调）；超限 multipart 层直接 413/`-10012`
> - 不信任客户端文件名 / Content-Type：格式由 ImageIO 从字节流嗅探
> - 只收 jpeg / png / gif，且必须能被 ImageIO 真正解码（挡 `.svg` / `.html` / 脚本 / 畸形文件）
> - 限制解码后像素总数（默认 6000 万）挡「解压炸弹」
> - png / jpeg 重新编码落盘（抹掉 EXIF、polyglot 载荷）；gif 原样保留（动图）
> - 文件名 = 雪花 ID + 规范化扩展名；`file_asset` 记服务端判定的 `mime_type` / `width` / `height`
> - `/uploads/**` 响应带 `X-Content-Type-Options: nosniff`（Spring Security 默认）
返回项的 `content.member`（`{id,username,avatarUrl}`）：仅「回复的回复」（`parent != root`）时出现，= 父评论作者，前端据此渲染「回复 @xxx：」。后端按 `parent_id` 现算（忽略父评论逻辑删除，删了也照常显示 @），无需前端上传。
`editComment` body：`{ aid, cid, isMarkdown, content:{msg} }`
返回项结构：`{ cid, aid, authorId, title, createTime, updateTime, canEdit, isMarkdown, content:{msg}, member:{id,username,avatarUrl}, parent, parentContent:{msg, deleted?}, root, comments:{list,total} }`
> `parentContent.deleted = true`：被回复的父评论已被删除（前端显示「评论已删除」）。父评论为 null（顶级评论）时 `parentContent = {msg:""}`。
> `updateTime` = 评论最后编辑时间（秒）；前端 `updateTime - createTime > 60` 时显示「(已编辑)」。
> `canEdit` = 当前登录用户是否可编辑此评论（本人 + 发布 5 分钟内 + 无回复）。前端据此决定「编辑」菜单项是否出现，「删除」始终有。

> 已修的契约问题：
> 1. **已修（2026-08-29）**：`content` 请求体形状 —— 后端 `CommentRequest.content` 由 `String` 改为对象 `{msg}`（取 `content.msg` 作正文，多余字段如 `member` 忽略），与前端及响应形状对齐。
> 2. **已修（2026-08-28）**：雪花 ID 精度 —— 后端 `JacksonConfiguration` 把超出 JS 安全范围（2^53）的 `Long` 序列化为**字符串**。因此所有 `id`/`aid`/`cid`/`pid` 等在响应里是字符串；`createTime`/`updateTime`/`total`/`viewCount` 等小数值仍是数字。请求侧发字符串或数字均可（后端 `parseId` 兼容）。

## 4) 与 mock 的对应规则

开发环境 mock 由 `src/utils/mockServer.ts` 处理：按 `mock/<模块>/<动词>/<method>.json` 读取。

示例：

- `GET /article/getArticleInfo?aid=123` → `src/mock/article/getArticleInfo/get.json`
- `POST /user/editPackage` → `src/mock/user/editPackage/post.json`

## 5) 建议

- 新增/编辑文章统一返回 `{ id: number }`，前端跳转逻辑已兼容 `id/aid`。
- 安全整改项（JWT 失效、登录限流、验证码加固、文件上传校验、A7/A8 改密拆分等）见 `项目完成度评估与计划书.md`。
