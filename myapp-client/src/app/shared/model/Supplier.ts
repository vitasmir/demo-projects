import { Injectable } from '@angular/core';

export class Supplier {
    public id: number;
    public name: string;
    public age: string;

    constructor(id: number, name: string, age: string) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
}
