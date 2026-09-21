import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TreesOfEmployeesComponent } from './trees-of-employees.component';

describe('TreesOfEmployeesComponent', () => {
  let component: TreesOfEmployeesComponent;
  let fixture: ComponentFixture<TreesOfEmployeesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreesOfEmployeesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TreesOfEmployeesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
