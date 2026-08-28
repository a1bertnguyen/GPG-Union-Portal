import type { BaseRecord } from '../../types'

export function grouped(records: BaseRecord[], field: string): Array<[string, number]> {
  const values = new Map<string, number>()
  records.forEach(item => {
    const key = String(item[field] ?? 'Chưa phân loại')
    values.set(key, (values.get(key) ?? 0) + 1)
  })
  return [...values.entries()].sort((left, right) => right[1] - left[1])
}

export function groupedAmount(records: BaseRecord[], field: string): Array<[string, number]> {
  const values = new Map<string, number>()
  records.forEach(item => {
    const key = String(item[field] ?? 'Chưa phân loại')
    values.set(key, (values.get(key) ?? 0) + Number(item.amount ?? 0))
  })
  return [...values.entries()].sort((left, right) => right[1] - left[1])
}

export function sum(records: BaseRecord[], field: string) {
  return records.reduce((total, item) => total + Number(item[field] ?? 0), 0)
}
