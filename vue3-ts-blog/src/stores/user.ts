import { defineStore } from 'pinia';
import {LOGIN_STATE} from '@/utils/localStoreItem';

 interface State {
    id: number|string,
    username: string,
    avatarUrl: string
}

export const useUserStore = defineStore('user', {
    state: (): State => { 
        return {
            id: 0,
            username: '',
            avatarUrl: ''
        };
    },
    getters: {
        isLoggedIn: (state) => !!state.id
    },
    actions: {
        setUserData(data) {
            Object.assign(this, data);
        },
        clear() {
            this.$reset();
            localStorage.removeItem(LOGIN_STATE);
        }
    }
});

