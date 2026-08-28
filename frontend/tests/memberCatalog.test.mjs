import assert from 'node:assert/strict'
import test from 'node:test'

import { memberCompanyOptions, memberWorkplaceOptions } from '../src/portal/memberCatalog.ts'

const values = options => options.map(option => option.value)

test('member company catalog matches the controlled Excel list without duplicates', () => {
  const companies = values(memberCompanyOptions)
  assert.equal(companies.length, 18)
  assert.equal(new Set(companies).size, companies.length)
  assert.ok(companies.includes('CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ'))
  assert.ok(companies.includes('GENUINE PARTNER LOGISTICS (CAMBODIA) CO., LTD'))
})

test('member workplace catalog keeps the normalized Excel values without duplicates', () => {
  const workplaces = values(memberWorkplaceOptions)
  assert.equal(workplaces.length, 29)
  assert.equal(new Set(workplaces).size, workplaces.length)
  assert.ok(workplaces.includes('BSD'))
  assert.ok(workplaces.includes('VP-TCT'))
  assert.equal(memberWorkplaceOptions.find(option => option.value === 'VP-TCT')?.label, 'VP-TCT · VĂN PHÒNG TỔNG CÔNG TY')
})
