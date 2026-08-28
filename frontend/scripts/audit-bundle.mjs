import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const dist = fileURLToPath(new URL('../dist/', import.meta.url))
const forbidden = [
  ['database connection string', /jdbc:(?:mysql|postgresql)|(?:mysql|postgres(?:ql)?):\/\//i],
  ['server-side database credentials', /DB_PASSWORD|DB_USERNAME|spring\.datasource/i],
  ['JWT signing secret', /JWT_SECRET/i],
  ['private key', /BEGIN (?:RSA |EC )?PRIVATE KEY|PRIVATE_KEY/i],
]

async function filesUnder(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map(entry => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? filesUnder(path) : [path]
  }))
  return files.flat()
}

const files = await filesUnder(dist)
const sourceMaps = files.filter(file => extname(file) === '.map')
const violations = []

for (const file of files.filter(path => /\.(?:html|js|css|json|svg)$/.test(path))) {
  const content = await readFile(file, 'utf8')
  for (const [label, pattern] of forbidden) {
    if (pattern.test(content)) violations.push(`${relative(dist, file)}: ${label}`)
  }
}

if (sourceMaps.length || violations.length) {
  if (sourceMaps.length) console.error(`Public source maps are not allowed: ${sourceMaps.map(file => relative(dist, file)).join(', ')}`)
  violations.forEach(violation => console.error(`Sensitive bundle pattern: ${violation}`))
  process.exitCode = 1
} else {
  console.log(`Bundle audit passed (${files.length} files, no source maps or server-secret patterns).`)
}
