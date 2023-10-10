'use strict';

import { test } from 'tape';
import fetch from 'node-fetch';

const KEYCLOAK_URL = 'http://localhost:8080';
const KEYCLOAK_REALM = 'master';
const KEYCLOAK_CLIENT_ID = 'admin-cli-end2end-testing';
const KEYCLOAK_CLIENT_SECRET = 'tIr3tYSCr0uEya2xx9vl9xnMgZTN4e0Y';
const KEYCLOAK_USER_LOGIN = 'kermit';
const KEYCLOAK_USER_PASS = 'kermit';

const delay = ms => new Promise(res => setTimeout(res, ms));

async function getAccessToken(){
  const params = new URLSearchParams();
  params.append('client_id', KEYCLOAK_CLIENT_ID);
  params.append('client_secret', KEYCLOAK_CLIENT_SECRET);
  params.append('grant_type', 'client_credentials');
  params.append('scope', 'profile');
  return (await (await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`,
    { method: 'POST', body: params }
  )).json()).access_token;
}

async function getUserAccessToken(){
  const params = new URLSearchParams();
  params.append('client_id', KEYCLOAK_CLIENT_ID);
  params.append('client_secret', KEYCLOAK_CLIENT_SECRET);
  params.append('username', KEYCLOAK_USER_LOGIN);
  params.append('password', KEYCLOAK_USER_PASS);
  params.append('grant_type', 'password');
  params.append('scope', 'profile');
  return (await (await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`,
    { method: 'POST', body: params }
  )).json()).access_token;
}

test('Should list users as JSON via custom API endpoint', { timeout: 3000 }, async (t) => {
  const users = (await (await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/auditing/users`,
    { headers: { Authorization: `Bearer ${await getAccessToken()}` } }
  )).json());
  console.log('Users: ', users)
  t.ok(users.length >= 2, 'Users found')
  t.end();

});

test('Should update user login data', { timeout: 3000 }, async (t) => {
  getUserAccessToken();
  await delay(2000);
  const users = (await (await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/auditing/users`,
    { headers: { Authorization: `Bearer ${await getAccessToken()}` } }
  )).json());
  const user = users.find(user => user.username == 'kermit');
  console.log('Last user login:', user.lastLogin )
  t.ok(user.lastLogin != null, 'Last login attribute found.')
  t.end();

});