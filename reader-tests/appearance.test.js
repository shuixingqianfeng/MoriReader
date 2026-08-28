import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyDocumentAppearance,
  applyRendererAppearance,
  buildAppearanceCss,
} from '../app/src/main/assets/reader/appearance.js'

test('applies mode and CSS to the direct Android renderer', () => {
  const classes = new Set()
  const reader = {
    classList: {
      toggle(name, enabled) {
        if (enabled) classes.add(name)
        else classes.delete(name)
      },
    },
  }
  const style = { textContent: '' }

  const colors = applyDocumentAppearance(reader, style, {
    mode: 'SCROLLED',
    theme: 'SEPIA',
    horizontalMarginDp: 28,
  })

  assert.equal(classes.has('scrolled'), true)
  assert.equal(classes.has('paginated'), false)
  assert.match(style.textContent, /--reader-margin: 28px/)
  assert.deepEqual(colors, { background: '#f6f0e3', foreground: '#332d25' })
})

test('applies flow and CSS through the foliate renderer API', () => {
  const calls = []
  const renderer = {
    setAttribute: (...args) => calls.push(['setAttribute', ...args]),
    setStyles: css => calls.push(['setStyles', css]),
  }

  const colors = applyRendererAppearance(renderer, {
    mode: 'SCROLLED',
    theme: 'SEPIA',
    fontSizeSp: 21,
    lineHeight: 1.8,
    paragraphSpacingEm: 1,
    horizontalMarginDp: 28,
  })

  assert.deepEqual(calls[0], ['setAttribute', 'flow', 'scrolled'])
  assert.equal(calls[1][0], 'setStyles')
  assert.match(calls[1][1], /font-size: 21px/)
  assert.match(calls[1][1], /line-height: 1.8/)
  assert.match(calls[1][1], /--reader-margin: 28px/)
  assert.deepEqual(colors, { background: '#f6f0e3', foreground: '#332d25' })
})

test('keeps fixed-layout renderers without setStyles usable', () => {
  const attributes = []
  const renderer = { setAttribute: (...args) => attributes.push(args) }

  assert.doesNotThrow(() => applyRendererAppearance(renderer, { mode: 'PAGINATED' }))
  assert.deepEqual(attributes, [['flow', 'paginated']])
})

test('builds dark theme CSS', () => {
  const css = buildAppearanceCss({ theme: 'DARK' })
  assert.match(css, /color-scheme: dark/)
  assert.match(css, /background: #16191d !important/)
  assert.match(css, /color: #d9dde2 !important/)
})
