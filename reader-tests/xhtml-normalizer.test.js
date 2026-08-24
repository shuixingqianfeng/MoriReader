import test from 'node:test'
import assert from 'node:assert/strict'
import { DOMParser, Node } from 'linkedom'

globalThis.DOMParser = DOMParser
globalThis.Node = Node
globalThis.XMLSerializer = class {
  serializeToString(node) { return node.toString() }
}

const { normalizeXhtml } = await import('../app/src/main/assets/reader/xhtml-normalizer.js')
const ns = 'http://www.w3.org/1999/xhtml'
const wrap = body => `<?xml version="1.0"?><html xmlns="${ns}"><head><title>fixture</title></head><body>${body}</body></html>`

test('keeps standard paragraphs and text', () => {
  const input = wrap('<p>第一段。</p><p><ruby>森<rt>もり</rt></ruby>第二段。</p>')
  const output = normalizeXhtml(input)
  const doc = new DOMParser().parseFromString(output, 'application/xhtml+xml')
  assert.equal(doc.querySelectorAll('p').length, 2)
  assert.equal(doc.querySelector('body').textContent, '第一段。森もり第二段。')
})

test('converts only double BR boundaries to XHTML paragraphs', () => {
  const input = wrap('第一段。<br/><br/>第二段，保留<br/>单换行。<br/>\n<br/>第三段。')
  const before = new DOMParser().parseFromString(input, 'application/xhtml+xml').querySelector('body').textContent
  const output = normalizeXhtml(input)
  const doc = new DOMParser().parseFromString(output, 'application/xhtml+xml')
  const paragraphs = [...doc.querySelectorAll('p')]
  assert.equal(paragraphs.length, 3)
  assert.ok(paragraphs.every(paragraph => paragraph.namespaceURI === ns))
  assert.equal(doc.querySelector('body').textContent, before)
  assert.equal(paragraphs[1].querySelectorAll('br').length, 1)
})

test('does not invent paragraphs from punctuation or single BR', () => {
  const input = wrap('一句。二句？三句！<br/>仍是同一段。')
  const output = normalizeXhtml(input)
  const doc = new DOMParser().parseFromString(output, 'application/xhtml+xml')
  assert.equal(doc.querySelectorAll('p').length, 0)
  assert.equal(doc.querySelectorAll('br').length, 1)
})

test('removes active content while preserving chapter prose', () => {
  const input = wrap('<div onclick="steal()">正文<script>alert(1)</script><a href="javascript:steal()">链接</a></div>')
  const output = normalizeXhtml(input)
  const doc = new DOMParser().parseFromString(output, 'application/xhtml+xml')
  assert.equal(doc.querySelectorAll('script').length, 0)
  assert.equal(doc.querySelector('div').hasAttribute('onclick'), false)
  assert.equal(doc.querySelector('a').hasAttribute('href'), false)
  assert.equal(doc.querySelector('body').textContent, '正文链接')
})
