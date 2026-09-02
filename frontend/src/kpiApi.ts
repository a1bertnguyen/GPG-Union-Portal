import { api, buildQuery, downloadFile } from './api'
import type {
  KpiDashboardParams,
  KpiDashboardView,
  KpiEvidenceAttachmentView,
  KpiEvidenceRecordView,
  KpiMetadataView,
} from './kpiModel'

const KPI_EVIDENCE_PATH = /^\/kpi\/evidence\/(?:union-unit|member|member-change|monthly-report|report-obligation|welfare|labor-case|activity|finance-entry|no-occurrence)\/[A-Za-z0-9:_-]+$/
const KPI_ATTACHMENT_PATH = /^\/(?:member-documents|welfare-documents|case-documents|activity-media|finance-documents)\/\d+\/download$/

function apiRelativePath(path: string): string {
  const value = path.trim()
  if (!value || value.startsWith('//') || value.includes('\\') || /^[a-z][a-z0-9+.-]*:/i.test(value)) {
    throw new Error('Đường dẫn chứng cứ KPI không hợp lệ.')
  }
  const pathOnly = value.split(/[?#]/, 1)[0]
  let decodedPath: string
  try {
    decodedPath = decodeURIComponent(pathOnly)
  } catch {
    throw new Error('Đường dẫn chứng cứ KPI không hợp lệ.')
  }
  if (decodedPath.split('/').some(segment => segment === '.' || segment === '..')) {
    throw new Error('Đường dẫn chứng cứ KPI không hợp lệ.')
  }
  return value.startsWith('/') ? value : `/${value}`
}

export function loadKpiMetadata(signal?: AbortSignal) {
  return api<KpiMetadataView>('/kpi/metadata', { signal })
}

export function loadKpiDashboard(params: KpiDashboardParams, signal?: AbortSignal) {
  const query = buildQuery({
    periodType: params.periodType,
    year: params.year,
    period: params.period,
    unitId: params.unitId,
  })
  return api<KpiDashboardView>(`/kpi?${query}`, { signal })
}

export function loadKpiEvidence(evidenceUrl: string, signal?: AbortSignal) {
  const path = apiRelativePath(evidenceUrl)
  if (!KPI_EVIDENCE_PATH.test(path)) throw new Error('Liên kết chứng cứ KPI không hợp lệ.')
  return api<KpiEvidenceRecordView>(path, { signal })
}

export function downloadKpiEvidenceAttachment(attachment: KpiEvidenceAttachmentView) {
  const path = apiRelativePath(attachment.downloadPath)
  if (!KPI_ATTACHMENT_PATH.test(path)) throw new Error('Liên kết tệp chứng cứ KPI không hợp lệ.')
  return downloadFile(path, attachment.fileName)
}
