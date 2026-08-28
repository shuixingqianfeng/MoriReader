import { makeBook } from '../foliate-js/view.js'
import { applyDocumentAppearance } from './appearance.js'
import { normalizeTransformData, webViewRenderType } from './xhtml-normalizer.js'
import { flattenToc } from './toc.js'

const reader = document.querySelector('#reader')
const chapter = document.querySelector('#chapter')
const loading = document.querySelector('#loading')
const appearanceStyle = document.querySelector('#appearance-style')
const pageTurnShadow = document.querySelector('#page-turn-shadow')

let currentBook = null
let currentSectionIndex = -1
let currentSectionDocument = null
let currentSectionUrl = null
let currentPage = 0
let appearance = {}
let tocItems = []
let chapterTitles = new Map()
let relocationFrame = 0
let suppressNextClick = false
let turningPage = false

const send = (type, payload = {}) => {
  globalThis.MoriNative?.postMessage(JSON.stringify({ type, ...payload }))
}

const nextFrame = () => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))

function removeChapterStyles() {
  for (const element of document.head.querySelectorAll('[data-mori-chapter-style]')) element.remove()
}

function installChapterStyles(sourceDocument) {
  removeChapterStyles()
  for (const source of sourceDocument.head?.querySelectorAll('style, link[rel~="stylesheet"]') ?? []) {
    const copy = document.importNode(source, true)
    copy.setAttribute('data-mori-chapter-style', '')
    document.head.insertBefore(copy, appearanceStyle)
  }
}

function applyAppearance(next = appearance) {
  const oldProgress = sectionProgress()
  appearance = { ...appearance, ...next }
  const colors = applyDocumentAppearance(reader, appearanceStyle, appearance)
  if (!colors) return
  document.documentElement.style.background = colors.background
  document.body.style.background = colors.background
  requestAnimationFrame(() => {
    if (appearance.mode === 'SCROLLED') {
      reader.scrollTop = Math.round(oldProgress * Math.max(0, reader.scrollHeight - reader.clientHeight))
    } else {
      currentPage = Math.round(oldProgress * Math.max(0, pageCount() - 1))
      snapToCurrentPage()
    }
    scheduleRelocation()
  })
}

function pageCount() {
  return Math.max(1, Math.ceil((reader.scrollWidth - 1) / Math.max(1, reader.clientWidth)))
}

function snapToCurrentPage() {
  if (appearance.mode === 'SCROLLED') return
  currentPage = Math.max(0, Math.min(pageCount() - 1, currentPage))
  const alignedLeft = currentPage * reader.clientWidth
  if (Math.abs(reader.scrollLeft - alignedLeft) > 0.5) reader.scrollLeft = alignedLeft
  send('stage', { name: `page:aligned:${currentPage}:left=${Math.round(reader.scrollLeft)}` })
}

function sectionProgress() {
  if (appearance.mode === 'SCROLLED') {
    return reader.scrollTop / Math.max(1, reader.scrollHeight - reader.clientHeight)
  }
  return currentPage / Math.max(1, pageCount() - 1)
}

function chapterTitle(index) {
  return chapterTitles.get(index) ?? ''
}

function emitRelocation() {
  if (!currentBook || currentSectionIndex < 0) return
  const sections = currentBook.sections ?? []
  const inSection = Math.min(1, Math.max(0, sectionProgress()))
  const fraction = Math.min(1, Math.max(0, (currentSectionIndex + inSection) / Math.max(1, sections.length)))
  send('relocated', {
    cfi: sections[currentSectionIndex]?.cfi ?? null,
    fraction,
    sectionIndex: currentSectionIndex,
    chapterTitle: chapterTitle(currentSectionIndex),
  })
}

function scheduleRelocation() {
  cancelAnimationFrame(relocationFrame)
  relocationFrame = requestAnimationFrame(emitRelocation)
}

function isReadableSection(index) {
  const section = currentBook?.sections?.[index]
  return Boolean(section && section.linear !== 'no')
}

function adjacentReadableIndex(start, direction) {
  const sections = currentBook?.sections ?? []
  for (let index = start + direction; index >= 0 && index < sections.length; index += direction) {
    if (isReadableSection(index)) return index
  }
  return -1
}

function targetIdentity(target) {
  if (target instanceof Element) return target.id || target.getAttribute('name') || ''
  if (target instanceof Range) {
    let node = target.startContainer
    if (node.nodeType !== Node.ELEMENT_NODE) node = node.parentElement
    return node?.closest?.('[id], [name]')?.id ?? node?.closest?.('[name]')?.getAttribute('name') ?? ''
  }
  return ''
}

function revealTarget(identity) {
  if (!identity) return false
  const escaped = CSS.escape(identity)
  const target = chapter.querySelector(`#${escaped}, [name="${escaped}"]`)
  if (!target) return false
  if (appearance.mode === 'SCROLLED') {
    reader.scrollTop = Math.max(0, target.offsetTop - 24)
  } else {
    const rect = target.getBoundingClientRect()
    const readerRect = reader.getBoundingClientRect()
    currentPage = Math.max(0, Math.min(pageCount() - 1,
      Math.floor((reader.scrollLeft + rect.left - readerRect.left) / Math.max(1, reader.clientWidth))))
    snapToCurrentPage()
  }
  return true
}

async function displaySection(index, options = {}) {
  const section = currentBook?.sections?.[index]
  if (!section) throw new Error(`章节不存在：${index}`)
  send('stage', { name: `chapter:loading:${index}` })

  const previousSection = currentBook.sections[currentSectionIndex]
  const url = await section.load()
  if (!url) throw new Error(`章节无法加载：${index}`)
  const response = await fetch(url)
  if (!response.ok) throw new Error(`章节读取失败：${response.status}`)
  const html = await response.text()
  const sourceDocument = new DOMParser().parseFromString(html, 'text/html')
  if (!sourceDocument.body) throw new Error(`章节正文无效：${index}`)

  chapter.replaceChildren()
  removeChapterStyles()
  previousSection?.unload?.()
  currentSectionDocument = null
  currentSectionUrl = url
  installChapterStyles(sourceDocument)
  const fragment = document.createDocumentFragment()
  for (const node of [...sourceDocument.body.childNodes]) fragment.append(document.importNode(node, true))
  chapter.replaceChildren(fragment)
  currentSectionIndex = index
  currentSectionDocument = sourceDocument
  currentPage = 0
  reader.scrollTo({ left: 0, top: 0, behavior: 'auto' })
  await nextFrame()

  let identity = options.identity ?? ''
  if (!identity && typeof options.anchor === 'function') {
    try { identity = targetIdentity(options.anchor(sourceDocument)) } catch (error) { console.warn(error) }
  }
  if (options.atEnd) {
    if (appearance.mode === 'SCROLLED') reader.scrollTop = Math.max(0, reader.scrollHeight - reader.clientHeight)
    else {
      currentPage = pageCount() - 1
      snapToCurrentPage()
    }
  } else if (!revealTarget(identity)) {
    currentPage = 0
    reader.scrollTo({ left: 0, top: 0, behavior: 'auto' })
    snapToCurrentPage()
  }
  send('stage', { name: `chapter:loaded:${index}:text=${chapter.innerText.length}:pages=${pageCount()}` })
  emitRelocation()
}

function buildChapterTitles(book, items) {
  const titles = new Map()
  for (const item of items) {
    if (!item.href) continue
    try {
      const resolved = book.resolveHref(item.href)
      if (resolved?.index >= 0 && !titles.has(resolved.index)) titles.set(resolved.index, item.label)
    } catch (error) {
      console.warn(error)
    }
  }
  return titles
}

async function goToHref(href) {
  if (!currentBook || !href) return
  const resolved = currentBook.resolveHref(href)
  if (!resolved || resolved.index < 0) throw new Error(`目录目标不存在：${href}`)
  await displaySection(resolved.index, { anchor: resolved.anchor })
}

async function goToCfi(cfi) {
  if (!currentBook || !cfi) return
  const resolved = currentBook.resolveCFI?.(cfi)
  if (!resolved || resolved.index < 0) throw new Error('保存的阅读位置无效')
  await displaySection(resolved.index, { anchor: resolved.anchor })
}

async function moveNext() {
  if (!currentBook) return
  if (appearance.mode === 'SCROLLED') {
    const max = Math.max(0, reader.scrollHeight - reader.clientHeight)
    if (reader.scrollTop < max - 4) {
      reader.scrollTop = Math.min(max, reader.scrollTop + Math.round(reader.clientHeight * 0.86))
      scheduleRelocation()
      return
    }
  } else if (currentPage < pageCount() - 1) {
    currentPage += 1
    snapToCurrentPage()
    scheduleRelocation()
    return
  }
  const index = adjacentReadableIndex(currentSectionIndex, 1)
  if (index >= 0) await displaySection(index)
}

async function movePrevious() {
  if (!currentBook) return
  if (appearance.mode === 'SCROLLED') {
    if (reader.scrollTop > 4) {
      reader.scrollTop = Math.max(0, reader.scrollTop - Math.round(reader.clientHeight * 0.86))
      scheduleRelocation()
      return
    }
  } else if (currentPage > 0) {
    currentPage -= 1
    snapToCurrentPage()
    scheduleRelocation()
    return
  }
  const index = adjacentReadableIndex(currentSectionIndex, -1)
  if (index >= 0) await displaySection(index, { atEnd: true })
}

const animationFinished = animation => animation.finished.catch(() => undefined)

async function turnPage(direction, operation) {
  if (turningPage || !currentBook) return
  const simulated = appearance.mode !== 'SCROLLED' && appearance.pageTurnEffect === 'SIMULATION'
  if (!simulated) {
    turningPage = true
    try {
      await operation()
      snapToCurrentPage()
    } finally {
      turningPage = false
    }
    return
  }

  turningPage = true
  const nextDirection = direction === 'next'
  reader.style.transformOrigin = nextDirection ? 'left center' : 'right center'
  pageTurnShadow.classList.toggle('previous', !nextDirection)
  try {
    const outgoing = reader.animate([
      { transform: 'perspective(1200px) rotateY(0deg) translateX(0)', filter: 'brightness(1)' },
      {
        transform: `perspective(1200px) rotateY(${nextDirection ? -9 : 9}deg) translateX(${nextDirection ? -2.5 : 2.5}%)`,
        filter: 'brightness(0.9)',
      },
    ], { duration: 145, easing: 'cubic-bezier(.35,.05,.7,.2)', fill: 'both' })
    const shadowOut = pageTurnShadow.animate(
      [{ opacity: 0 }, { opacity: 0.72 }],
      { duration: 145, easing: 'ease-in', fill: 'both' },
    )
    await Promise.all([animationFinished(outgoing), animationFinished(shadowOut)])
    await operation()
    snapToCurrentPage()
    outgoing.cancel()
    shadowOut.cancel()

    const incoming = reader.animate([
      {
        transform: `perspective(1200px) rotateY(${nextDirection ? 7 : -7}deg) translateX(${nextDirection ? 2 : -2}%)`,
        filter: 'brightness(0.92)',
      },
      { transform: 'perspective(1200px) rotateY(0deg) translateX(0)', filter: 'brightness(1)' },
    ], { duration: 185, easing: 'cubic-bezier(.2,.75,.25,1)', fill: 'both' })
    const shadowIn = pageTurnShadow.animate(
      [{ opacity: 0.55 }, { opacity: 0 }],
      { duration: 185, easing: 'ease-out', fill: 'both' },
    )
    await Promise.all([animationFinished(incoming), animationFinished(shadowIn)])
    incoming.cancel()
    shadowIn.cancel()
  } finally {
    reader.style.transformOrigin = ''
    pageTurnShadow.classList.remove('previous')
    pageTurnShadow.style.opacity = '0'
    snapToCurrentPage()
    scheduleRelocation()
    turningPage = false
  }
}

const next = () => turnPage('next', moveNext)
const previous = () => turnPage('previous', movePrevious)

async function openBook(command) {
  loading.classList.remove('hidden')
  try {
    closeBook()
    send('stage', { name: 'open:start' })
    const book = await makeBook(command.url)
    currentBook = book
    send('stage', { name: `book:made:${book.sections?.length ?? 0}` })
    book.transformTarget?.addEventListener('data', event => {
      const detail = event.detail
      const sourceType = detail.type
      detail.data = Promise.resolve(detail.data).then(value => normalizeTransformData(value, sourceType))
      detail.type = webViewRenderType(sourceType)
    })

    appearance = command.appearance ?? {}
    applyAppearance()
    tocItems = flattenToc(book.toc)
    chapterTitles = buildChapterTitles(book, tocItems)
    send('toc', { items: tocItems })

    if (command.lastCfi) await goToCfi(command.lastCfi)
    else {
      const firstReadableHref = tocItems.find(item => item.href)?.href
      if (firstReadableHref) await goToHref(firstReadableHref)
      else {
        const firstIndex = (book.sections ?? []).findIndex(section => section.linear !== 'no')
        await displaySection(Math.max(0, firstIndex))
      }
    }
    loading.classList.add('hidden')
    send('opened')
  } catch (error) {
    loading.classList.add('hidden')
    send('error', { message: error?.message ?? String(error) })
  }
}

function closeBook() {
  cancelAnimationFrame(relocationFrame)
  currentBook?.sections?.[currentSectionIndex]?.unload?.()
  currentBook?.destroy?.()
  currentBook = null
  currentSectionIndex = -1
  currentSectionDocument = null
  currentSectionUrl = null
  currentPage = 0
  tocItems = []
  chapterTitles = new Map()
  chapter.replaceChildren()
  removeChapterStyles()
}

async function handleCommand(message) {
  const command = typeof message === 'string' ? JSON.parse(message) : message
  switch (command.type) {
    case 'open': await openBook(command); break
    case 'close': closeBook(); break
    case 'next': await next(); break
    case 'previous': await previous(); break
    case 'goToCfi': await goToCfi(command.cfi); break
    case 'goToHref': await goToHref(command.href); break
    case 'goToSection': if (Number.isInteger(command.index)) await displaySection(command.index); break
    case 'setMode': applyAppearance({ mode: command.mode }); break
    case 'setAppearance': applyAppearance(command.appearance ?? {}); break
  }
}

reader.addEventListener('scroll', () => {
  if (appearance.mode !== 'SCROLLED') {
    currentPage = Math.max(0, Math.min(pageCount() - 1,
      Math.round(reader.scrollLeft / Math.max(1, reader.clientWidth))))
  }
  scheduleRelocation()
}, { passive: true })

reader.addEventListener('click', event => {
  if (suppressNextClick) {
    suppressNextClick = false
    return
  }
  const link = event.target.closest?.('a[href]')
  if (link) {
    event.preventDefault()
    const href = link.getAttribute('href')
    if (!href) return
    if (href.startsWith('#')) {
      revealTarget(decodeURIComponent(href.slice(1)))
      scheduleRelocation()
      return
    }
    try {
      const internalHref = currentBook?.sections?.[currentSectionIndex]?.resolveHref?.(href) ?? href
      goToHref(internalHref).catch(error => send('error', { message: error.message }))
    } catch (error) {
      console.warn(error)
    }
    return
  }
  const ratio = event.clientX / Math.max(1, reader.clientWidth)
  if (ratio < 0.28) previous().catch(error => send('error', { message: error.message }))
  else if (ratio > 0.72) next().catch(error => send('error', { message: error.message }))
  else send('centerTap')
})

let touchStart = null
reader.addEventListener('touchstart', event => {
  const touch = event.changedTouches[0]
  touchStart = touch ? { x: touch.clientX, y: touch.clientY } : null
}, { passive: true })
reader.addEventListener('touchmove', event => {
  if (appearance.mode !== 'SCROLLED' && appearance.swipeEnabled !== false && touchStart) {
    event.preventDefault()
  }
}, { passive: false })
reader.addEventListener('touchend', event => {
  const touch = event.changedTouches[0]
  if (!touchStart || !touch) return
  const deltaX = touch.clientX - touchStart.x
  const deltaY = touch.clientY - touchStart.y
  touchStart = null
  if (appearance.swipeEnabled === false) return
  if (Math.abs(deltaX) < 48 || Math.abs(deltaX) < Math.abs(deltaY) * 1.2) return
  suppressNextClick = true
  const action = deltaX < 0 ? next() : previous()
  action.catch(error => send('error', { message: error.message }))
})
reader.addEventListener('touchcancel', () => {
  touchStart = null
  snapToCurrentPage()
})
window.addEventListener('resize', () => requestAnimationFrame(snapToCurrentPage))

if (globalThis.MoriNative) {
  globalThis.MoriNative.onmessage = event => handleCommand(event.data)
    .catch(error => send('error', { message: error?.message ?? String(error) }))
}
send('ready')
