export function themeColors(theme) {
  if (theme === 'SEPIA') return ['#f6f0e3', '#332d25']
  if (theme === 'GRAY') return ['#e9edf0', '#263039']
  if (theme === 'DARK') return ['#16191d', '#d9dde2']
  return ['#ffffff', '#20252a']
}

const finiteNumber = (value, fallback, minimum, maximum) => {
  const number = Number(value)
  return Number.isFinite(number) ? Math.min(maximum, Math.max(minimum, number)) : fallback
}

export function buildAppearanceCss(appearance = {}) {
  const [background, foreground] = themeColors(appearance.theme)
  const fontSize = finiteNumber(appearance.fontSizeSp, 19, 12, 48)
  const lineHeight = finiteNumber(appearance.lineHeight, 1.75, 1.1, 3)
  const paragraphSpacing = finiteNumber(appearance.paragraphSpacingEm, 0.8, 0, 3)
  const margin = finiteNumber(appearance.horizontalMarginDp, 24, 8, 80)
  return `
    :root {
      color-scheme: ${appearance.theme === 'DARK' ? 'dark' : 'light'};
      --reader-background: ${background};
      --reader-foreground: ${foreground};
      --reader-font-size: ${fontSize}px;
      --reader-line-height: ${lineHeight};
      --reader-paragraph-spacing: ${paragraphSpacing}em;
      --reader-margin: ${margin}px;
    }
    html, body, #reader { background: ${background} !important; color: ${foreground} !important; }
    #chapter { color: ${foreground} !important; font-size: ${fontSize}px !important; line-height: ${lineHeight} !important; }
    #chapter, #chapter * { color: ${foreground} !important; }
    #chapter p { margin-block: 0 ${paragraphSpacing}em !important; }
  `
}

export function applyDocumentAppearance(reader, styleElement, appearance = {}) {
  if (!reader || !styleElement) return null
  reader.classList.toggle('scrolled', appearance.mode === 'SCROLLED')
  reader.classList.toggle('paginated', appearance.mode !== 'SCROLLED')
  styleElement.textContent = buildAppearanceCss(appearance)
  const [background, foreground] = themeColors(appearance.theme)
  return { background, foreground }
}

// Kept for compatibility with foliate-based callers while MoriReader uses the
// direct renderer above. This makes appearance changes safe for fixed-layout
// renderers that do not expose setStyles.
export function applyRendererAppearance(renderer, appearance = {}) {
  if (!renderer) return null
  renderer.setAttribute?.('flow', appearance.mode === 'SCROLLED' ? 'scrolled' : 'paginated')
  renderer.setStyles?.(buildAppearanceCss(appearance))
  const [background, foreground] = themeColors(appearance.theme)
  return { background, foreground }
}
