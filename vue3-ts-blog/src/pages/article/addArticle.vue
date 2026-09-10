<template>
    <div class="a-full">
        <custom-header hide-search hide-write></custom-header>
        <main class="a-pt-header a-full">
            <div class="a-p-lg a-full container clearfix">
                <el-form :model="form" ref="formRef" :rules="rules" label-width="90px" class="a-full">
                    <el-form-item :label="$t('label.title')" prop="title">
                        <el-input v-model="form.title" maxlength="64"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('label.package')" prop="package">
                        <el-select v-model="form.package" filterable default-first-option>
                            <el-option v-for="item in packageOptions" :key="item[ArticleConstant.packageId]"
                                :label="item[ArticleConstant.packageName]" :value="item[ArticleConstant.packageId]" />
                        </el-select>
                        <el-button size="small" link type="primary" @click="addPackage" class="a-ml-xs">
                            {{ $t('label.createOption') }}
                        </el-button>
                    </el-form-item>
                    <el-form-item :label="$t('label.tag')" prop="tags">
                        <el-select v-model="form.tags" filterable multiple allow-create default-first-option>
                            <el-option v-for="item in tagOptions" :key="item" :label="item" :value="item" />
                        </el-select>
                        <span class="a-ml-xs a-font-body-2">{{ $t('inputMessage.enterToCreate') }}</span>
                    </el-form-item>
                    <el-form-item :label="$t('label.abstract')" prop="abstract">
                        <el-input v-model="form.abstract" type="textarea" :autosize="{ minRows: 1, maxRows: 3 }"
                            maxlength="200" show-word-limit :placeholder="$t('inputMessage.abstractHint')" />
                    </el-form-item>
                    <el-form-item :label="$t('label.mainBody')" prop="content" class="form-item-content">
                        <v-md-editor
                            left-toolbar="undo redo clear | h emoji bold italic strikethrough quote | ul ol table todo-list hr | link image code tip"
                            :placeholder="$t('inputMessage.useMarkdown')" :disabled-menus="[]"
                            @upload-image="handleUploadImage" @blur="validateContent" v-model="form.content">
                        </v-md-editor>
                    </el-form-item>
                    <div class="flex-end">
                        <el-button v-if="canSaveDraft" plain :loading="isSavingDraft" @click="saveDraftHandler">
                            {{ $t('label.save') }}
                        </el-button>
                        <el-button type="warning" :loading="isQuerying" @click="onSubmit(formRef)">
                            {{ $t('label.publish') }}
                        </el-button>
                    </div>
                </el-form>
            </div>
        </main>
    </div>
</template>

<script lang="ts" setup name="AddArticle">
import CustomHeader from '@/components/customHeader/index.vue';
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue';
import { useUserStore } from '@/stores/user';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus';
import ArticleConstant from '@/model/article/constant';
import UserConstant, { defaultPackage } from '@/model/user/constant';
import i18n from '@/language/i18n';
import { globalRules } from '@/utils/validate';

const props = defineProps({
    articleId: [Number, String]
});

interface ArticleForm {
    title: string,
    abstract: string,
    content: string,
    package: number,
    tags: []
}
const userStore = useUserStore();
const router = useRouter();
const route = useRoute();
const { proxy }: any = getCurrentInstance();
const { t } = i18n.global as any;
const formRef = ref<FormInstance>();

// 三种进入方式：/write 新建、/draft/:id 编辑草稿、/edit/:id 编辑已发布文章
const isEditingPublished = route.name === 'edit';
// 已发布文章不给「保存草稿」（只有发布/保存修改）；新建和草稿都给
const canSaveDraft = computed(() => !isEditingPublished);
// 有值 = 当前内容对应一条草稿行（/draft 进来，或 /write 存过一次草稿）
const draftId = ref<string | number | null>(route.name === 'editDraft' ? props.articleId ?? null : null);
const isSavingDraft = ref(false);
const form = reactive<ArticleForm>({
    title: '',
    abstract: '',
    content: '',
    package: 0,
    tags: []
});

const validateTags = (rule: any, value: Array<string>, callback: any) => {
    const invalid = value.some(i => i.length > 30);
    invalid ? callback(new Error(t('inputMessage.tagFormat'))) : callback();
};
const validateContentTooLong = (rule: any, value: string, callback: any) => {
    const invalid = value.length > 100_000;
    invalid ? callback(new Error(t('inputMessage.contentTooLong'))) : callback();
};
const rules = reactive<FormRules<ArticleForm>>({
    title: globalRules.required,
    package: globalRules.required,
    tags: [{ validator: validateTags, trigger: 'blur' }],
    content: [...globalRules.required, { validator: validateContentTooLong, trigger: 'blur' }]
});

const validateContent = () => {
    if (!formRef.value) return;
    formRef.value.validateField('content');
};

const buildPayload = () => ({
    [ArticleConstant.userId]: userStore.id,
    [ArticleConstant.title]: form.title,
    [ArticleConstant.abstract]: form.abstract,
    [ArticleConstant.content]: form.content,
    [ArticleConstant.packageId]: form.package,
    [ArticleConstant.tags]: form.tags
});

let isQuerying = ref(false);
// 发布：新建 → addArticle；草稿 / 已发布文章 → editArticle（后端见到草稿行会「提升为已发布」）
const onSubmit = (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    formEl.validate(async (valid) => {
        if (!valid) return;
        isQuerying.value = true;
        const payload = buildPayload();
        const targetAid = draftId.value ?? (isEditingPublished ? props.articleId : null);
        const req = targetAid
            ? proxy.$request.post(ArticleConstant.url.edit, { ...payload, [ArticleConstant.articleId]: targetAid })
            : proxy.$request.post(ArticleConstant.url.add, payload);
        req.then(res => {
            isQuerying.value = false;
            const aid = res.result[ArticleConstant.articleId] ?? res.result.id;
            ElMessage.success(t('infoMessage.publishSuccess'));
            router.push({ path: `/article/${aid}` });
        }).catch(() => {
            isQuerying.value = false;
        });
    });
};

// 保存草稿
const saveDraftHandler = () => {
    formRef.value?.validate(async (valid) => {
        if (!valid) return;
        isSavingDraft.value = true;
        const payload: Record<string, unknown> = buildPayload();
        if (draftId.value) payload[ArticleConstant.articleId] = draftId.value;
        proxy.$request.post(ArticleConstant.url.saveDraft, payload).then(res => {
            isSavingDraft.value = false;
            const id = res.result[ArticleConstant.articleId] ?? res.result.id;
            ElMessage.success(t('infoMessage.draftSaved'));
            if (!draftId.value) {
                draftId.value = id;
                // 换掉地址栏但不重新导航，刷新 / 再次保存都指向这条草稿
                window.history.replaceState(window.history.state, '', `/draft/${id}`);
            }
        }).catch(() => {
            isSavingDraft.value = false;
        });
    });
};

const packageOptions = ref([defaultPackage]);
const getPackages = () => {
    proxy.$request.get(UserConstant.url.getPackages, { [UserConstant.userId]: userStore.id }).then(res => {
        packageOptions.value = [defaultPackage, ...res.result.list];
    });
};
const addPackage = () => {
    ElMessageBox.prompt(t('inputMessage.inputPackageName'), t('label.tip'), {
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.packageNameLimit')
    }).then(({ value }) => {
        proxy.$request.post(UserConstant.url.addPackage, {
            [UserConstant.userId]: userStore.id,
            [UserConstant.packageName]: value
        }).then(res => {
            ElMessage({
                type: 'success',
                message: t('infoMessage.addSuccess')
            });
            getPackages();
        });
    });
};

const tagOptions = ref([]);
const getTags = () => {
    proxy.$request.get(ArticleConstant.url.getTags).then(res => {
        tagOptions.value = res.result.list;
    });
};

function handleUploadImage(event, insertImage, files) {
    let formData = new FormData();
    formData.append('file', files[0]);
    proxy.$request.post(ArticleConstant.url.uploadImage, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => {
        insertImage({
            url: res.result.url,
            desc: ''
        });
    });
}

const fillForm = (result: Record<string, any>) => {
    form.content = result[ArticleConstant.content] || '';
    form.title = result[ArticleConstant.title] || '';
    // 只在作者自填过时回填；自动生成的不回填，留空 = 继续自动
    form.abstract = result[ArticleConstant.isCustomAbstract] ? (result[ArticleConstant.abstract] || '') : '';
    form.package = result[ArticleConstant.package]?.[ArticleConstant.packageId] ?? 0;
    form.tags = result[ArticleConstant.tags] || [];
};
const getArticleInfo = () => {
    proxy.$request.get(ArticleConstant.url.getArticleInfo, { [ArticleConstant.articleId]: props.articleId })
        .then(({ result }) => fillForm(result));
};
const getDraft = () => {
    proxy.$request.get(ArticleConstant.url.getDraft, { [ArticleConstant.articleId]: props.articleId })
        .then(({ result }) => fillForm(result))
        .catch(() => router.replace('/user/manageContent'));
};

onMounted(() => {
    if (route.name === 'edit') {
        getArticleInfo();
    } else if (route.name === 'editDraft') {
        getDraft();
    }
    getPackages();
    getTags();
});
</script>

<style scoped lang="scss">
.container {
    min-width: 960px;
    max-width: 1424px;
    min-height: 720px;
    margin: 0 auto;
}

.el-form {
    display: flex;
    flex-direction: column;
}

.form-item-content {
    flex: 1;
    min-height: 0;

    :deep(.el-form-item__content) {
        height: 100%;
        min-height: 0;
    }
}

.v-md-editor {
    height: 100%;
}

.el-form-item.is-error .v-md-editor {
    box-shadow: 0 0 0 1px $--color-danger inset;
}

.el-select {
    width: 280px;
}
</style>
