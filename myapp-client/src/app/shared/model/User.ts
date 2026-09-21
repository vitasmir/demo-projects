export class User {
    public id: string;
    public firstName: string;
    public lastName: string;
    public username: string;
    public age: string;
    public email: string;


    constructor(id: string, firstName: string, lastName: string,
        username: string, email: string, age: string) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.age = age;
        this.email = email;
    }
}