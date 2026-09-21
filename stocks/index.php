<?php

declare(strict_types=1);

require_once __DIR__ . '/layout.php';

$periods = getPeriods();
$selectedPeriod = selectedPeriod();

$stocksResponse = listStocksDirect();
$stocks = $stocksResponse['ok'] ? normalizeStockList($stocksResponse['data']) : [];
$stocksError = $stocksResponse['ok'] ? '' : (string) $stocksResponse['error'];

$requestedSymbol = trim((string) ($_GET['symbol'] ?? ''));
$selectedStock = findStockBySymbol($stocks, $requestedSymbol);
if ($selectedStock === null && $stocks !== []) {
    $selectedStock = $stocks[0];
}

$historyError = '';
$points = [];
$history = null;
if ($selectedStock !== null) {
    $historyResponse = getStockHistoryDirect($selectedStock['symbol'], $selectedPeriod);
    if ($historyResponse['ok']) {
        $history = is_array($historyResponse['data']) ? $historyResponse['data'] : null;
        $points = normalizePoints($history['points'] ?? []);
    } else {
        $historyError = (string) $historyResponse['error'];
    }
}

$chart = buildChartModel($points, $selectedPeriod);

renderPageStart('Akcie');
?>
<div class="stocks-root">
    <aside class="stocks-list-panel">
        <div class="panel-title">US akcie (NASDAQ, DJIA, S&amp;P 500)</div>
        <div class="panel-subtitle">Celkem titulů: <strong><?= count($stocks); ?></strong></div>

        <?php if ($stocksError !== ''): ?>
            <div class="panel-state error"><?= h($stocksError); ?></div>
        <?php elseif ($stocks === []): ?>
            <div class="panel-state">Seznam akcií je prázdný.</div>
        <?php else: ?>
            <?php foreach ($stocks as $stock): ?>
                <?php
                $isActive = $selectedStock !== null && $selectedStock['symbol'] === $stock['symbol'];
                $url = '?' . http_build_query([
                    'symbol' => $stock['symbol'],
                    'period' => $selectedPeriod,
                ]);
                ?>
                <a class="stock-item<?= $isActive ? ' active' : ''; ?>" href="<?= h($url); ?>">
                    <div class="stock-symbol"><?= h($stock['symbol']); ?></div>
                    <div class="stock-name"><?= h($stock['name']); ?></div>
                </a>
            <?php endforeach; ?>
        <?php endif; ?>
    </aside>

    <section class="stocks-chart-panel">
        <?php if ($selectedStock === null && $stocksError === ''): ?>
            <div class="panel-state">Vyber akcii vlevo.</div>
        <?php endif; ?>

        <?php if ($historyError !== ''): ?>
            <div class="panel-state error">Nepodařilo se načíst graf akcie. <?= h($historyError); ?></div>
        <?php endif; ?>

        <?php if ($selectedStock !== null): ?>
            <div class="chart-box">
                <div class="chart-header">
                    <div>
                        <h2><?= h($selectedStock['name']); ?> (<?= h($selectedStock['symbol']); ?>)</h2>
                        <small>Vývoj ceny</small>
                    </div>

                    <div class="period-tabs">
                        <?php foreach ($periods as $periodKey => $periodLabel): ?>
                            <?php $url = '?' . http_build_query(['symbol' => $selectedStock['symbol'], 'period' => $periodKey]); ?>
                            <a class="period-tab<?= $selectedPeriod === $periodKey ? ' active' : ''; ?>" href="<?= h($url); ?>">
                                <?= h($periodLabel); ?>
                            </a>
                        <?php endforeach; ?>
                    </div>
                </div>

                <?php if ($points !== []): ?>
                    <div class="stats-row">
                        <div class="stat-card">
                            <span>Min</span>
                            <strong><?= h(formatPrice($chart['minPrice'])); ?></strong>
                        </div>
                        <div class="stat-card">
                            <span>Max</span>
                            <strong><?= h(formatPrice($chart['maxPrice'])); ?></strong>
                        </div>
                        <div class="stat-card">
                            <span>Poslední</span>
                            <strong><?= h(formatPrice($chart['lastPrice'])); ?></strong>
                        </div>
                        <div class="stat-card <?= $chart['changePercent'] >= 0 ? 'positive' : 'negative'; ?>">
                            <span>Změna</span>
                            <strong><?= h(formatPercent($chart['changePercent'])); ?>%</strong>
                        </div>
                    </div>

                    <div class="chart-with-axes">
                        <div class="y-axis-panel">
                            <div class="y-axis-label">Cena (USD)</div>
                        </div>

                        <div class="chart-area">
                            <div class="chart-interactive-wrap" id="chartHost">
                                <svg
                                    id="priceChart"
                                    viewBox="0 0 <?= (int) $chart['width']; ?> <?= (int) $chart['height']; ?>"
                                    class="price-chart"
                                    preserveAspectRatio="none"
                                    aria-label="Graf ceny akcie"
                                >
                                    <defs>
                                        <linearGradient id="lineGradient" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="0%" stop-color="#4f7cff" stop-opacity="0.85"></stop>
                                            <stop offset="100%" stop-color="#4f7cff" stop-opacity="0.25"></stop>
                                        </linearGradient>
                                    </defs>

                                    <g>
                                        <line
                                            x1="<?= (int) $chart['padding']; ?>"
                                            y1="<?= (int) $chart['padding']; ?>"
                                            x2="<?= (int) $chart['padding']; ?>"
                                            y2="<?= (int) $chart['plotEndY']; ?>"
                                            class="chart-axis-line"
                                        ></line>

                                        <line
                                            x1="<?= (int) $chart['padding']; ?>"
                                            y1="<?= (int) $chart['plotEndY']; ?>"
                                            x2="<?= (int) $chart['plotEndX']; ?>"
                                            y2="<?= (int) $chart['plotEndY']; ?>"
                                            class="chart-axis-line"
                                        ></line>

                                        <?php foreach ($chart['yAxisTicks'] as $tick): ?>
                                            <text
                                                x="<?= (int) $chart['padding'] - 10; ?>"
                                                y="<?= round((float) $tick['y'] + 4, 2); ?>"
                                                text-anchor="end"
                                                class="chart-axis-text"
                                            ><?= h((string) $tick['label']); ?></text>
                                            <line
                                                x1="<?= (int) $chart['padding']; ?>"
                                                y1="<?= round((float) $tick['y'], 2); ?>"
                                                x2="<?= (int) $chart['plotEndX']; ?>"
                                                y2="<?= round((float) $tick['y'], 2); ?>"
                                                class="chart-grid-line"
                                            ></line>
                                            <line
                                                x1="<?= (int) $chart['padding'] - 8; ?>"
                                                y1="<?= round((float) $tick['y'], 2); ?>"
                                                x2="<?= (int) $chart['padding']; ?>"
                                                y2="<?= round((float) $tick['y'], 2); ?>"
                                                class="chart-axis-line"
                                            ></line>
                                        <?php endforeach; ?>

                                        <?php foreach ($chart['xAxisTicks'] as $tick): ?>
                                            <line
                                                x1="<?= round((float) $tick['x'], 2); ?>"
                                                y1="<?= (int) $chart['plotEndY']; ?>"
                                                x2="<?= round((float) $tick['x'], 2); ?>"
                                                y2="<?= (int) $chart['plotEndY'] + 6; ?>"
                                                class="chart-axis-line"
                                            ></line>
                                            <text
                                                x="<?= round((float) $tick['x'], 2); ?>"
                                                y="<?= (int) $chart['plotEndY'] + 22; ?>"
                                                text-anchor="middle"
                                                class="chart-axis-text chart-axis-text-x"
                                            ><?= h((string) $tick['label']); ?></text>
                                        <?php endforeach; ?>
                                    </g>

                                    <?php if ($chart['path'] !== ''): ?>
                                        <path d="<?= h($chart['path']); ?>" fill="none" stroke="#2f5bff" stroke-width="3"></path>
                                    <?php endif; ?>

                                    <line id="hoverLine" class="hover-line" x1="0" x2="0" y1="0" y2="0" hidden></line>
                                    <circle id="hoverPoint" class="hover-point" cx="0" cy="0" r="5" hidden></circle>
                                </svg>

                                <div class="chart-tooltip" id="chartTooltip" hidden>
                                    <div class="tooltip-time" id="tooltipTime"></div>
                                    <div class="tooltip-price" id="tooltipPrice"></div>
                                </div>
                            </div>

                            <div class="x-axis-label">Datum / čas</div>
                        </div>
                    </div>

                    <div class="footer-meta">
                        <span>První bod: <strong><?= h(formatDateTime((int) $points[0]['timestamp'])); ?></strong></span>
                        <span>Poslední bod: <strong><?= h(formatDateTime((int) $points[array_key_last($points)]['timestamp'])); ?></strong></span>
                        <span>Počet bodů: <strong><?= count($points); ?></strong></span>
                    </div>
                <?php elseif ($historyError === ''): ?>
                    <div class="panel-state">Pro tuto akcii nejsou dostupná data.</div>
                <?php endif; ?>
            </div>
        <?php endif; ?>
    </section>
</div>

<?php if ($points !== []): ?>
    <script>
        (function () {
            const chart = document.getElementById('priceChart');
            const tooltip = document.getElementById('chartTooltip');
            const tooltipTime = document.getElementById('tooltipTime');
            const tooltipPrice = document.getElementById('tooltipPrice');
            const hoverLine = document.getElementById('hoverLine');
            const hoverPoint = document.getElementById('hoverPoint');
            const host = document.getElementById('chartHost');
            const chartWidth = <?= (int) $chart['width']; ?>;
            const chartHeight = <?= (int) $chart['height']; ?>;
            const padding = <?= (int) $chart['padding']; ?>;
            const plotEndY = <?= (int) $chart['plotEndY']; ?>;
            const points = <?= json_encode($chart['pointViews'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_AMP | JSON_HEX_QUOT); ?>;

            if (!chart || !tooltip || !tooltipTime || !tooltipPrice || !hoverLine || !hoverPoint || !host || !Array.isArray(points) || points.length === 0) {
                return;
            }

            function setHidden(hidden) {
                tooltip.hidden = hidden;
                hoverLine.hidden = hidden;
                hoverPoint.hidden = hidden;
            }

            function getNearestIndex(xInViewBox) {
                if (points.length <= 1) {
                    return 0;
                }

                const usableWidth = chartWidth - (padding * 2);
                const normalized = (xInViewBox - padding) / Math.max(usableWidth, 1);
                const index = Math.round(normalized * (points.length - 1));
                return Math.max(0, Math.min(points.length - 1, index));
            }

            chart.addEventListener('mousemove', function (event) {
                const rect = chart.getBoundingClientRect();
                if (!rect.width || !rect.height) {
                    return;
                }

                const xInViewBox = ((event.clientX - rect.left) / rect.width) * chartWidth;
                const point = points[getNearestIndex(xInViewBox)];
                if (!point) {
                    setHidden(true);
                    return;
                }

                hoverLine.setAttribute('x1', point.x);
                hoverLine.setAttribute('x2', point.x);
                hoverLine.setAttribute('y1', padding);
                hoverLine.setAttribute('y2', plotEndY);
                hoverPoint.setAttribute('cx', point.x);
                hoverPoint.setAttribute('cy', point.y);
                tooltipTime.textContent = point.labelTime;
                tooltipPrice.textContent = point.labelPrice;

                const leftPercent = (point.x / chartWidth) * 100;
                tooltip.style.left = leftPercent + '%';
                tooltip.style.top = '8px';
                tooltip.classList.toggle('align-right', leftPercent > 72);
                setHidden(false);
            });

            host.addEventListener('mouseleave', function () {
                setHidden(true);
            });
        }());
    </script>
<?php endif; ?>
<?php
renderPageEnd();
