import { useState } from 'react'
import { api, downloadFile } from '../api'
import type { SpreadsheetImportResult } from '../types'

type Props = {
  resource: string
  filename: string
  importLabel?: string
  templateLabel?: string
  disabled?: boolean
  onImported?: (result: SpreadsheetImportResult) => void | Promise<void>
  onError?: (message: string) => void
}

export default function ExcelImportActions({
  resource,
  filename,
  importLabel = 'Nhập Excel',
  templateLabel = 'Tải mẫu Excel',
  disabled = false,
  onImported,
  onError,
}: Props) {
  const [busy, setBusy] = useState<'download' | 'import' | ''>('')

  const download = async () => {
    setBusy('download')
    try {
      await downloadFile(`/spreadsheets/${resource}/template.xlsx`, filename)
    } catch (err) {
      onError?.(err instanceof Error ? err.message : 'Không thể tải file Excel mẫu')
    } finally {
      setBusy('')
    }
  }

  const upload = async (file: File) => {
    setBusy('import')
    const body = new FormData()
    body.append('file', file)
    try {
      const result = await api<SpreadsheetImportResult>(`/spreadsheets/${resource}/import`, { method: 'POST', body })
      await onImported?.(result)
    } catch (err) {
      onError?.(err instanceof Error ? err.message : 'Không thể nhập file Excel')
    } finally {
      setBusy('')
    }
  }

  const isBusy = Boolean(busy) || disabled
  return (
    <>
      <button className="button button--ghost" disabled={isBusy} onClick={() => void download()}>
        {busy === 'download' ? 'Đang tạo mẫu…' : templateLabel}
      </button>
      <label className={`button button--ghost ${isBusy ? 'button--disabled' : ''}`}>
        {busy === 'import' ? 'Đang nhập…' : importLabel}
        <input type="file" hidden accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" disabled={isBusy} onChange={event => {
          const file = event.target.files?.[0]
          if (file) void upload(file)
          event.target.value = ''
        }} />
      </label>
    </>
  )
}
