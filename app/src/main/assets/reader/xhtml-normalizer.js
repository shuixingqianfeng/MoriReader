const XHTML_NS = 'http://www.w3.org/1999/xhtml'
const ALLOWED_CONTAINERS = new Set(['body', 'div', 'section', 'article', 'main', 'blockquote', 'li', 'td'])
const BLOCK_ELEMENTS = new Set([
  'address', 'article', 'aside', 'blockquote', 'div', 'dl', 'fieldset', 'figure',
  'footer', 'form', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hr', 'main',
  'nav', 'ol', 'p', 'pre', 'section', 'table', 'ul',
])
const FORBIDDEN_ELEMENTS = 'script,iframe,object,embed,form'

const localName = node => (node.localName ?? '').toLowerCase()
const isWhitespace = node => node.nodeType === Node.TEXT_NODE && !node.data.trim()

function sanitize(doc) {
  for (const element of doc.querySelectorAll(FORBIDDEN_ELEMENTS)) element.remove()
  for (const element of doc.querySelectorAll('*')) {
    for (const attribute of [...element.attributes]) {
      const name = attribute.name.toLowerCase()
      const value = attribute.value.trim().toLowerCase()
      if (name.startsWith('on') || ((name === 'href' || name === 'src') && value.startsWith('javascript:'))) {
        element.removeAttributeNode(attribute)
      }
    }
  }
}

function hasDoubleBreak(container) {
  const nodes = [...container.childNodes]
  for (let index = 0; index < nodes.length; index += 1) {
    if (localName(nodes[index]) !== 'br') continue
    let next = index + 1
    while (next < nodes.length && isWhitespace(nodes[next])) next += 1
    if (localName(nodes[next]) === 'br') return true
  }
  return false
}

function hasBlockChild(container) {
  return [...container.children].some(child => BLOCK_ELEMENTS.has(localName(child)))
}

function normalizeContainer(doc, container) {
  const source = [...container.childNodes]
  const fragment = doc.createDocumentFragment()
  let paragraph = doc.createElementNS(doc.documentElement.namespaceURI || XHTML_NS, 'p')
  let converted = false

  const flush = () => {
    if (paragraph.childNodes.length) fragment.append(paragraph)
    paragraph = doc.createElementNS(doc.documentElement.namespaceURI || XHTML_NS, 'p')
  }

  for (let index = 0; index < source.length; index += 1) {
    const node = source[index]
    if (localName(node) !== 'br') {
      paragraph.append(node)
      continue
    }

    const between = []
    let cursor = index + 1
    while (cursor < source.length && isWhitespace(source[cursor])) {
      between.push(source[cursor])
      cursor += 1
    }
    if (localName(source[cursor]) === 'br') {
      for (const whitespace of between) paragraph.append(whitespace)
      flush()
      converted = true
      index = cursor
      while (index + 1 < source.length && localName(source[index + 1]) === 'br') index += 1
    } else {
      paragraph.append(node)
    }
  }
  flush()
  if (converted) container.replaceChildren(fragment)
  return converted
}

export function normalizeXhtml(source) {
  if (typeof source !== 'string') return source
  const doc = new DOMParser().parseFromString(source, 'application/xhtml+xml')
  if (doc.querySelector('parsererror') || !doc.documentElement) return source

  sanitize(doc)
  const beforeText = doc.body?.textContent ?? doc.documentElement.textContent
  const candidates = [...doc.querySelectorAll('body,div,section,article,main,blockquote,li,td')]
    .filter(element => ALLOWED_CONTAINERS.has(localName(element)))
    .filter(element => !hasBlockChild(element) && hasDoubleBreak(element))

  let converted = false
  for (const candidate of candidates) converted = normalizeContainer(doc, candidate) || converted
  if (!converted) return new XMLSerializer().serializeToString(doc)

  const afterText = doc.body?.textContent ?? doc.documentElement.textContent
  if (beforeText !== afterText) throw new Error('Paragraph normalization changed the chapter text')
  for (const paragraph of doc.querySelectorAll('p')) {
    if (paragraph.namespaceURI !== XHTML_NS) throw new Error('Invalid XHTML paragraph namespace')
  }
  return new XMLSerializer().serializeToString(doc)
}

export async function normalizeTransformData(value, type = '') {
  const isXhtml = /xhtml|html|xml/i.test(type)
  if (!isXhtml) return value
  if (value instanceof Blob) {
    const normalized = normalizeXhtml(await value.text())
    return new Blob([normalized], { type: value.type || type })
  }
  return normalizeXhtml(value)
}

export function webViewRenderType(type = '') {
  return /^application\/xhtml\+xml(?:\s*;|$)/i.test(type) ? 'text/html' : type
}
