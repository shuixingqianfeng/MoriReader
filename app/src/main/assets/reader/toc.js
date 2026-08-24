export function flattenToc(items, depth = 0) {
  return (items ?? []).flatMap(item => [
    { label: item.label ?? '', href: item.href ?? '', depth },
    ...flattenToc(item.subitems, depth + 1),
  ])
}
