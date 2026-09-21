import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { User } from '../shared/model/User';


@Injectable({
    providedIn: 'root'
})
export class UserService {
    // Use a relative path for API calls to work with the Nginx proxy
    private baseUrl = "/api";

    constructor(private http: HttpClient) { }

    getUsers(): Observable<User[]> {
        return this.http.get<User[]>(`${this.baseUrl}/users`);
    }

    getUser(id: String): Observable<User> {
        return this.http.get<User>(`${this.baseUrl}/users/${id}`);
    }

    createUser(user: User): Observable<User> {
        return this.http.post<User>(`${this.baseUrl}/users`, user);
    }

    updateUser(id: string, user: User): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/users/${id}`, user);
    }

    deleteUser(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/users/${id}`);
    }

}
