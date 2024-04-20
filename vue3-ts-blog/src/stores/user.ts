import { defineStore } from 'pinia';
import {LOGIN_STATE} from '@/utils/localStoreItem';

 interface State {
    id: number|string,
    username: string,
    avatarAssetId: number|string,
    avatarUrl: string
}

export const useUserStore = defineStore('user', {
    state: (): State => { 
        return {
            id: 0,
            username: '',
            avatarAssetId: 0,
            avatarUrl: ''
        };
    },
    getters: {
        isLoggedIn: (state) => !!state.id
    },
    actions: {
        setUserData(data: Partial<State>) {
            Object.assign(this, data);
        },
        clear() {
            this.$reset();
            localStorage.removeItem(LOGIN_STATE);
        }
    }
});

