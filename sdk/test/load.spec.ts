/// <reference types="jest-extended" />
import { Constants, ClientLoginDetails, UserLoginDetails } from '../';
test('accept Constants', async () => {
  expect(<Constants>{}).toBeDefined();
});

test('accept ClientLoginDetails', async () => {
  expect(<ClientLoginDetails>{}).toBeDefined();
});

test('accept UserLoginDetails', async () => {
    expect(<UserLoginDetails>{}).toBeDefined();
  });
  