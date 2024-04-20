<template>
<div class="a-full">
    <custom-header hide-search hide-write></custom-header>
    <main class="a-pt-header a-full">
        <div class="a-p-lg a-full container clearfix">
            <el-form :model="form" ref="formRef" :rules="rules" label-width="90px" class="a-full">
                <el-form-item size="large" :label="$t('label.title')" prop="title">
                    <div class="title-div">
                        <el-input v-model="form.title" maxlength="64" minlength="1"></el-input>
                        <el-button @click="onSubmit(formRef)" class="a-ml-md" type="warning">{{$t('label.publish')}}</el-button>
                    </div>
                </el-form-item>
                <el-form-item :label="$t('label.package')" prop="package">
                    <el-select
                        v-model="form.package"
                        filterable
                        default-first-option>
                        <el-option
                            v-for="item in packageOptions"
                            :key="item[ArticleConstant.packageId]"
                            :label="item[ArticleConstant.packageName]"
                            :value="item[ArticleConstant.packageId]"/>
                    </el-select>
                    <el-button size="small"
                               link
                               type="primary"
                               @click="addPackage"
                               class="a-ml-xs">
                        {{$t('label.createOption')}}
                    </el-button>
                </el-form-item>
                <el-form-item :label="$t('label.tag')" prop="tags">
                    <el-select
                        v-model="form.tags"
                        filterable
                        multiple
                        allow-create
                        default-first-option>
                        <el-option
                            v-for="item in tagOptions"
                            :key="item"
                            :label="item"
                            :value="item"/>
                    </el-select>
                    <span class="a-ml-xs a-font-body-2">{{$t('inputMessage.enterToCreate')}}</span>
                </el-form-item>
                <el-form-item :label="$t('label.mainBody')" prop="content" class="form-item-content">
                    <v-md-editor left-toolbar="undo redo clear | h emoji bold italic strikethrough quote | ul ol table todo-list hr | link image code tip"
                                 :placeholder="$t('inputMessage.useMarkdown')"
                                 :disabled-menus="[]"
                                 @upload-image="handleUploadImage"
                                 @change="validateContent"
                                 v-model="form.content">
                    </v-md-editor>
                </el-form-item>
            </el-form>
        </div>
    </main>
</div>
</template>

<script lang="ts" setup name="AddArticle">
import CustomHeader from '@/components/customHeader/index.vue';
import {getCurrentInstance, onMounted, reactive, ref} from 'vue';
import {useUserStore} from '@/stores/user';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox, FormInstance, FormRules} from 'element-plus';
import ArticleConstant from '@/model/article/constant';
import UserConstant, {defaultPackage} from '@/model/user/constant';
import i18n from '@/language/i18n';
import {globalRules} from '@/utils/validate';

const props = defineProps({
    articleId: [Number, String]
});

interface ArticleForm {
    title: string,
    content: string,
    package: number,
    tags: []
}
const userStore = useUserStore();
const router = useRouter();
const { proxy }: any = getCurrentInstance();
const { t } = i18n.global as any;
const formRef = ref<FormInstance>();
const form = reactive<ArticleForm>({
    title: '',
    content: '',
    package: 1,
    tags: []
});

const validateTags = (rule: any, value: Array<string>, callback: any) => {
    const invalid = value.some(i => i.length > 30);
    invalid ? callback(new Error(t('inputMessage.tagFormat'))) : callback();
};
const rules = reactive<FormRules<ArticleForm>>({
    title: [
        { required: true, message: t('inputMessage.titleFormat'), trigger: 'change'},
        { pattern: /^\S.{0,63}$/, message: t('inputMessage.titleFormat'), trigger: 'blur' }
    ],
    package: globalRules.required,
    tags: [...globalRules.required, { validator: validateTags, trigger: 'blur' }],
    content: globalRules.required
});
const validateContent = () => {
    if (!formRef.value) return;
    formRef.value.validateField('content');
};

let isQuerying = ref(false);
const onSubmit =  (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    formEl.validate(async (valid) => {
        if (valid) {
            isQuerying.value = true;
            const url = props.articleId ? ArticleConstant.url.edit : ArticleConstant.url.add;
            proxy.$request.post(url, {
                [ArticleConstant.userId]: userStore.id,
                [ArticleConstant.title]: form.title,
                [ArticleConstant.content]: form.content,
                [ArticleConstant.createTime]: props.articleId ? originArticle[ArticleConstant.createTime] : (new Date().getTime() / 1000).toFixed(),
                [ArticleConstant.packageId]: form.package,
                [ArticleConstant.tags]: form.tags
            }).then(res => {
                isQuerying.value = false;
                router.push({path: `/article/${res.result.id}`});
            }).catch(err => {
                isQuerying.value = false;
            });
        }
    });
};

const packageOptions = ref([defaultPackage]);
const getPackages = () => {
    proxy.$request.post(UserConstant.url.getPackages, {
        [UserConstant.userId]: userStore.id
    }).then(res => {
        packageOptions.value = [defaultPackage, ...res.result.list];
    });
};
const addPackage = () => {
    ElMessageBox.prompt(t('inputMessage.inputPackageName'), t('label.tip'), {
        inputPattern: /^\S.{0,63}$/,
        inputErrorMessage: t('inputMessage.invalidInput')
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
        headers: {'Content-Type': 'multipart/form-data'}
    }).then(res => {
        insertImage({
            url: res.result.url,
            desc: ''
        });
    });
}

let originArticle = null;
const getArticleInfo = () => {
    proxy.$request.post(ArticleConstant.url.getArticleInfo, {
        [ArticleConstant.articleId]: props.articleId
    }).then(({result}) => {
        originArticle = result;
        form.content = result[ArticleConstant.content];
        form.title = result[ArticleConstant.title];
        form.package = result[ArticleConstant.package][ArticleConstant.packageId];
        form.tags = result[ArticleConstant.tags];
    });
};

onMounted(() => {
    if(props.articleId) {
        getArticleInfo();
    }
    getPackages();
    getTags();
});
</script>

<style scoped lang="scss">
.container {
    min-width: 960px;
    max-width: 1424px;
    min-height: 619px;
    margin: 0 auto;
}

.title-div {
    display: flex;
    width: 100%;
    .el-input {
        flex-grow: 1;
    }
}
.v-md-editor {
    min-height: 400px;
    height: 100%;
}
.el-form-item.is-error .v-md-editor {
    box-shadow: 0 0 0 1px $--color-danger inset;
}
.el-select {
    width: 280px;
}
.el-form {
    display: flex;
    flex-direction: column;
    .form-item-content {
        flex-grow: 1;
    }
}
</style>
