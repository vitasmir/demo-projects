import { DataTables3Component } from './data-tables3.component';

describe('DataTables3Component intraday changes', () => {
  let component: DataTables3Component;

  beforeEach(() => {
    component = Object.create(DataTables3Component.prototype) as DataTables3Component;
  });

  it('parses an UPDATE row even when the export has no CREATE row', () => {
    const content = [
      csvRow([
        'timestamp', 'action', 'employee_id', 'old_name', 'new_name', 'old_first_name', 'new_first_name',
        'old_last_name', 'new_last_name', 'old_position', 'new_position',
        'old_extn', 'new_extn', 'old_salary', 'new_salary', 'old_start_date', 'new_start_date',
        'old_office', 'new_office', 'old_has_manager_rights', 'new_has_manager_rights',
        'old_manager_id', 'new_manager_id', 'old_manager_name', 'new_manager_name'
      ]),
      csvRow([
        '2026-08-14T10:00:00', 'UPDATE', 'employee-1', 'Old, Name', 'New, Name', 'Old', 'New', 'Name', 'Surname',
        'Developer', 'Senior Developer', '100', '101', '50000', '55000', '2026-01-01', '2026-08-01', 'Prague', 'Brno', 'false', 'true',
        'manager-old', 'manager-new', 'Old Manager', 'New Manager'
      ])
    ].join('\n');

    const changes = (component as any).parseIntradayChanges(content);

    expect(changes).toEqual([jasmine.objectContaining({
      action: 'UPDATE',
      employeeId: 'employee-1',
      newName: 'New, Name',
      newFirstName: 'New',
      newLastName: 'Surname',
      newManagerId: 'manager-new'
    })]);
  });

  it('rejects an unknown intraday action', () => {
    const content = [
      csvRow(['timestamp', 'action', 'employee_id']),
      csvRow(['2026-08-14T10:00:00', 'RENAME', 'employee-1', ...Array(18).fill('')])
    ].join('\n');

    expect(() => (component as any).parseIntradayChanges(content))
      .toThrowError('Unsupported intraday action "RENAME".');
  });

  it('preserves the CSV event order', () => {
    const changes = [
      { action: 'DELETE', employeeId: 'parent', oldManagerId: '' },
      { action: 'CREATE', employeeId: 'child', managerId: 'parent' },
      { action: 'UPDATE', employeeId: 'existing' },
      { action: 'DELETE', employeeId: 'child', oldManagerId: 'parent' },
      { action: 'CREATE', employeeId: 'parent', managerId: '' }
    ];

    expect(changes.map((change: any) => `${change.action}:${change.employeeId}`)).toEqual([
      'DELETE:parent', 'CREATE:child', 'UPDATE:existing', 'DELETE:child', 'CREATE:parent'
    ]);
  });
});

function csvRow(fields: string[]): string {
  return fields.map(field => `"${field.replace(/"/g, '""')}"`).join(',');
}
