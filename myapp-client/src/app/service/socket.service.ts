import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, filter, switchMap, take, tap } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface LogMessage {
    timestamp: Date;
    message: string;
}

@Injectable({
    providedIn: 'root'
})
export class SocketService implements OnDestroy {
    private clients: Map<string, Client> = new Map();
    private logSubjects: Map<string, Subject<LogMessage>> = new Map();

    private getClient(host: string): Client {
        if (!this.clients.has(host)) {
            const client = this._createClient(host);
            this.clients.set(host, client);
            client.activate();
        }
        return this.clients.get(host)!;
    }

    private _createClient(host: string): Client {
        const client = new Client({
            // The host should be the origin of the window, which will be proxied by Nginx.
            // The path '/ws' must match the WebSocket location block in your nginx.conf.
            webSocketFactory: () => new SockJS(`${window.location.origin}/ws`),
            debug: (str) => { console.log(new Date(), str); },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log(`Connected to ${host}`);
            // Resubscribe to topics if needed upon reconnection
            this.resubscribeToTopics(client, host);
        };

        client.onStompError = (frame) => {
            console.error(`Broker reported error on ${host}: ` + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        return client;
    }

    private resubscribeToTopics(client: Client, host: string) {
        if (this.logSubjects.has(host)) {
            this.subscribeToLogTopic(client, host);
        }
        // Add other topic resubscriptions here
    }

    private subscribeToLogTopic(client: Client, host: string) {
        const logTopic = `/topic/logs`;
        client.subscribe(logTopic, (message: IMessage) => {
            const log: LogMessage = JSON.parse(message.body);
            this.logSubjects.get(host)?.next(log);
        });
    }

    // Listen for new log events from a specific server
    onNewLog(host: string): Observable<LogMessage> {
        if (!this.logSubjects.has(host)) {
            this.logSubjects.set(host, new Subject<LogMessage>());
            const client = this.getClient(host);
            if (client.connected) {
                this.subscribeToLogTopic(client, host);
            }
        }
        return this.logSubjects.get(host)!.asObservable();
    }

    ngOnDestroy(): void {
        this.clients.forEach(client => client.deactivate());
    }
}
