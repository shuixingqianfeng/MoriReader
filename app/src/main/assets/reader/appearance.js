export function themeColors(theme) {
  if (theme === 'SEPIA') return ['#f6f0e3', '#332d25']
  if (theme === 'GRAY') return ['#e9edf0', '#263039']
  if (theme === 'DARK') return ['#16191d', '#d9dde2']
  return ['#ffffff', '#20252a']
}

export function buildAppearanceCss(appearance = {}) {
  const [background, foreground] = themeColors(appearance.theme)
  const fontSize = Number(appearance.fontSizeSp ?? 19)
  const lineHeight = Number(appearance.lineHeight ?? 1.75)
  const paragraphSpacing = Number(appearance.paragraphSpacingEm ?? 0.8)
  const margin = Number(appearance.horizontalMarginDp ?? 24)
  return `
    :root { color-scheme: ${appearance.theme === 'DARK' ? 'dark' : 'light'}; }
    html, body { background: ${background} !important; color: ${foreground} !important; }
    body { font-size: ${fontSize}px !important; line-height: ${lineHeight} !important; padding-inline: ${margin}px !important; }
    p { margin-block: 0 ${paragraphSpacing}em !important; }
    img, svg { max-width: 100% !important; height: auto !important; margin-inline: auto !important; }
  `
}

export function applyRendererAppearance(renderer, appearance = {}) {
  if (!renderer) return null
  renderer.setAttribute('flow', appearance.mode === 'SCROLLED' ? 'scrolled' : 'paginated')
  renderer.setStyles?.(buildAppearanceCss(appearance))
  const [background, foreground] = themeColors(appearance.theme)
  return { background, foreground }
}
