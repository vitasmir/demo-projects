import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ColorSketchModule } from 'ngx-color/sketch';
import { ColorPickerComponent } from './chat_model/chat.colorpicker.component';
import { ChatGroup } from './chat_model/chat-group';
import { JsonChatMessage } from './chat_model/json-chat-message';
import { ChatUser } from './chat_model/chat-user';


@Component({
  selector: 'app-websockets-log',
  standalone: true,
  imports: [CommonModule, FormsModule, ColorSketchModule, MatDialogModule],
  templateUrl: './websockets-chat-component.html',
  styleUrls: ['./websockets-chat-component.scss']
})
export class WebsocketsChatComponent implements OnInit, OnDestroy {
  private readonly stompClient: Client;
  private groupSubscription?: StompSubscription;
  private groupsEventsSubscription?: StompSubscription;
  private usersEventsSubscription?: StompSubscription;

  public groups: ChatGroup[] = [];
  public onlineUsers: ChatUser[] = [];
  public activeGroupId: string | null = null;
  public messageToSend = '';
  public receivedLogs: JsonChatMessage[] = [];
  public color = '#2f6bff';
  public nickname = '';
  public joinNickname = '';
  public joinColor = '#2f6bff';
  public profileReady = false;
  public newGroupName = '';
  private presenceNickname: string | null = null;
  private beforeUnloadHandler?: () => void;

  constructor(
    private readonly http: HttpClient,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog
  ) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        login: 'myuser',
        passcode: 'mypassword'
      },
      debug: (str) => { console.log(new Date(), str); },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.stompClient.onConnect = () => {
      this.subscribeToGroupEvents();
      this.subscribeToActiveGroup();
      this.subscribeToUsersEvents();
      this.loadOnlineUsers();
      if (this.profileReady) {
        this.loginPresence();
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };
  }

  ngOnInit(): void {
    this.stompClient.activate();
    this.loadGroups();

    this.beforeUnloadHandler = () => {
      this.bestEffortLogoutOnUnload();
    };
    window.addEventListener('beforeunload', this.beforeUnloadHandler);
  }

  ngOnDestroy(): void {
    this.groupSubscription?.unsubscribe();
    this.groupsEventsSubscription?.unsubscribe();
    this.usersEventsSubscription?.unsubscribe();
    if (this.beforeUnloadHandler) {
      window.removeEventListener('beforeunload', this.beforeUnloadHandler);
      this.beforeUnloadHandler = undefined;
    }
    this.logoutPresence();
    if (this.stompClient.active) {
      this.stompClient.deactivate();
    }
  }

  openColorPickerDialog(currentColor: string, mode: 'join' | 'chat'): void {
    const dialogRef = this.dialog.open(ColorPickerComponent, {
      width: '400px',
      data: currentColor,
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        if (mode === 'join') {
          this.joinColor = result;
        } else {
          this.color = result;
        }
        this.cdr.detectChanges();
      }
    });
  }

  saveProfile(): void {
    const name = this.joinNickname.trim();
    if (!name) {
      return;
    }
    const previousNickname = this.presenceNickname;
    this.nickname = name;
    this.color = this.joinColor;
    this.profileReady = true;

    if (previousNickname && previousNickname.toLowerCase() !== this.nickname.toLowerCase()) {
      this.logoutPresence(this.activeGroupId ?? undefined, previousNickname);
    }

    this.loginPresence();

    if (!this.activeGroupId && this.groups.length > 0) {
      this.selectGroup(this.groups[0].id);
    }
  }

  loadGroups(): void {
    this.http.get<ChatGroup[]>('/api/chat/groups').subscribe({
      next: (groups) => {
        const previousActiveId = this.activeGroupId;
        this.groups = groups;

        if (this.activeGroupId && !this.groups.some((g) => g.id === this.activeGroupId)) {
          this.activeGroupId = this.groups.length > 0 ? this.groups[0].id : null;
          this.receivedLogs = [];
        }

        if (!this.activeGroupId && this.groups.length > 0) {
          this.activeGroupId = this.groups[0].id;
        }

        if (this.profileReady && previousActiveId !== this.activeGroupId) {
          if (previousActiveId) {
            this.logoutPresence(previousActiveId);
          }
          if (this.activeGroupId) {
            this.loginPresence(this.activeGroupId);
          }
        }

        this.subscribeToActiveGroup();
        this.subscribeToUsersEvents();
        this.loadOnlineUsers();
        if (this.activeGroupId && (previousActiveId !== this.activeGroupId || this.receivedLogs.length === 0)) {
          this.loadHistory(this.activeGroupId);
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Cannot load chat groups', error);
      }
    });
  }

  createGroup(): void {
    const name = this.newGroupName.trim();
    if (!name) {
      return;
    }

    this.http.post<ChatGroup>('/api/chat/groups', { name }).subscribe({
      next: (group) => {
        this.newGroupName = '';
        this.groups = [...this.groups, group];
        this.selectGroup(group.id);
      },
      error: (error) => {
        console.error('Cannot create group', error);
      }
    });
  }

  deleteGroup(group: ChatGroup, event: Event): void {
    event.stopPropagation();
    this.http.delete<void>(`/api/chat/groups/${group.id}`).subscribe({
      next: () => {
        const wasActive = this.activeGroupId === group.id;
        const previousGroupId = this.activeGroupId;
        this.groups = this.groups.filter((g) => g.id !== group.id);

        if (wasActive) {
          if (this.profileReady && previousGroupId) {
            this.logoutPresence(previousGroupId);
          }
          this.receivedLogs = [];
          this.activeGroupId = this.groups.length > 0 ? this.groups[0].id : null;
          this.subscribeToActiveGroup();
          this.subscribeToUsersEvents();
          this.loadOnlineUsers();
          if (this.profileReady && this.activeGroupId) {
            this.loginPresence(this.activeGroupId);
          }
          if (this.activeGroupId) {
            this.loadHistory(this.activeGroupId);
          }
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Cannot delete group', error);
      }
    });
  }

  selectGroup(groupId: string): void {
    if (this.activeGroupId === groupId) {
      return;
    }

    const previousGroupId = this.activeGroupId;
    if (this.profileReady && previousGroupId) {
      this.logoutPresence(previousGroupId);
    }

    this.activeGroupId = groupId;
    this.receivedLogs = [];
    this.subscribeToActiveGroup();
    this.subscribeToUsersEvents();
    this.loadOnlineUsers();

    if (this.profileReady) {
      this.loginPresence(groupId);
    }

    this.loadHistory(groupId);
  }

  loadHistory(groupId: string): void {
    this.http.get<JsonChatMessage[]>(`/api/chat/groups/${groupId}/messages`).subscribe({
      next: (messages) => {
        this.receivedLogs = messages;
        this.cdr.detectChanges();
        this.scrollToBottom(true);
      },
      error: (error) => {
        console.error('Cannot load chat history', error);
      }
    });
  }

  private subscribeToActiveGroup(): void {
    this.groupSubscription?.unsubscribe();

    if (!this.activeGroupId || !(this.stompClient as any).connected) {
      return;
    }

    this.groupSubscription = this.stompClient.subscribe(
      `/topic/chat.group.${this.activeGroupId}`,
      (message: IMessage) => {
        const chatMessage = JSON.parse(message.body) as JsonChatMessage;
        if (chatMessage.groupId !== this.activeGroupId) {
          return;
        }
        this.receivedLogs.push(chatMessage);
        this.cdr.detectChanges();
        this.scrollToBottom(false);
      }
    );
  }

  private subscribeToGroupEvents(): void {
    this.groupsEventsSubscription?.unsubscribe();

    if (!(this.stompClient as any).connected) {
      return;
    }

    this.groupsEventsSubscription = this.stompClient.subscribe('/topic/chat.groups', (_message: IMessage) => {
      this.loadGroups();
    });
  }

  private subscribeToUsersEvents(): void {
    this.usersEventsSubscription?.unsubscribe();

    if (!this.activeGroupId || !(this.stompClient as any).connected) {
      return;
    }

    const groupId = this.activeGroupId;
    this.usersEventsSubscription = this.stompClient.subscribe(`/topic/chat.users.${groupId}`, (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body) as ChatUser[];
        this.onlineUsers = Array.isArray(payload) ? payload : [];
        this.cdr.detectChanges();
      } catch {
        this.loadOnlineUsers();
      }
    });
  }

  private loadOnlineUsers(): void {
    if (!this.activeGroupId) {
      this.onlineUsers = [];
      this.cdr.detectChanges();
      return;
    }

    this.http.get<ChatUser[]>(`/api/chat/presence/users/${this.activeGroupId}`).subscribe({
      next: (users) => {
        this.onlineUsers = Array.isArray(users) ? users : [];
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Cannot load online users', error);
      }
    });
  }

  private loginPresence(groupIdOverride?: string): void {
    const groupId = (groupIdOverride ?? this.activeGroupId ?? '').trim();
    const nickname = this.nickname.trim();
    if (!groupId || !nickname) {
      return;
    }

    this.http.post<ChatUser>('/api/chat/presence/login', {
      groupId,
      nickname,
      color: this.color || '#2f6bff'
    }).subscribe({
      next: () => {
        this.presenceNickname = nickname;
        this.loadOnlineUsers();
      },
      error: (error) => {
        console.error('Cannot register online presence', error);
      }
    });
  }

  private logoutPresence(groupIdOverride?: string, nicknameOverride?: string): void {
    const groupId = (groupIdOverride ?? this.activeGroupId ?? '').trim();
    const nickname = (nicknameOverride ?? this.presenceNickname ?? this.nickname).trim();
    if (!groupId || !nickname) {
      return;
    }

    this.http.post<void>('/api/chat/presence/logout', { groupId, nickname }).subscribe({
      next: () => {
        if (!nicknameOverride || this.presenceNickname === nicknameOverride) {
          this.presenceNickname = null;
        }
      },
      error: () => {
        // ignore best-effort logout
      }
    });
  }

  private bestEffortLogoutOnUnload(): void {
    const groupId = (this.activeGroupId ?? '').trim();
    const nickname = (this.presenceNickname ?? this.nickname).trim();
    if (!groupId || !nickname) {
      return;
    }

    const payload = JSON.stringify({ groupId, nickname });

    try {
      const blob = new Blob([payload], { type: 'application/json' });
      if (navigator.sendBeacon('/api/chat/presence/logout', blob)) {
        return;
      }
    } catch {
      // fallback below
    }

    try {
      void fetch('/api/chat/presence/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: payload,
        keepalive: true
      });
    } catch {
      // ignore best-effort only
    }
  }

  getFirstCharacterUppercase(name: string) {
    if (!name) {
      return '';
    }
    return name.charAt(0).toUpperCase();
  }

  trackByIndex(index: number, item: JsonChatMessage) {
    return index;
  }

  trackGroup(index: number, group: ChatGroup): string {
    return group.id;
  }

  trackOnlineUser(index: number, user: ChatUser): string {
    return user.nickname.toLowerCase();
  }

  sendMessage(input?: HTMLInputElement): void {
    if (!this.profileReady || !this.activeGroupId) {
      return;
    }

    const value = input ? input.value : this.messageToSend;
    const msg = value ? value.trim() : '';
    if (msg !== '' && (this.stompClient as any).connected) {
      const chatMsg = {
        groupId: this.activeGroupId,
        nickname: this.nickname,
        color: this.color || '#000000',
        content: msg
      };
      this.stompClient.publish({ destination: '/app/chat.send', body: JSON.stringify(chatMsg) });
      if (input) {
        input.value = '';
      } else {
        this.messageToSend = '';
      }
    }
  }

  formatTimestamp(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    const datePart = date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    const timePart = date.toLocaleTimeString('cs-CZ', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    return `${datePart} ${timePart}`;
  }

  @ViewChild('logContainer', { static: false }) logContainerRef?: ElementRef<HTMLDivElement>;

  private scrollToBottom(force: boolean): void {
    const el: HTMLElement | null = this.logContainerRef?.nativeElement || document.querySelector('.log-container');
    if (!el) return;

    requestAnimationFrame(() => {
      try {
        if (force) {
          el.scrollTop = el.scrollHeight;
          return;
        }
        const tolerancePx = 80;
        const distanceFromBottom = el.scrollHeight - el.clientHeight - el.scrollTop;
        const userAtBottom = distanceFromBottom <= tolerancePx;
        if (userAtBottom) {
          el.scrollTop = el.scrollHeight;
        }
      } catch (e) {
        console.debug('scroll error', e);
      }
    });
  }
}
