import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';

interface StockItem {
  symbol: string;
  name: string;
}

interface StockPricePoint {
  timestamp: number;
  price: number;
}

interface StockHistoryResponse {
  symbol: string;
  period: string;
  points: StockPricePoint[];
}

type PeriodKey = '1h' | '1d' | '3m' | '1y' | 'max';

interface XAxisTick {
  leftPercent: number;
  label: string;
}

interface YAxisTick {
  topPercent: number;
  label: string;
}

@Component({
  selector: 'app-stocks',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stocks.component.html',
  styleUrls: ['./stocks.component.scss']
})
export class StocksComponent implements OnInit, AfterViewInit, OnDestroy {
  public stocks: StockItem[] = [];
  public selectedStock: StockItem | null = null;

  public selectedPeriod: PeriodKey = '1d';
  public readonly periods: Array<{ key: PeriodKey; label: string }> = [
    { key: '1h', label: '1 hodina' },
    { key: '1d', label: '1 den' },
    { key: '3m', label: 'Čtvrtletí' },
    { key: '1y', label: '1 rok' },
    { key: 'max', label: 'Celá doba' }
  ];

  public points: StockPricePoint[] = [];
  public loadingStocks = false;
  public loadingChart = false;
  public errorMessage = '';
  public hoveredPoint: StockPricePoint | null = null;
  public hoverX = 0;
  public hoverY = 0;
  public chartWidth = 980;
  public chartHeight = 520;

  private resizeObserver?: ResizeObserver;
  private chartHostRef?: ElementRef<HTMLDivElement>;

  public readonly chartPadding = 72;

  @ViewChild('chartHost')
  set chartHost(value: ElementRef<HTMLDivElement> | undefined) {
    this.chartHostRef = value;
    this.observeChartSize();
    if (value) {
      requestAnimationFrame(() => this.updateChartSize());
    }
  }

  constructor(
    private readonly http: HttpClient,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStocks();
  }

  ngAfterViewInit(): void {
    this.observeChartSize();
    requestAnimationFrame(() => this.updateChartSize());
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  loadStocks(): void {
    this.loadingStocks = true;
    this.errorMessage = '';

    this.http.get<StockItem[]>('/api/stocks/nasdaq').subscribe({
      next: (items) => {
        this.stocks = Array.isArray(items) ? items : [];
        this.loadingStocks = false;

        if (this.stocks.length > 0) {
          this.selectStock(this.stocks[0]);
        } else {
          this.selectedStock = null;
          this.points = [];
        }

        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingStocks = false;
        this.errorMessage = 'Nepodařilo se načíst seznam akcií.';
        this.cdr.detectChanges();
      }
    });
  }

  selectStock(stock: StockItem): void {
    if (!stock) {
      return;
    }

    this.selectedStock = stock;
    this.loadHistory();
  }

  setPeriod(period: PeriodKey): void {
    if (this.selectedPeriod === period) {
      return;
    }

    this.selectedPeriod = period;
    this.loadHistory();
  }

  get chartPath(): string {
    if (this.points.length < 2) {
      return '';
    }

    const min = Math.min(...this.points.map((p) => p.price));
    const max = Math.max(...this.points.map((p) => p.price));
    const span = Math.max(max - min, 0.0001);

    const usableW = this.chartWidth - this.chartPadding * 2;
    const usableH = this.chartHeight - this.chartPadding * 2;

    return this.points
      .map((p, i) => {
        const x = this.chartPadding + (usableW * i) / (this.points.length - 1);
        const y = this.chartPadding + usableH - ((p.price - min) / span) * usableH;
        return `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

  get hoverXPercent(): number {
    if (!this.chartWidth) {
      return 0;
    }
    return (this.hoverX / this.chartWidth) * 100;
  }

  get hoverYPercent(): number {
    if (!this.chartHeight) {
      return 0;
    }
    return (this.hoverY / this.chartHeight) * 100;
  }

  get tooltipRightAligned(): boolean {
    return this.hoverXPercent > 72;
  }

  get yAxisTicks(): YAxisTick[] {
    if (!this.points.length) {
      return [];
    }

    const min = this.minPrice;
    const max = this.maxPrice;
    if (!Number.isFinite(min) || !Number.isFinite(max)) {
      return [];
    }

    if (min === max) {
      return [{ topPercent: 50, label: this.formatAxisPrice(max) }];
    }

    const targetTickCount = 6;
    const range = max - min;
    const rawStep = range / (targetTickCount - 1);
    const step = this.getNiceStep(rawStep);

    const niceMin = Math.floor(min / step) * step;
    const niceMax = Math.ceil(max / step) * step;

    const ticks: YAxisTick[] = [];
    const tickRange = Math.max(niceMax - niceMin, step);
    for (let value = niceMax; value >= niceMin - step / 2; value -= step) {
      ticks.push({
        topPercent: ((niceMax - value) / tickRange) * 100,
        label: this.formatAxisPrice(value)
      });
    }

    return ticks;
  }

  get xAxisTicks(): XAxisTick[] {
    const total = this.points.length;
    if (!total) {
      return [];
    }

    if (total === 1) {
      return [{ leftPercent: 0, label: this.formatAxisDate(this.points[0].timestamp) }];
    }

    const usableW = this.chartWidth - this.chartPadding * 2;
    const spacing = this.selectedPeriod === '1h'
      ? 55
      : this.selectedPeriod === '1d'
        ? 65
        : this.selectedPeriod === '3m'
          ? 80
          : 100;

    const preferredTickCount = Math.max(2, Math.min(total, Math.floor(usableW / spacing)));
    const periodCap = this.selectedPeriod === '1h'
      ? 24
      : this.selectedPeriod === '1d'
        ? 18
        : this.selectedPeriod === '3m'
          ? 12
          : 10;
    const desired = Math.min(preferredTickCount, periodCap, total);

    const indices = new Set<number>();
    for (let i = 0; i < desired; i++) {
      const idx = Math.round((i * (total - 1)) / (desired - 1));
      indices.add(idx);
    }

    return [...indices]
      .sort((a, b) => a - b)
      .map((idx) => ({
        leftPercent: (idx / (total - 1)) * 100,
        label: this.formatAxisDate(this.points[idx].timestamp)
      }));
  }

  get minPrice(): number {
    return this.points.length ? Math.min(...this.points.map((p) => p.price)) : 0;
  }

  get maxPrice(): number {
    return this.points.length ? Math.max(...this.points.map((p) => p.price)) : 0;
  }

  get firstPrice(): number {
    return this.points.length ? this.points[0].price : 0;
  }

  get lastPrice(): number {
    return this.points.length ? this.points[this.points.length - 1].price : 0;
  }

  get changePercent(): number {
    const first = this.firstPrice;
    if (!first) {
      return 0;
    }
    return ((this.lastPrice - first) / first) * 100;
  }

  get plotEndX(): number {
    return this.chartWidth - this.chartPadding;
  }

  get plotEndY(): number {
    return this.chartHeight - this.chartPadding;
  }

  formatPrice(value: number): string {
    return Number.isFinite(value) ? value.toFixed(2) : '-';
  }

  formatDate(ts: number): string {
    if (!ts) {
      return '-';
    }
    const date = new Date(ts);
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

  formatAxisDate(ts: number): string {
    if (!ts) {
      return '-';
    }

    const date = new Date(ts);
    if (this.selectedPeriod === '1d') {
      return date.toLocaleTimeString('cs-CZ', {
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    if (this.selectedPeriod === '1h') {
      const datePart = date.toLocaleDateString('cs-CZ', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
      const timePart = date.toLocaleTimeString('cs-CZ', {
        hour: '2-digit',
        minute: '2-digit'
      });
      return `${datePart} ${timePart}`;
    }

    return date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatShortDate(ts: number): string {
    if (!ts) {
      return '-';
    }

    const date = new Date(ts);
    if (this.selectedPeriod === '1d') {
      return date.toLocaleTimeString('cs-CZ', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    }

    const datePart = date.toLocaleDateString('cs-CZ', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    const timePart = date.toLocaleTimeString('cs-CZ', {
      hour: '2-digit',
      minute: '2-digit'
    });
    return `${datePart} ${timePart}`;
  }

  trackStock(_index: number, stock: StockItem): string {
    return stock.symbol;
  }

  onChartMouseMove(event: MouseEvent, svg: Element): void {
    if (!(svg instanceof SVGSVGElement)) {
      return;
    }

    if (!this.points.length) {
      return;
    }

    const rect = svg.getBoundingClientRect();
    if (!rect.width || !rect.height) {
      return;
    }

    const xInViewBox = ((event.clientX - rect.left) / rect.width) * this.chartWidth;
    const nearestIndex = this.getNearestIndexByX(xInViewBox);
    const point = this.points[nearestIndex] ?? null;
    if (!point) {
      this.hoveredPoint = null;
      return;
    }

    this.hoveredPoint = point;
    this.hoverX = this.getPointX(nearestIndex);
    this.hoverY = this.getPointY(point.price);
  }

  onChartMouseLeave(): void {
    this.hoveredPoint = null;
  }

  private loadHistory(): void {
    if (!this.selectedStock) {
      return;
    }

    this.loadingChart = true;
    this.errorMessage = '';

    const symbol = encodeURIComponent(this.selectedStock.symbol);
    this.http.get<StockHistoryResponse>(`/api/stocks/${symbol}/history?period=${this.selectedPeriod}`).subscribe({
      next: (response) => {
        this.points = Array.isArray(response?.points) ? response.points : [];
        this.hoveredPoint = null;
        this.loadingChart = false;
        this.cdr.detectChanges();
        requestAnimationFrame(() => this.updateChartSize());
      },
      error: () => {
        this.points = [];
        this.hoveredPoint = null;
        this.loadingChart = false;
        this.errorMessage = 'Nepodařilo se načíst graf akcie.';
        this.cdr.detectChanges();
      }
    });
  }

  private observeChartSize(): void {
    if (!this.chartHostRef?.nativeElement || typeof ResizeObserver === 'undefined') {
      return;
    }

    this.resizeObserver?.disconnect();
    this.resizeObserver = new ResizeObserver(() => this.updateChartSize());
    this.resizeObserver.observe(this.chartHostRef.nativeElement);
  }

  private updateChartSize(): void {
    const host = this.chartHostRef?.nativeElement;
    if (!host) {
      return;
    }

    const { clientWidth, clientHeight } = host;
    if (!clientWidth || !clientHeight) {
      return;
    }

    this.chartWidth = Math.max(clientWidth, 320);
    this.chartHeight = Math.max(clientHeight, 320);
    this.cdr.detectChanges();
  }

  private getNearestIndexByX(xInViewBox: number): number {
    if (this.points.length <= 1) {
      return 0;
    }

    const usableW = this.chartWidth - this.chartPadding * 2;
    const normalized = (xInViewBox - this.chartPadding) / Math.max(usableW, 1);
    const index = Math.round(normalized * (this.points.length - 1));
    return Math.max(0, Math.min(this.points.length - 1, index));
  }

  private getPointX(index: number): number {
    if (this.points.length <= 1) {
      return this.chartPadding;
    }
    const usableW = this.chartWidth - this.chartPadding * 2;
    return this.chartPadding + (usableW * index) / (this.points.length - 1);
  }

  private getPointY(price: number): number {
    const min = this.minPrice;
    const max = this.maxPrice;
    const span = Math.max(max - min, 0.0001);
    const usableH = this.chartHeight - this.chartPadding * 2;
    return this.chartPadding + usableH - ((price - min) / span) * usableH;
  }

  public getYAxisPosition(topPercent: number): number {
    const usableH = this.chartHeight - this.chartPadding * 2;
    return this.chartPadding + usableH * (topPercent / 100);
  }

  public getXAxisPosition(leftPercent: number): number {
    const usableW = this.chartWidth - this.chartPadding * 2;
    return this.chartPadding + usableW * (leftPercent / 100);
  }

  private formatAxisPrice(value: number): string {
    if (!Number.isFinite(value)) {
      return '-';
    }

    const abs = Math.abs(value);
    let text: string;

    if (abs >= 100) {
      text = value.toFixed(2);
    } else if (abs >= 1) {
      text = value.toFixed(3);
    } else {
      text = value.toFixed(4);
    }

    text = text
      .replace(/\.0+$/, '')
      .replace(/(\.\d*?)0+$/, '$1');

    return `${text} USD`;
  }

  private getNiceStep(rawStep: number): number {
    if (!Number.isFinite(rawStep) || rawStep <= 0) {
      return 1;
    }

    const exponent = Math.floor(Math.log10(rawStep));
    const fraction = rawStep / Math.pow(10, exponent);

    let niceFraction = 1;
    if (fraction <= 1) {
      niceFraction = 1;
    } else if (fraction <= 2) {
      niceFraction = 2;
    } else if (fraction <= 5) {
      niceFraction = 5;
    } else {
      niceFraction = 10;
    }

    return niceFraction * Math.pow(10, exponent);
  }
}
