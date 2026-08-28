import { createReadStream } from 'node:fs'
import { access, mkdir } from 'node:fs/promises'
import { createServer } from 'node:http'
import path from 'node:path'
import process from 'node:process'
import { pathToFileURL } from 'node:url'

const [epubArgument, screenshotArgument] = process.argv.slice(2)
if (!epubArgument) throw new Error('Usage: node real-epub-smoke.mjs <book.epub> [screenshot.png]')

const epubPath = path.resolve(epubArgument)
const screenshotPath = path.resolve(screenshotArgument ?? 'build/real-epub-smoke.png')
const assetsRoot = path.resolve(process.env.MORI_ASSETS_ROOT ?? 'app/src/main/assets')
await Promise.all([access(epubPath), access(path.join(assetsRoot, 'reader/index.html'))])
await mkdir(path.dirname(screenshotPath), { recursive: true })

const mimeTypes = new Map([
  ['.css', 'text/css'],
  ['.epub', 'application/epub+zip'],
  ['.html', 'text/html'],
  ['.js', 'text/javascript'],
  ['.json', 'application/json'],
  ['.svg', 'image/svg+xml'],
  ['.wasm', 'application/wasm'],
])

const server = createServer((request, response) => {
  const url = new URL(request.url, 'http://127.0.0.1')
  let filePath
  if (url.pathname === '/books/book.epub') filePath = epubPath
  else if (url.pathname.startsWith('/assets/')) {
    const relative = decodeURIComponent(url.pathname.slice('/assets/'.length))
    const candidate = path.resolve(assetsRoot, relative)
    if (candidate === assetsRoot || !candidate.startsWith(`${assetsRoot}${path.sep}`)) {
      response.writeHead(403).end()
      return
    }
    filePath = candidate
  }
  if (!filePath) {
    response.writeHead(404).end()
    return
  }
  response.writeHead(200, {
    'Cache-Control': 'no-store',
    'Content-Type': mimeTypes.get(path.extname(filePath).toLowerCase()) ?? 'application/octet-stream',
  })
  createReadStream(filePath).on('error', () => response.destroy()).pipe(response)
})

await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
const { port } = server.address()
const origin = `http://127.0.0.1:${port}`

const playwrightPath = process.env.PLAYWRIGHT_MODULE
if (!playwrightPath) throw new Error('PLAYWRIGHT_MODULE must point to Playwright index.mjs')
const { chromium } = await import(pathToFileURL(playwrightPath).href)
const browser = await chromium.launch({
  executablePath: process.env.BROWSER_EXECUTABLE,
  headless: true,
})

try {
  const page = await browser.newPage({ viewport: { width: 432, height: 900 } })
  const diagnostics = []
  page.on('console', message => diagnostics.push(`console.${message.type()}: ${message.text()}`))
  page.on('pageerror', error => diagnostics.push(`pageerror: ${error.stack ?? error.message}`))
  page.on('requestfailed', request => diagnostics.push(`requestfailed: ${request.url()} ${request.failure()?.errorText}`))
  await page.addInitScript(() => {
    globalThis.__moriMessages = []
    globalThis.MoriNative = {
      onmessage: null,
      postMessage(message) { globalThis.__moriMessages.push(JSON.parse(message)) },
    }
  })
  await page.goto(`${origin}/assets/reader/index.html`, { waitUntil: 'load' })
  await page.waitForFunction(() => globalThis.__moriMessages.some(message => message.type === 'ready'))
  await page.evaluate(url => globalThis.MoriNative.onmessage({
    data: JSON.stringify({
      type: 'open',
      url,
      lastCfi: null,
      appearance: {
        fontSizeSp: 19,
        lineHeight: 1.75,
        paragraphSpacingEm: 0.8,
        horizontalMarginDp: 24,
        theme: 'WHITE',
        mode: 'PAGINATED',
      },
    }),
  }), `${origin}/books/book.epub`)

  let timedOut = false
  try {
    await page.waitForFunction(() => globalThis.__moriMessages.some(message =>
      message.type === 'opened' || message.type === 'error'), null, { timeout: 30_000 })
  } catch {
    timedOut = true
  }
  if (!timedOut) await page.waitForTimeout(250)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  const state = await page.evaluate(() => {
    const view = document.querySelector('#reader')
    const chapter = document.querySelector('#chapter')
    const paragraph = chapter?.querySelector('p')
    const paragraphStyle = paragraph ? getComputedStyle(paragraph) : null
    const paragraphRect = paragraph?.getBoundingClientRect()
    return {
      messages: globalThis.__moriMessages,
      loadingHidden: document.querySelector('#loading')?.classList.contains('hidden'),
      hasDirectRenderer: Boolean(view && chapter),
      chapterTextLength: chapter?.innerText?.length ?? 0,
      paragraphStyle: {
        color: paragraphStyle?.color ?? null,
        display: paragraphStyle?.display ?? null,
        opacity: paragraphStyle?.opacity ?? null,
        visibility: paragraphStyle?.visibility ?? null,
        rect: paragraphRect ? {
          x: paragraphRect.x,
          y: paragraphRect.y,
          width: paragraphRect.width,
          height: paragraphRect.height,
        } : null,
      },
      imageCount: chapter?.querySelectorAll('img').length ?? 0,
      loadedImageCount: [...(chapter?.querySelectorAll('img') ?? [])]
        .filter(image => image.complete && image.naturalWidth > 0).length,
      scrollWidth: view?.scrollWidth ?? 0,
      clientWidth: view?.clientWidth ?? 0,
    }
  })
  process.stdout.write(`${JSON.stringify({ timedOut, state, diagnostics, screenshotPath }, null, 2)}\n`)
  const hasVisibleContent = state.chapterTextLength > 0 || state.loadedImageCount > 0
  const latestRelocation = state.messages.filter(message => message.type === 'relocated').at(-1)
  const expectedSectionIndex = process.env.EXPECTED_SECTION_INDEX
  const expectedChapterTitle = process.env.EXPECTED_CHAPTER_TITLE
  const expectedMinTextLength = Number(process.env.EXPECTED_MIN_TEXT_LENGTH ?? 1)
  const matchesExpectedSection = expectedSectionIndex == null || latestRelocation?.sectionIndex === Number(expectedSectionIndex)
  const matchesExpectedTitle = expectedChapterTitle == null || latestRelocation?.chapterTitle === expectedChapterTitle
  const matchesExpectedTextLength = state.chapterTextLength >= expectedMinTextLength
  if (
    timedOut ||
    state.messages.some(message => message.type === 'error') ||
    !hasVisibleContent ||
    !matchesExpectedSection ||
    !matchesExpectedTitle ||
    !matchesExpectedTextLength
  ) {
    process.exitCode = 1
  }
} finally {
  await browser.close()
  await new Promise(resolve => server.close(resolve))
}
