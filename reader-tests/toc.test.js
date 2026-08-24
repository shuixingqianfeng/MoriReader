import test from 'node:test'
import assert from 'node:assert/strict'
import { flattenToc } from '../app/src/main/assets/reader/toc.js'

test('accepts EPUBs without a parsed table of contents', () => {
  assert.deepEqual(flattenToc(null), [])
  assert.deepEqual(flattenToc(undefined), [])
})

test('flattens nested navigation while preserving depth', () => {
  assert.deepEqual(flattenToc([{
    label: 'Part',
    href: 'part.xhtml',
    subitems: [{ label: 'Chapter', href: 'chapter.xhtml' }],
  }]), [
    { label: 'Part', href: 'part.xhtml', depth: 0 },
    { label: 'Chapter', href: 'chapter.xhtml', depth: 1 },
  ])
})
