<template>
<div class="main-container a-p-lg">
    <div class="flex-between a-font-title-1 a-bb-base a-pb-lg">
        <span>{{isViewFollowing?$t('label.myFollowing'):$t('label.myFans')}}</span>
        <el-input v-model="keyword" class="a-w-200" :placeholder="$t('label.search')" @change="getData"></el-input>
    </div>
    <div v-if="total===0" class="a-ta-c a-pt-lg">{{$t('label.emptyData')}}</div>
    <div v-for="item in list" :key="item[Constant.userId]" class="user-item a-bb-base a-p-sm">
        <img class="avatar" :src="handleAvatar(item[Constant.avatarUrl])" alt="">
        <div class="a-font-label-1 a-ml-xs">
            <span class="a-c-h-primary a-c-p" @click="viewUser(router, item)">{{item[Constant.username]}}</span>
        </div>
        <el-button :type="item.isFollowing?'info':'primary'" @click="switchFollow(item)" plain>
            {{item[Constant.mutual] ?
                $t('label.followingEachOther'):
                (item.isFollowing ? $t('label.following') : $t('label.follow'))}}
        </el-button>
    </div>
    <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        layout="total, prev, pager, next, jumper"
        :total="totalCount"
        hide-on-single-page
        @current-change="handleCurrentChange"
    />
</div>
</template>

<script lang="ts" setup name="Follow">
import {useRoute, useRouter} from 'vue-router';
import {getCurrentInstance, onMounted, ref} from 'vue';
import Constant, {viewUser, handleAvatar} from '@/model/user/constant';
import {ElMessageBox} from 'element-plus';
import i18n from '@/language/i18n';

const route = useRoute();
const router = useRouter();
const {proxy} = getCurrentInstance();
const { t } = i18n.global as any;

const isViewFollowing = ref(false);
let url = '';
const list = ref([]);
const total = ref(0);
const pageSize = 20;
const totalCount = ref(0);
const currentPage = ref(1);
const keyword = ref('');

const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    getData();
};

const getData = () => {
    proxy.$request.post(url, {
        pageSize,
        currentPage: currentPage.value,
        filter: {
            keyword: keyword.value
        }
    }).then(res => {
        list.value = res.result.list.map(item => {
            item.isFollowing = isViewFollowing.value || item[Constant.mutual];
            return item;
        });
        total.value = res.result.total;
    });
};

const isQuerying = ref(false);
const switchFollow = (item) => {
    console.log(item);
    if(item.isFollowing) {
        ElMessageBox.confirm(
            t('infoMessage.confirmUnfollow', {name: item[Constant.username]}),
            t('label.tip')
        ).then(() => {
            isQuerying.value = true;
            proxy.$request.post(Constant.url.switchFollow, {
                [Constant.userId]: item[Constant.userId],
                [Constant.value]: false
            }).then(() => {
                isQuerying.value = false;
                item.isFollowing = false;
            }).catch(err => isQuerying.value = false);
        }).catch(() => {});
    } else {
        proxy.$request.post(Constant.url.switchFollow, {
            [Constant.userId]: item[Constant.userId],
            [Constant.value]: true
        }).then(() => {
            isQuerying.value = false;
            item.isFollowing = true;
        }).catch(err => isQuerying.value = false);
    }
};

onMounted(() => {
    isViewFollowing.value = route.name === 'following';
    url = isViewFollowing.value ? Constant.url.followings : Constant.url.followers;
    getData();
});
</script>

<style scoped lang="scss">
.main-container {
    max-width: 900px;
    min-width: 360px;
    margin: 10px auto;
    background: $--bg-color;
}
.avatar {
    width: 60px;
    height: 60px;
    border-radius: 50%;
}
.user-item {
    display: flex;
    align-items: center;
    .a-font-label-1 {
        flex-grow: 1;
    }
}
</style>
