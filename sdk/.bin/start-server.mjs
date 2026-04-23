#!/usr/bin/env node
// @ts-check
import { Octokit } from '@octokit/rest'
import gunzip from 'gunzip-maybe'
import fetch from 'node-fetch'
import { spawn } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { pipeline } from 'node:stream'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import tar from 'tar-fs'

const DIR_NAME = path.dirname(fileURLToPath(import.meta.url))
const SERVER_DIR = path.resolve(DIR_NAME, '../tmp/server')
const SCRIPT_EXTENSION = process.platform === 'win32' ? '.bat' : '.sh'

// TODO: Once support for Node.js 14 has been dropped this can be replaced with an import from 'node:stream/promises'.
// More information: https://nodejs.org/api/stream.html#streams-promises-api
const pipelineAsync = promisify(pipeline)

await startServer()

async function startServer () {
  await downloadServer()

  console.info('Copying kc auditor extension to server …')
  fs.createReadStream(path.resolve(DIR_NAME, '../../spi/target/keycloak-auditor-keycloak-auditor-spi.jar'))
  .pipe(fs.createWriteStream(path.resolve(DIR_NAME, '../tmp/server/providers/keycloak-auditor-spi.jar')));

  console.info('Starting server …')

  const args = process.argv.slice(2)
  const child = spawn(
    path.join(SERVER_DIR, `bin/kc${SCRIPT_EXTENSION}`),
    ['start-dev', ...args],
    {
      env: {
        KC_BOOTSTRAP_ADMIN_USERNAME: 'master-admin',
        KC_BOOTSTRAP_ADMIN_PASSWORD: 'admin',
        ...process.env
      }
    }
  )

  child.stdout.pipe(process.stdout)
  child.stderr.pipe(process.stderr)
}

async function downloadServer () {
  const tag = process.env.KC_VERSION || 'nightly'
  const sentinelPath = path.join(SERVER_DIR, '.kcversion')
  const installedVersion = fs.existsSync(sentinelPath) ? fs.readFileSync(sentinelPath, 'utf8').trim() : null
  const directoryExists = fs.existsSync(path.join(SERVER_DIR, `bin/kc${SCRIPT_EXTENSION}`))

  if (directoryExists && installedVersion === tag) {
    console.info(`Server installation found for ${tag}, skipping download.`)
    return
  }

  if (directoryExists && installedVersion !== tag) {
    console.info(`Installed version (${installedVersion}) differs from requested (${tag}), re-downloading…`)
    fs.rmSync(SERVER_DIR, { recursive: true, force: true })
  }

  console.info('Downloading and extracting server…')

  const nightlyAsset = await getNightlyAsset()
  //console.log(nightlyAsset)
  const assetStream = await getAssetAsStream(nightlyAsset)

  await extractTarball(assetStream, SERVER_DIR, { strip: 1 })
  fs.writeFileSync(sentinelPath, tag)
}

async function getNightlyAsset () {
  const api = new Octokit({
    auth: process.env.GITHUB_TOKEN || undefined
  })
  const tag = process.env.KC_VERSION || 'nightly'
  const release = await api.repos.getReleaseByTag({
    owner: 'keycloak',
    repo: 'keycloak',
    tag: tag
  })
  let assertName = `keycloak-${tag}.tar.gz`
  if (tag == 'nightly') {
    assertName = 'keycloak-999.0.0-SNAPSHOT.tar.gz'
  }

  return release.data.assets.find(
    ({ name }) => name === assertName
  )
}

async function getAssetAsStream (asset) {
  const response = await fetch(asset.browser_download_url)

  if (!response.ok) {
    throw new Error('Something went wrong requesting the nightly release.')
  }

  return response.body
}

function extractTarball (stream, path, options) {
  return pipelineAsync(stream, gunzip(), tar.extract(path, options))
}