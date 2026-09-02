import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

import {
  classificationTone,
  defaultKpiPeriod,
  formatKpiNumber,
  formatKpiRate,
  KPI_PERIOD_TYPES,
  kpiPeriodLabel,
  kpiPeriodOptions,
  kpiYearOptions,
} from '../src/kpiModel.ts'

test('exposes all four KPI period types required by the specification', () => {
  assert.deepEqual(KPI_PERIOD_TYPES.map(item => item.value), ['MONTH', 'QUARTER', 'HALF_YEAR', 'YEAR'])
  assert.equal(kpiPeriodOptions('MONTH').length, 12)
  assert.equal(kpiPeriodOptions('QUARTER').length, 4)
  assert.equal(kpiPeriodOptions('HALF_YEAR').length, 2)
  assert.deepEqual(kpiPeriodOptions('YEAR'), [{ value: 1, label: 'Cả năm' }])
})

test('does not offer a KPI period before that period has started', () => {
  const date = new Date(2026, 8, 2)
  assert.deepEqual(kpiPeriodOptions('MONTH', 2026, date).map(option => option.value), [1, 2, 3, 4, 5, 6, 7, 8, 9])
  assert.deepEqual(kpiPeriodOptions('QUARTER', 2026, date).map(option => option.value), [1, 2, 3])
  assert.deepEqual(kpiPeriodOptions('HALF_YEAR', 2026, date).map(option => option.value), [1, 2])
  assert.deepEqual(kpiPeriodOptions('YEAR', 2026, date).map(option => option.value), [1])
  assert.equal(kpiPeriodOptions('MONTH', 2027, date).length, 0)
  assert.equal(kpiPeriodOptions('MONTH', 2025, date).length, 12)
  assert.equal(kpiPeriodOptions('QUARTER', 2025, date).length, 4)
  assert.equal(kpiPeriodOptions('HALF_YEAR', 2025, date).length, 2)
})

test('requires one version window to cover the complete KPI period after metadata loads', () => {
  const date = new Date(2026, 8, 2)
  const versions = [{
    versionId: 'V-JULY',
    name: 'Version beginning in July',
    effectiveFrom: '2026-07-01',
    effectiveTo: null,
    status: 'ACTIVE',
  }]
  assert.deepEqual(kpiPeriodOptions('MONTH', 2026, date, versions).map(option => option.value), [7, 8, 9])
  assert.deepEqual(kpiPeriodOptions('QUARTER', 2026, date, versions).map(option => option.value), [3])
  assert.deepEqual(kpiPeriodOptions('HALF_YEAR', 2026, date, versions).map(option => option.value), [2])
  assert.deepEqual(kpiPeriodOptions('YEAR', 2026, date, versions), [])
  assert.deepEqual(kpiPeriodOptions('MONTH', 2026, date), kpiPeriodOptions('MONTH', 2026, date, undefined))
})

test('does not offer a KPI period covered by overlapping selectable versions', () => {
  const date = new Date(2026, 8, 2)
  const overlappingVersions = [
    {
      versionId: 'V1',
      name: 'Version 1',
      effectiveFrom: '2026-01-01',
      effectiveTo: null,
      status: 'ACTIVE',
    },
    {
      versionId: 'V2',
      name: 'Version 2',
      effectiveFrom: '2026-01-01',
      effectiveTo: '2026-12-31',
      status: 'ACTIVE',
    },
  ]

  assert.deepEqual(kpiPeriodOptions('MONTH', 2026, date, overlappingVersions), [])
  assert.deepEqual(kpiPeriodOptions('QUARTER', 2026, date, overlappingVersions), [])
  assert.deepEqual(kpiPeriodOptions('HALF_YEAR', 2026, date, overlappingVersions), [])
  assert.deepEqual(kpiPeriodOptions('YEAR', 2026, date, overlappingVersions), [])
})

test('selects the current calendar period and creates dynamic year choices', () => {
  const date = new Date(2026, 8, 2)
  assert.equal(defaultKpiPeriod('MONTH', date), 9)
  assert.equal(defaultKpiPeriod('QUARTER', date), 3)
  assert.equal(defaultKpiPeriod('HALF_YEAR', date), 2)
  assert.equal(defaultKpiPeriod('YEAR', date), 1)
  assert.deepEqual(kpiYearOptions(date), [2026])
  assert.deepEqual(kpiYearOptions(date, []), [])
  assert.deepEqual(kpiYearOptions(date, [{
    versionId: 'V1',
    name: 'Version 1',
    effectiveFrom: '2024-01-01',
    effectiveTo: null,
    status: 'ACTIVE',
  }]), [2026, 2025, 2024])
  assert.deepEqual(kpiYearOptions(date, [{
    versionId: 'V2',
    name: 'Future version',
    effectiveFrom: '2027-01-01',
    effectiveTo: '2028-12-31',
    status: 'DRAFT',
  }]), [])
  assert.deepEqual(kpiYearOptions(date, [{
    versionId: 'V1',
    name: 'Closed version',
    effectiveFrom: '2022-03-01',
    effectiveTo: '2023-12-31',
    status: 'INACTIVE',
  }, {
    versionId: 'V2',
    name: 'Later version',
    effectiveFrom: '2025-01-01',
    effectiveTo: '2025-12-31',
    status: 'INACTIVE',
  }]), [2025, 2023, 2022])
})

test('formats the selected period and server numbers without recalculating scores', () => {
  assert.equal(kpiPeriodLabel('MONTH', 2026, 9), 'Tháng 9/2026')
  assert.equal(kpiPeriodLabel('QUARTER', 2026, 3), 'Quý 3/2026')
  assert.equal(kpiPeriodLabel('HALF_YEAR', 2026, 1), '6 tháng đầu năm/2026')
  assert.equal(kpiPeriodLabel('YEAR', 2026, 1), 'Năm 2026')
  assert.equal(formatKpiNumber(86.425), '86.42')
  assert.equal(formatKpiNumber(null), '—')
  assert.equal(formatKpiRate(0.7934), '79.34%')
})

test('maps the server classification only to a visual tone', () => {
  assert.equal(classificationTone('Xuất sắc'), 'excellent')
  assert.equal(classificationTone('Tốt'), 'good')
  assert.equal(classificationTone('Khá'), 'passed')
  assert.equal(classificationTone('Trung bình'), 'average')
  assert.equal(classificationTone('Không đạt'), 'attention')
})

test('KPI page is wired to the live endpoint and contains no mock score source', async () => {
  const [pageSource, apiSource, modelSource] = await Promise.all([
    readFile(new URL('../src/pages/KpiPage.tsx', import.meta.url), 'utf8'),
    readFile(new URL('../src/kpiApi.ts', import.meta.url), 'utf8'),
    readFile(new URL('../src/kpiModel.ts', import.meta.url), 'utf8'),
  ])

  assert.match(pageSource, /loadKpiDashboard/)
  assert.doesNotMatch(pageSource, /MOCK|FALLBACK_UNITS|buildMockRows|Dữ liệu mô phỏng/)
  assert.match(apiSource, /buildQuery/)
  assert.match(apiSource, /periodType/)
  assert.match(apiSource, /unitId/)
  assert.match(apiSource, /\/kpi\?/) 
  assert.match(apiSource, /\/kpi\/metadata/)
  assert.match(modelSource, /evidenceUrl: string \| null/)
})

test('KPI evidence drill-down is lazy, authenticated and keeps redacted rows inert', async () => {
  const [pageSource, apiSource] = await Promise.all([
    readFile(new URL('../src/pages/KpiPage.tsx', import.meta.url), 'utf8'),
    readFile(new URL('../src/kpiApi.ts', import.meta.url), 'utf8'),
  ])

  assert.match(pageSource, /loadKpiEvidence\(evidence\.evidenceUrl/)
  assert.match(pageSource, /!evidence\.redacted && Boolean\(evidence\.evidenceUrl\)/)
  assert.match(pageSource, /aria-expanded=\{isActive\}/)
  assert.match(pageSource, /downloadKpiEvidenceAttachment\(attachment\)/)
  assert.match(pageSource, /Đang tải chi tiết bản ghi/)
  assert.match(pageSource, /onClick=\{\(\) => openEvidence\(activeEvidence\)\}/)
  assert.match(apiSource, /api<KpiEvidenceRecordView>\(path, \{ signal \}\)/)
  assert.match(apiSource, /KPI_EVIDENCE_PATH\.test\(path\)/)
  assert.match(apiSource, /KPI_ATTACHMENT_PATH\.test\(path\)/)
  assert.match(apiSource, /case-documents/)
  assert.doesNotMatch(apiSource, /labor-case-documents/)
  assert.match(apiSource, /downloadFile\(path, attachment\.fileName\)/)
})

test('approved KPI adjustments are exposed as an auditable, permission-aware journal', async () => {
  const [pageSource, modelSource] = await Promise.all([
    readFile(new URL('../src/pages/KpiPage.tsx', import.meta.url), 'utf8'),
    readFile(new URL('../src/kpiModel.ts', import.meta.url), 'utf8'),
  ])

  assert.match(modelSource, /export interface KpiAdjustmentAuditView/)
  assert.match(modelSource, /adjustments: KpiAdjustmentAuditView\[\]/)
  assert.match(pageSource, /Nhật ký điều chỉnh đã duyệt/)
  assert.match(pageSource, /adjustment\.approvedBy/)
  assert.match(pageSource, /adjustment\.redacted/)
})
