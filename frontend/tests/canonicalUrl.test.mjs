import assert from 'node:assert/strict'
import test from 'node:test'

import { getCanonicalUrl } from '../src/canonicalUrl.ts'

test('redirects a deployment hostname to the configured production origin', () => {
  assert.equal(
    getCanonicalUrl(
      'https://gpg-union-portal.vercel.app',
      'https://gpg-union-portal-9drjxgicl-gpg7.vercel.app/reports?month=2026-08#summary',
    ),
    'https://gpg-union-portal.vercel.app/reports?month=2026-08#summary',
  )
})

test('does not redirect when the current origin is already canonical', () => {
  assert.equal(
    getCanonicalUrl(
      'https://gpg-union-portal.vercel.app',
      'https://gpg-union-portal.vercel.app/reports',
    ),
    null,
  )
})

test('does not redirect local or preview builds without a configured origin', () => {
  assert.equal(getCanonicalUrl(undefined, 'http://localhost:3637/'), null)
})

test('ignores an invalid canonical origin instead of breaking application startup', () => {
  assert.equal(getCanonicalUrl('not-a-url', 'https://example.com/'), null)
})
