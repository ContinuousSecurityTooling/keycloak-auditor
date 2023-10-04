'use strict';

import { test } from 'tape';
import fetch from 'node-fetch';

const KEYCLOAK_URL = 'http://localhost:8080';
const KEYCLOAK_REALM = 'master';
const KEYCLOAK_CLIENT_ID = 'admin-cli-end2end-testing';
const KEYCLOAK_CLIENT_SECRET = 'tIr3tYSCr0uEya2xx9vl9xnMgZTN4e0Y';

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

test('Should list users as JSON via custom API endpoint', { timeout: 3000 }, async (t) => {
  const users = (await (await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/auditing/users`,
    { headers: { Authorization: `Bearer ${await getAccessToken()}` } }
  )).json());
  console.log(users)
  t.ok(users.length >= 2, '')
  t.end();

});