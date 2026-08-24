import { makeBook } from '../foliate-js/view.js'
import { applyRendererAppearance } from './appearance.js'
import { normalizeTransformData } from './xhtml-normalizer.js'
import { flattenToc } from './toc.js'

const view = document.querySelector('#reader')
const loading = document.querySelector('#loading')
let currentBook = null
let appearance = {}

const send = (type, payload = {}) => {
  globalThis.MoriNative?.postMessage(JSON.stringify({ type, ...payload }))
}

function applyAppearance(next = appearance) {
  appearance = { ...appearance, ...next }
  const colors = applyRendererAppearance(view.renderer, appearance)
  if (!colors) return
  const { background } = colors
  document.documentElement.style.background = background
  document.body.style.background = background
}

function installTapZones(doc) {
  doc.addEventListener('click', event => {
    if (event.target.closest?.('a')) return
    const ratio = event.clientX / Math.max(1, doc.documentElement.clientWidth)
    if (ratio < 0.28) view.prev()
    else if (ratio > 0.72) view.next()
    else send('centerTap')
  })
}

view.addEventListener('load', event => installTapZones(event.detail.doc))
view.addEventListener('relocate', event => {
  const detail = event.detail ?? {}
  send('relocated', {
    cfi: detail.cfi ?? null,
    fraction: Number(detail.fraction ?? 0),
    sectionIndex: Number(detail.section?.current ?? 0),
    chapterTitle: detail.tocItem?.label ?? '',
  })
})

async function openBook(command) {
  loading.classList.remove('hidden')
  try {
    view.close?.()
    const book = await makeBook(command.url)
    currentBook = book
    book.transformTarget?.addEventListener('data', event => {
      const detail = event.detail
      detail.data = Promise.resolve(detail.data).then(value => normalizeTransformData(value, detail.type))
    })
    await view.open(book)
    appearance = command.appearance ?? {}
    applyAppearance()
    await view.init({ lastLocation: command.lastCfi || null, showTextStart: !command.lastCfi })
    send('toc', { items: flattenToc(book.toc) })
    loading.classList.add('hidden')
    send('opened')
  } catch (error) {
    loading.classList.add('hidden')
    send('error', { message: error?.message ?? String(error) })
  }
}

async function handleCommand(message) {
  const command = typeof message === 'string' ? JSON.parse(message) : message
  switch (command.type) {
    case 'open': await openBook(command); break
    case 'close': view.close?.(); currentBook = null; break
    case 'next': await view.next(); break
    case 'previous': await view.prev(); break
    case 'goToCfi': if (command.cfi) await view.goTo(command.cfi); break
    case 'goToHref': if (command.href) await view.goTo(command.href); break
    case 'goToSection': if (Number.isInteger(command.index)) await view.goTo(command.index); break
    case 'setMode': applyAppearance({ mode: command.mode }); break
    case 'setAppearance': applyAppearance(command.appearance ?? {}); break
  }
}

if (globalThis.MoriNative) {
  globalThis.MoriNative.onmessage = event => handleCommand(event.data).catch(error => send('error', { message: error.message }))
}
send('ready')
