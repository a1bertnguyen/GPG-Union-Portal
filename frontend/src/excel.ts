import type { SpreadsheetImportResult } from './types'

export const importSummary = (result: SpreadsheetImportResult) =>
  `Đã xử lý ${result.run.successfulRows}/${result.run.totalRows} dòng: tạo mới ${result.createdRows}, cập nhật ${result.updatedRows}.`
