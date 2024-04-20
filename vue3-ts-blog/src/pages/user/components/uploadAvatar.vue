<template>
<el-dialog
    class="upload-dialog"
    v-model="show"
    top="30vh"
    :title="$t('label.uploadAvatar')"
    :before-close="handleClose">
    <template v-if="active===1">
        <el-upload
            ref="uploadRef"
            action="#"
            :accept="acceptType"
            drag
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleChange"
        >
            <svg-icon name="cloud-upload" :size="64" color="#a8abb2"></svg-icon>
            <div class="el-upload__text">
                {{$t('inputMessage.uploadAvatarText')}}
            </div>
            <template #tip>
                <div class="el-upload__tip">
                    {{$t('inputMessage.uploadAvatarTip')}}
                </div>
            </template>
        </el-upload>
    </template>
    <template v-else>
        <div class="step2">
            <div style="height:250px;width: 250px">
                <vueCropper ref="cropperRef" 
                            @realTime="realTime" 
                            :img="cropperOption.img" 
                            :canMove="cropperOption.canMove"
                            :centerBox="cropperOption.centerBox"
                            :autoCrop="cropperOption.autoCrop" 
                            :autoCropWidth="cropperOption.autoCropWidth"
                            :autoCropHeight="cropperOption.autoCropHeight" 
                            :fixed="cropperOption.fixed"
                            :outputType="cropperOption.outputType" />
            </div>
            <div class="preview-container">
                <div :style="previewStyle">
                    <div :style="preview.div">
                        <img :src="preview.url" alt="" :style="preview.img">
                    </div>
                </div>
                <p class="a-font-body-2 a-mt-sm">{{$t('label.previewAvatar')}}</p>
            </div>
        </div>
    </template>
    <template #footer>
        <el-button v-if="active===1" @click="close">{{$t('label.cancel')}}</el-button>
        <template v-else>
            <el-button @click="active=1" :disabled="isQuerying">
                {{$t('label.lastStep')}}
            </el-button>
            <el-button type="primary" @click="confirm" :loading="isQuerying">
                {{$t('label.confirm')}}
            </el-button>
        </template>
    </template>
</el-dialog>
</template>

<script lang="ts" setup name="UploadAvatar">
import 'vue-cropper/dist/index.css';
import { VueCropper }  from 'vue-cropper';
import {ElMessage} from 'element-plus';
import type {UploadProps} from 'element-plus';
import {getCurrentInstance, reactive, ref, watch} from 'vue';
import i18n from '@/language/i18n';
import Constant from '@/model/user/constant';

const {proxy} = getCurrentInstance();
const { t } = i18n.global as any;
const props = defineProps({
    modelValue: Boolean
});
const emit = defineEmits(['update:modelValue', 'success']);

const show = ref(false);
watch(() => props.modelValue, val => {
    show.value = val;
});

const isQuerying = ref(false);
const active = ref(1);  // 1 upload 2 modify

const cropperRef = ref();
const cropperOption = reactive({
    img: '', // 裁剪图片的地址
    canMove: false, // 上传图片是否可以移动  (默认:true)
    centerBox: true, // 截图框是否被限制在图片里面  (默认:false)
    autoCrop: true, // 是否默认生成截图框  (默认:false)
    autoCropWidth: 150, // 默认生成截图框宽度  (默认:80%)
    autoCropHeight: 150, // 默认生成截图框高度  (默认:80%)
    fixed: true, // 是否开启截图框宽高固定比例  (默认:false)
    outputType: 'png'
});

const acceptType = 'image/png,image/jpg,image/jpeg';
const handleChange: UploadProps['onChange'] = (uploadFile) => {
    const rawFile = uploadFile.raw;
    if (!acceptType.search(rawFile.type)) {
        ElMessage.error(t('infoMessage.avatarFormat'));
    } else if (rawFile.size / 1024 / 1024 > 2) {
        ElMessage.error(t('infoMessage.avatarSize'));
    } else {
        const reader = new FileReader();
        reader.readAsDataURL(rawFile);
        reader.onload = () => {
            cropperOption.img = reader.result as any;
            active.value++;
        };
    }
};

const preview = ref({
    url: ''
});
const previewStyle = ref({});
function realTime(data) {  // 实时预览
    preview.value = data;
    previewStyle.value = {
        width: data.w + 'px',
        height: data.h + 'px',
        overflow: 'hidden',
        margin: '0',
        borderRadius: '50%',
        zoom: 150 / data.h
    };
}

const confirm = () => {
    cropperRef.value.getCropData(data => {
        let formData = new FormData();
        formData.append('file', data);
        proxy.$request.post(Constant.url.uploadAvatar, formData, {
            headers: {'Content-Type': 'multipart/form-data'}
        }).then(res => {
            emit('success');
            close();
        });
    });
};

const close = () => {
    show.value = false;
    active.value = 1;
    isQuerying.value = false;
    emit('update:modelValue', false);
};

const handleClose = (done: () => void) => {
    if(isQuerying.value) return;
    close();
    done();
};
</script>

<style lang="scss">
.upload-dialog {
    max-width: 700px;
    min-width: 480px;
}
</style>
<style scoped lang="scss">
.step2 {
    display: flex;
}
.preview-container {
    flex-grow: 1;
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}
</style>
