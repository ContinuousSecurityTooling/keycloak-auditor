/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2023-10-14 17:09:23.

export interface ClientLoginDetails {
    kcLogin: Date;
}

export interface UserLoginDetails {
    kcLogin: Date;
    clientLogins: { [index: string]: Date };
}

export const enum ConfigConstants {
    DISABLE_EXTERNAL_ACCESS = "KC_AUD_DISABLE_EXTERNAL_ACCESS",
    DISABLE_ROLE_CHECK = "KC_AUD_DISABLE_ROLE_CHECK",
    GLOBAL_MASTER_ACCESS = "KC_AUD_GLOBAL_MASTER_ACCESS",
    DEFAULT_ROLE = "KC_AUD_DEFAULT_ROLE",
}

export const enum Constants {
    USER_EVENT_PREFIX = "aud_usr",
    CLIENT_EVENT_PREFIX = "aud_cls",
    ADMIN_EVENT_PREFIX = "aud_adm",
    LAST_LOGIN_INFIX = "last-login",
}
