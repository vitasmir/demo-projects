import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  ViewChild,
  ElementRef
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface StockItem { symbol: string; name: string; }
interface StockPricePoint { timestamp: number; price: number; }
interface StockHistoryResponse { symbol: string; period: string; points: StockPricePoint[]; }
interface TradeOrder { id: number; symbol: string; orderType: string; price: number; quantity: number; status: string; }
type PeriodKey = '1h' | '1d' | '3m' | '1y' | 'max';
type ChartType = 'line' | 'candles';
interface AxisTick { value: number; label: string; topPercent: number; }
interface TimeTick { label: string; leftPercent: number; }
interface CandleStick {
  x: number;
  wickTop: number;
  wickBottom: number;
  bodyTop: number;
  bodyHeight: number;
  width: number;
  bullish: boolean;
}

const TOOLTIP_WIDTH = 176;
const TOOLTIP_HEIGHT = 58;

@Component({
  selector: 'app-broker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './broker.component.html',
  styleUrls: ['./broker.component.scss']
})
export class BrokerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('chartContainer') chartContainer!: ElementRef;
  @ViewChild('chartStage') chartStage!: ElementRef;

  public stocks: StockItem[] = [];
  public selectedStock: StockItem | null = null;
  public selectedPeriod: PeriodKey = '1d';
  public selectedChartType: ChartType = 'line';
  public points: StockPricePoint[] = [];
  public errorMessage = '';
  public hoveredPoint: StockPricePoint | null = null;
  public hoverX = 0;
  public hoverY = 0;
  
  public orderPrice: number | null = null;
  public orderQuantity: number = 1;

  public asks: TradeOrder[] = [];
  public bids: TradeOrder[] = [];
  public liveConnected = false;
  
  private updateSubscription: Subscription | null = null;
  private readonly stompClient: Client;
  private historySubscription?: StompSubscription;
  private resizeObserver?: ResizeObserver;

  public chartWidth = 600;
  public chartHeight = 350;
  public readonly chartPaddingTop = 10;
  public readonly chartPaddingRight = 12;
  public readonly chartPaddingBottom = 24;
  public readonly chartPaddingLeft = 68;

  constructor(private readonly http: HttpClient, private readonly cdr: ChangeDetectorRef) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${window.location.origin}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.stompClient.onConnect = () => {
      this.liveConnected = true;
      this.subscribeToLiveHistory();
      this.cdr.detectChanges();
    };

    this.stompClient.onWebSocketClose = () => {
      this.liveConnected = false;
      this.cdr.detectChanges();
    };

    this.stompClient.onStompError = () => {
      this.liveConnected = false;
      this.cdr.detectChanges();
    };
  }

  ngOnInit(): void {
    this.http.get<StockItem[]>('/api/stocks/nasdaq').subscribe({
      next: (items) => {
        this.stocks = items || [];
        if (this.stocks.length > 0) this.selectStock(this.stocks[0]);
      },
      error: () => this.errorMessage = 'Nepodařilo se načíst akcie.'
    });
    
    this.updateSubscription = interval(4000).subscribe(() => this.loadOrders());
    this.stompClient.activate();
  }

  ngAfterViewInit(): void {
    this.observeChartSize();
    requestAnimationFrame(() => this.onResize());
  }

  @HostListener('window:resize')
  onResize(): void {
    if (this.chartStage?.nativeElement) {
      const { clientWidth, clientHeight } = this.chartStage.nativeElement as HTMLElement;
      this.chartWidth = Math.max(clientWidth, 320);
      this.chartHeight = Math.max(clientHeight, 220);
      this.cdr.detectChanges();
    }
  }

  ngOnDestroy(): void {
    if (this.updateSubscription) this.updateSubscription.unsubscribe();
    this.historySubscription?.unsubscribe();
    this.resizeObserver?.disconnect();
    if (this.stompClient.active) {
      this.stompClient.deactivate();
    }
  }

  selectStock(stock: StockItem): void {
    if (!stock) return;
    this.selectedStock = stock;
    this.loadHistory();
    this.loadOrders();
    this.subscribeToLiveHistory();
  }

  setPeriod(period: PeriodKey): void {
    if (this.selectedPeriod === period) return;
    this.selectedPeriod = period;
    this.loadHistory();
    this.subscribeToLiveHistory();
  }

  setChartType(type: ChartType): void {
    if (this.selectedChartType === type) {
      return;
    }

    this.selectedChartType = type;
  }

  placeLimitOrder(type: 'BUY' | 'SELL'): void {
    if (!this.selectedStock || !this.selectedStock.symbol) return;
    if (!this.orderPrice || this.orderPrice <= 0 || !this.orderQuantity || this.orderQuantity <= 0) return;
    
    this.http.post('/api/broker/order', {
      symbol: this.selectedStock.symbol,
      orderType: type,
      price: this.orderPrice,
      quantity: this.orderQuantity
    }).subscribe(() => {
       this.loadOrders();
       // Don't reset inputs to allow fast trading
    });
  }

  private loadOrders(): void {
    if (!this.selectedStock) return;
    const symbol = this.selectedStock.symbol;
    this.http.get<TradeOrder[]>(`/api/broker/${symbol}/asks`).subscribe(data => this.asks = data);
    this.http.get<TradeOrder[]>(`/api/broker/${symbol}/bids`).subscribe(data => this.bids = data);
  }

  private loadHistory(isRealtimeRefresh = false): void {
    if (!this.selectedStock) return;
    const symbol = encodeURIComponent(this.selectedStock.symbol);
    this.http.get<StockHistoryResponse>(`/api/stocks/${symbol}/history?period=${this.selectedPeriod}`).subscribe(res => {
      this.applyHistoryResponse(res, isRealtimeRefresh);
    });
  }

  private subscribeToLiveHistory(): void {
    this.historySubscription?.unsubscribe();

    if (!this.selectedStock || !this.stompClient.connected) {
      return;
    }

    const destination = `/topic/broker/history/${this.selectedPeriod}/${this.selectedStock.symbol}`;
    this.historySubscription = this.stompClient.subscribe(destination, (message: IMessage) => {
      const response = JSON.parse(message.body) as StockHistoryResponse;
      this.applyHistoryResponse(response, true);
    });
  }

  private applyHistoryResponse(response: StockHistoryResponse, isRealtimeRefresh: boolean): void {
    this.points = response?.points || [];
    if (this.points.length > 0 && (!isRealtimeRefresh || !this.orderPrice || this.orderPrice <= 0)) {
      this.orderPrice = this.lastPrice;
    }
    this.cdr.detectChanges();
    requestAnimationFrame(() => this.onResize());
  }

  private observeChartSize(): void {
    if (!this.chartStage?.nativeElement || typeof ResizeObserver === 'undefined') {
      return;
    }

    this.resizeObserver?.disconnect();
    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.resizeObserver.observe(this.chartStage.nativeElement);
  }

  get lastPrice(): number {
    return this.points.length ? this.points[this.points.length - 1].price : 0;
  }

  get firstPrice(): number {
    return this.points.length ? this.points[0].price : 0;
  }

  get priceChange(): number {
    return this.lastPrice - this.firstPrice;
  }

  get priceChangePercent(): number {
    if (!this.firstPrice) {
      return 0;
    }

    return (this.priceChange / this.firstPrice) * 100;
  }

  get lastTimestamp(): number {
    return this.points.length ? this.points[this.points.length - 1].timestamp : 0;
  }

  get minPrice(): number { return this.points.length ? Math.min(...this.points.map((p) => p.price)) : 0; }
  get maxPrice(): number { return this.points.length ? Math.max(...this.points.map((p) => p.price)) : 0; }

  get yAxisTicks(): AxisTick[] {
    if (!this.points.length) {
      return [];
    }

    const min = this.minPrice;
    const max = this.maxPrice;
    const tickCount = 5;
    const span = Math.max(max - min, 0.0001);

    return Array.from({ length: tickCount }, (_, index) => {
      const ratio = index / (tickCount - 1);
      const value = max - span * ratio;
      return {
        value,
        label: this.formatPrice(value),
        topPercent: ratio * 100
      };
    });
  }

  get xAxisTicks(): TimeTick[] {
    const total = this.chartPoints.length;
    if (!total) {
      return [];
    }

    if (total === 1) {
      return [{ leftPercent: 0, label: this.formatAxisDate(this.chartPoints[0].timestamp) }];
    }

    const preferredTickCount = Math.max(2, Math.floor(this.getXAxisPlotWidth() / 90));
    const periodCap = this.selectedPeriod === '1h'
      ? 14
      : this.selectedPeriod === '1d'
        ? 12
        : this.selectedPeriod === '3m'
          ? 10
          : 8;
    const tickCount = Math.min(periodCap, preferredTickCount, total);
    const indexes = new Set<number>();

    for (let i = 0; i < tickCount; i++) {
      indexes.add(Math.round((i * (total - 1)) / (tickCount - 1)));
    }

    return [...indexes]
      .sort((a, b) => a - b)
      .map((index) => ({
        leftPercent: (index / (total - 1)) * 100,
        label: this.formatAxisDate(this.chartPoints[index].timestamp)
      }));
  }

  get lineRenderedPoints(): StockPricePoint[] {
    const total = this.points.length;
    if (total <= 2) {
      return this.points;
    }

    const plotWidth = Math.max(Math.floor(this.getXAxisPlotWidth()), 1);
    const maxRenderablePoints = plotWidth + 1;
    if (total <= maxRenderablePoints) {
      return this.points;
    }

    const indexes = new Set<number>();
    for (let i = 0; i < maxRenderablePoints; i++) {
      indexes.add(Math.round((i * (total - 1)) / (maxRenderablePoints - 1)));
    }

    return [...indexes]
      .sort((a, b) => a - b)
      .map((index) => this.points[index]);
  }

  get chartPoints(): StockPricePoint[] {
    return this.selectedChartType === 'line' ? this.lineRenderedPoints : this.points;
  }

  get hoverXPercent(): number {
    return this.chartWidth ? (this.hoverX / this.chartWidth) * 100 : 0;
  }

  get hoverYPercent(): number {
    return this.chartHeight ? (this.hoverY / this.chartHeight) * 100 : 0;
  }

  get lastPointX(): number {
    return this.chartPoints.length ? this.getPointX(this.chartPoints.length - 1) : 0;
  }

  get lastPointY(): number {
    return this.points.length ? this.getPointY(this.lastPrice) : 0;
  }

  get tooltipLeft(): number {
    const preferred = this.hoverX + 16;
    return Math.max(12, Math.min(preferred, this.chartWidth - TOOLTIP_WIDTH - 12));
  }

  get tooltipTop(): number {
    const preferred = this.hoverY - TOOLTIP_HEIGHT - 14;
    return Math.max(12, Math.min(preferred, this.chartHeight - TOOLTIP_HEIGHT - 12));
  }

  formatPrice(value: number): string {
    return Number.isFinite(value) ? value.toFixed(2) : '-';
  }

  formatDateTime(timestamp: number): string {
    if (!timestamp) {
      return '-';
    }

    const date = new Date(timestamp);
    const datePart = date.toLocaleDateString('cs-CZ', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    const timePart = date.toLocaleTimeString('cs-CZ', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    return `${datePart} ${timePart}`;
  }

  formatAxisDate(timestamp: number): string {
    if (!timestamp) {
      return '-';
    }

    const date = new Date(timestamp);
    if (this.selectedPeriod === '1h') {
      return date.toLocaleTimeString('cs-CZ', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    }

    if (this.selectedPeriod === '1d') {
      return date.toLocaleTimeString('cs-CZ', {
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    return date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit'
    });
  }

  getYAxisPosition(topPercent: number): number {
    const usableH = this.chartHeight - this.chartPaddingTop - this.chartPaddingBottom;
    return this.chartPaddingTop + usableH * (topPercent / 100);
  }

  getXAxisPosition(leftPercent: number): number {
    const usableW = this.getXAxisPlotWidth();
    return this.chartPaddingLeft + usableW * (leftPercent / 100);
  }

  get plotEndX(): number {
    return this.chartPaddingLeft + this.getXAxisPlotWidth();
  }

  get plotEndY(): number {
    return this.chartHeight - this.chartPaddingBottom;
  }

  onChartMouseMove(event: MouseEvent, svg: Element): void {
    if (!(svg instanceof SVGSVGElement)) {
      return;
    }

    if (!this.chartPoints.length) {
      this.hoveredPoint = null;
      return;
    }

    const ctm = svg.getScreenCTM();
    if (!ctm) {
      return;
    }

    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;

    const svgPoint = point.matrixTransform(ctm.inverse());
    const x = svgPoint.x;
    const index = this.getNearestIndexByX(x);
    const hovered = this.chartPoints[index] ?? null;
    if (!hovered) {
      this.hoveredPoint = null;
      return;
    }

    this.hoveredPoint = hovered;
    this.hoverX = this.getPointX(index);
    this.hoverY = this.getPointY(hovered.price);
  }

  onChartMouseLeave(): void {
    this.hoveredPoint = null;
  }

  get buildLineCoords(): string[] {
    if (this.lineRenderedPoints.length < 2) return [];
    const min = this.minPrice, max = this.maxPrice;
    const span = Math.max(max - min, 0.0001);
    const usableW = this.getXAxisPlotWidth();
    const usableH = this.chartHeight - this.chartPaddingTop - this.chartPaddingBottom;
    
    return this.lineRenderedPoints.map((p, i) => {
      const x = this.chartPaddingLeft + (usableW * i) / (this.lineRenderedPoints.length - 1);
      const y = this.chartPaddingTop + usableH - ((p.price - min) / span) * usableH;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    });
  }

  get chartPath(): string {
    const coords = this.buildLineCoords;
    if (coords.length < 2) return '';
    return coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c}`).join(' ');
  }

  get candlesticks(): CandleStick[] {
    if (!this.points.length) {
      return [];
    }

    const usableW = this.getXAxisPlotWidth();
    const slotWidth = this.points.length > 1 ? usableW / (this.points.length - 1) : usableW;
    const candleWidth = Math.max(1, Math.min(16, slotWidth * 0.58));

    return this.points.map((point, index) => {
      const previous = this.points[index - 1]?.price ?? point.price;
      const next = this.points[index + 1]?.price ?? point.price;
      const open = previous;
      const close = point.price;
      const high = Math.max(open, close, next);
      const low = Math.min(open, close, next);
      const x = this.getPointX(index);
      const openY = this.getPointY(open);
      const closeY = this.getPointY(close);
      const highY = this.getPointY(high);
      const lowY = this.getPointY(low);
      const bodyTop = Math.min(openY, closeY);
      const bodyHeight = Math.max(Math.abs(closeY - openY), 2);

      return {
        x,
        wickTop: highY,
        wickBottom: lowY,
        bodyTop,
        bodyHeight,
        width: candleWidth,
        bullish: close >= open
      };
    });
  }

  get chartFillPath(): string {
    const coords = this.buildLineCoords;
    if (coords.length < 2) return '';
    
    const usableH = this.chartHeight - this.chartPaddingTop - this.chartPaddingBottom;
    const baseY = this.chartPaddingTop + usableH;
    
    let d = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c}`).join(' ');
    
    // Close the path at the bottom
    const firstX = coords[0].split(',')[0];
    const lastX = coords[coords.length - 1].split(',')[0];
    d += ` L${lastX},${baseY} L${firstX},${baseY} Z`;
    
    return d;
  }

  private getNearestIndexByX(xInViewBox: number): number {
    if (this.chartPoints.length <= 1) {
      return 0;
    }

    const usableW = this.getXAxisPlotWidth();
    const normalized = (xInViewBox - this.chartPaddingLeft) / Math.max(usableW, 1);
    const index = Math.round(normalized * (this.chartPoints.length - 1));
    return Math.max(0, Math.min(this.chartPoints.length - 1, index));
  }

  private getPointX(index: number): number {
    if (this.chartPoints.length <= 1) {
      return this.chartPaddingLeft;
    }

    const usableW = this.getXAxisPlotWidth();
    return this.chartPaddingLeft + (usableW * index) / (this.chartPoints.length - 1);
  }

  private getXAxisPlotWidth(): number {
    return Math.max(this.chartWidth - this.chartPaddingLeft - this.chartPaddingRight, 0);
  }

  private getPointY(price: number): number {
    const span = Math.max(this.maxPrice - this.minPrice, 0.0001);
    const usableH = this.chartHeight - this.chartPaddingTop - this.chartPaddingBottom;
    return this.chartPaddingTop + usableH - ((price - this.minPrice) / span) * usableH;
  }
}
