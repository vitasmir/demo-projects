<?php

declare(strict_types=1);

setlocale(LC_TIME, 'cs_CZ.UTF-8', 'cs_CZ.utf8', 'cs_CZ');

$config = require __DIR__ . '/config.php';

function appConfig(): array
{
    global $config;

    return $config;
}

function h(?string $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8');
}

function pageTitle(string $title): string
{
    $appName = appConfig()['app_name'] ?? 'Aplikace';
    return $title === '' ? $appName : $title . ' | ' . $appName;
}

function extractHttpStatusCode(array $headers): int
{
    foreach ($headers as $headerLine) {
        if (preg_match('#HTTP/\S+\s+(\d{3})#', $headerLine, $matches) === 1) {
            return (int) $matches[1];
        }
    }

    return 0;
}

function fetchRemoteJson(string $url): array
{
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => 15,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json',
                'User-Agent: PHP Stocks Client/1.0',
            ],
        ]);

        $body = curl_exec($ch);
        $curlError = curl_error($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);

        if ($body === false) {
            return [
                'ok' => false,
                'status' => 0,
                'data' => null,
                'error' => $curlError !== '' ? $curlError : 'Nepodařilo se načíst vzdálená data.',
            ];
        }
    } else {
        $context = stream_context_create([
            'http' => [
                'method' => 'GET',
                'timeout' => 15,
                'header' => "Accept: application/json\r\nUser-Agent: PHP Stocks Client/1.0\r\n",
                'ignore_errors' => true,
            ],
        ]);

        $body = @file_get_contents($url, false, $context);
        $status = isset($http_response_header) && is_array($http_response_header)
            ? extractHttpStatusCode($http_response_header)
            : 0;

        if ($body === false) {
            return [
                'ok' => false,
                'status' => $status,
                'data' => null,
                'error' => 'Nepodařilo se načíst vzdálená data.',
            ];
        }
    }

    $decoded = json_decode($body, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        return [
            'ok' => false,
            'status' => $status,
            'data' => null,
            'error' => 'Poskytovatel dat nevrátil validní JSON odpověď.',
        ];
    }

    if ($status < 200 || $status >= 300) {
        $message = is_array($decoded) && isset($decoded['message']) && is_string($decoded['message'])
            ? $decoded['message']
            : 'Poskytovatel dat vrátil chybu ' . $status . '.';

        return [
            'ok' => false,
            'status' => $status,
            'data' => $decoded,
            'error' => $message,
        ];
    }

    return [
        'ok' => true,
        'status' => $status,
        'data' => $decoded,
        'error' => '',
    ];
}

function getPeriods(): array
{
    return appConfig()['periods'] ?? [];
}

function selectedPeriod(): string
{
    $periods = getPeriods();
    $selected = (string) ($_GET['period'] ?? '1d');

    return array_key_exists($selected, $periods) ? $selected : '1d';
}

function getStockCatalog(): array
{
    static $catalog = null;

    if (is_array($catalog)) {
        return $catalog;
    }

    $catalogPath = __DIR__ . '/catalog.json';
    if (!is_file($catalogPath)) {
        $catalog = [];
        return $catalog;
    }

    $json = file_get_contents($catalogPath);
    if ($json === false) {
        $catalog = [];
        return $catalog;
    }

    $decoded = json_decode($json, true);
    $catalog = normalizeStockList($decoded);

    return $catalog;
}

function listStocksDirect(): array
{
    return [
        'ok' => true,
        'status' => 200,
        'data' => getStockCatalog(),
        'error' => '',
    ];
}

function getYahooPeriodSpec(string $period): ?array
{
    return match ($period) {
        '1h' => ['range' => '1d', 'interval' => '1m'],
        '1d' => ['range' => '1d', 'interval' => '1m'],
        '3m' => ['range' => '3mo', 'interval' => '1h'],
        '1y' => ['range' => '1y', 'interval' => '1d'],
        'max' => ['range' => 'max', 'interval' => '1wk'],
        default => null,
    };
}

function getStockHistoryDirect(string $symbol, string $period): array
{
    $normalizedSymbol = strtoupper(trim($symbol));
    $normalizedPeriod = strtolower(trim($period));

    if ($normalizedSymbol === '') {
        return [
            'ok' => false,
            'status' => 400,
            'data' => null,
            'error' => 'Symbol akcie je povinný.',
        ];
    }

    $spec = getYahooPeriodSpec($normalizedPeriod);
    if ($spec === null) {
        return [
            'ok' => false,
            'status' => 400,
            'data' => null,
            'error' => 'Nepodporovaná perioda.',
        ];
    }

    $url = sprintf(
        'https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=%s',
        rawurlencode($normalizedSymbol),
        rawurlencode($spec['range']),
        rawurlencode($spec['interval'])
    );

    $response = fetchRemoteJson($url);
    if (!$response['ok']) {
        return $response;
    }

    $payload = is_array($response['data']) ? $response['data'] : [];
    $result = $payload['chart']['result'][0] ?? null;
    if (!is_array($result)) {
        return [
            'ok' => false,
            'status' => 502,
            'data' => null,
            'error' => 'Poskytovatel dat nevrátil použitelná data.',
        ];
    }

    $timestamps = $result['timestamp'] ?? [];
    $close = $result['indicators']['quote'][0]['close'] ?? [];
    if (!is_array($timestamps) || !is_array($close)) {
        return [
            'ok' => false,
            'status' => 502,
            'data' => null,
            'error' => 'Poskytovatel dat vrátil neplatný formát historie.',
        ];
    }

    $len = min(count($timestamps), count($close));
    $points = [];
    for ($i = 0; $i < $len; $i++) {
        $timestamp = $timestamps[$i] ?? null;
        $price = $close[$i] ?? null;

        if (!is_numeric($timestamp) || !is_numeric($price)) {
            continue;
        }

        $points[] = [
            'timestamp' => ((int) $timestamp) * 1000,
            'price' => (float) $price,
        ];
    }

    if ($normalizedPeriod === '1h' && count($points) > 60) {
        $points = array_slice($points, -60);
    }

    $points = normalizePoints($points);

    return [
        'ok' => true,
        'status' => 200,
        'data' => [
            'symbol' => $normalizedSymbol,
            'period' => $normalizedPeriod,
            'points' => $points,
        ],
        'error' => '',
    ];
}

function normalizeStockList(mixed $stocks): array
{
    if (!is_array($stocks)) {
        return [];
    }

    $normalized = [];
    foreach ($stocks as $stock) {
        if (!is_array($stock)) {
            continue;
        }

        $symbol = trim((string) ($stock['symbol'] ?? ''));
        $name = trim((string) ($stock['name'] ?? ''));
        if ($symbol === '' || $name === '') {
            continue;
        }

        $normalized[] = [
            'symbol' => $symbol,
            'name' => $name,
        ];
    }

    return $normalized;
}

function normalizePoints(mixed $points): array
{
    if (!is_array($points)) {
        return [];
    }

    $normalized = [];
    foreach ($points as $point) {
        if (!is_array($point)) {
            continue;
        }

        $timestamp = (int) ($point['timestamp'] ?? 0);
        $price = (float) ($point['price'] ?? 0);
        if ($timestamp <= 0 || !is_finite($price)) {
            continue;
        }

        $normalized[] = [
            'timestamp' => $timestamp,
            'price' => $price,
        ];
    }

    usort($normalized, static fn (array $a, array $b): int => $a['timestamp'] <=> $b['timestamp']);

    return $normalized;
}

function findStockBySymbol(array $stocks, string $symbol): ?array
{
    foreach ($stocks as $stock) {
        if (($stock['symbol'] ?? '') === $symbol) {
            return $stock;
        }
    }

    return null;
}

function formatPrice(float $value): string
{
    return is_finite($value) ? number_format($value, 2, '.', '') : '-';
}

function formatPercent(float $value): string
{
    return number_format($value, 2, '.', '');
}

function formatDateTime(int $timestamp): string
{
    if ($timestamp <= 0) {
        return '-';
    }

    if (class_exists('IntlDateFormatter')) {
        $formatter = new IntlDateFormatter(
            'cs_CZ',
            IntlDateFormatter::MEDIUM,
            IntlDateFormatter::MEDIUM,
            'Europe/Prague',
            IntlDateFormatter::GREGORIAN,
            'dd.MM.yyyy HH:mm:ss'
        );
        return $formatter->format((int) floor($timestamp / 1000));
    }

    return date('d.m.Y H:i:s', (int) floor($timestamp / 1000));
}

function formatAxisDate(int $timestamp, string $period): string
{
    if ($timestamp <= 0) {
        return '-';
    }

    $dateTime = new DateTimeImmutable('@' . (string) floor($timestamp / 1000));
    $dateTime = $dateTime->setTimezone(new DateTimeZone('Europe/Prague'));

    return match ($period) {
        '1d' => $dateTime->format('H:i'),
        '1h' => $dateTime->format('H:i'),
        default => $dateTime->format('d.m.Y'),
    };
}

function formatShortDate(int $timestamp, string $period): string
{
    if ($timestamp <= 0) {
        return '-';
    }

    $dateTime = new DateTimeImmutable('@' . (string) floor($timestamp / 1000));
    $dateTime = $dateTime->setTimezone(new DateTimeZone('Europe/Prague'));

    return $period === '1d'
        ? $dateTime->format('H:i:s')
        : $dateTime->format('Y-m-d H:i');
}

function formatAxisPrice(float $value): string
{
    if (!is_finite($value)) {
        return '-';
    }

    $abs = abs($value);
    if ($abs >= 100) {
        $text = number_format($value, 2, '.', '');
    } elseif ($abs >= 1) {
        $text = number_format($value, 3, '.', '');
    } else {
        $text = number_format($value, 4, '.', '');
    }

    $text = preg_replace('/\.0+$/', '', $text) ?? $text;
    $text = preg_replace('/(\.\d*?)0+$/', '$1', $text) ?? $text;

    return $text . ' USD';
}

function getNiceStep(float $rawStep): float
{
    if (!is_finite($rawStep) || $rawStep <= 0) {
        return 1.0;
    }

    $exponent = floor(log10($rawStep));
    $fraction = $rawStep / (10 ** $exponent);

    if ($fraction <= 1) {
        $niceFraction = 1;
    } elseif ($fraction <= 2) {
        $niceFraction = 2;
    } elseif ($fraction <= 5) {
        $niceFraction = 5;
    } else {
        $niceFraction = 10;
    }

    return $niceFraction * (10 ** $exponent);
}

function getXAxisTickTarget(int $usableWidth, string $period, int $total): int
{
    $minSpacing = match ($period) {
        '1h' => 76,
        '1d' => 72,
        '3m' => 84,
        '1y' => 84,
        'max' => 88,
        default => 84,
    };

    $target = (int) floor($usableWidth / max($minSpacing, 1));
    $target = max(6, $target);

    return min($target, $total, 14);
}

function getTimeAxisStepSeconds(string $period): ?int
{
    return match ($period) {
        '1h' => 5 * 60,
        '1d' => 15 * 60,
        default => null,
    };
}

function buildTimeXAxisTicks(array $points, array $pointViews, string $period): array
{
    $stepSeconds = getTimeAxisStepSeconds($period);
    if ($stepSeconds === null || count($points) < 2 || count($pointViews) < 2) {
        return [];
    }

    $firstTimestamp = (int) $points[0]['timestamp'];
    $lastTimestamp = (int) $points[array_key_last($points)]['timestamp'];
    if ($firstTimestamp <= 0 || $lastTimestamp <= $firstTimestamp) {
        return [];
    }

    $startSeconds = (int) floor($firstTimestamp / 1000);
    $endSeconds = (int) floor($lastTimestamp / 1000);
    $firstTickSeconds = (int) ceil($startSeconds / $stepSeconds) * $stepSeconds;

    $ticks = [];
    for ($tickSeconds = $firstTickSeconds; $tickSeconds <= $endSeconds; $tickSeconds += $stepSeconds) {
        $nearestIndex = 0;
        $nearestDiff = PHP_INT_MAX;

        foreach ($points as $index => $point) {
            $diff = abs((int) $point['timestamp'] - ($tickSeconds * 1000));
            if ($diff < $nearestDiff) {
                $nearestDiff = $diff;
                $nearestIndex = $index;
            }
        }

        $view = $pointViews[$nearestIndex] ?? null;
        if ($view === null) {
            continue;
        }

        $ticks[] = [
            'leftPercent' => 0,
            'label' => formatAxisDate($tickSeconds * 1000, $period),
            'x' => (float) $view['x'],
        ];
    }

    return $ticks;
}

function buildChartModel(array $points, string $period): array
{
    $chartConfig = appConfig()['chart'] ?? [];
    $width = (int) ($chartConfig['width'] ?? 980);
    $height = (int) ($chartConfig['height'] ?? 520);
    $padding = (int) ($chartConfig['padding'] ?? 72);
    $usableWidth = max($width - ($padding * 2), 1);
    $usableHeight = max($height - ($padding * 2), 1);

    if ($points === []) {
        return [
            'width' => $width,
            'height' => $height,
            'padding' => $padding,
            'plotEndX' => $width - $padding,
            'plotEndY' => $height - $padding,
            'path' => '',
            'pointViews' => [],
            'yAxisTicks' => [],
            'xAxisTicks' => [],
            'minPrice' => 0.0,
            'maxPrice' => 0.0,
            'firstPrice' => 0.0,
            'lastPrice' => 0.0,
            'changePercent' => 0.0,
        ];
    }

    $prices = array_column($points, 'price');
    $minPrice = (float) min($prices);
    $maxPrice = (float) max($prices);
    $span = max($maxPrice - $minPrice, 0.0001);

    $pointViews = [];
    $pathParts = [];
    $lastIndex = max(count($points) - 1, 1);

    foreach ($points as $index => $point) {
        $x = $padding + ($usableWidth * $index) / $lastIndex;
        $y = $padding + $usableHeight - ((($point['price'] - $minPrice) / $span) * $usableHeight);

        $pathParts[] = sprintf('%s%.2f,%.2f', $index === 0 ? 'M' : 'L', $x, $y);
        $pointViews[] = [
            'index' => $index,
            'x' => round($x, 2),
            'y' => round($y, 2),
            'timestamp' => (int) $point['timestamp'],
            'price' => (float) $point['price'],
            'labelTime' => formatShortDate((int) $point['timestamp'], $period),
            'labelPrice' => formatPrice((float) $point['price']) . ' USD',
        ];
    }

    $yAxisTicks = [];
    if ($minPrice === $maxPrice) {
        $yAxisTicks[] = [
            'topPercent' => 50,
            'label' => formatAxisPrice($maxPrice),
            'y' => $padding + ($usableHeight * 0.5),
        ];
    } else {
        $targetTickCount = 6;
        $range = $maxPrice - $minPrice;
        $rawStep = $range / ($targetTickCount - 1);
        $step = getNiceStep($rawStep);
        $niceMin = floor($minPrice / $step) * $step;
        $niceMax = ceil($maxPrice / $step) * $step;
        $tickRange = max($niceMax - $niceMin, $step);

        for ($value = $niceMax; $value >= $niceMin - ($step / 2); $value -= $step) {
            $topPercent = (($niceMax - $value) / $tickRange) * 100;
            $yAxisTicks[] = [
                'topPercent' => $topPercent,
                'label' => formatAxisPrice((float) $value),
                'y' => $padding + ($usableHeight * ($topPercent / 100)),
            ];
        }
    }

    $total = count($points);
    $xAxisTicks = [];
    if ($total === 1) {
        $xAxisTicks[] = [
            'leftPercent' => 0,
            'label' => formatAxisDate((int) $points[0]['timestamp'], $period),
            'x' => $padding,
        ];
    } else {
        $xAxisTicks = buildTimeXAxisTicks($points, $pointViews, $period);

        if ($xAxisTicks === []) {
            $desired = getXAxisTickTarget($usableWidth, $period, $total);
            $indices = [];
            for ($i = 0; $i < $desired; $i++) {
                $index = (int) round(($i * ($total - 1)) / max($desired - 1, 1));
                $indices[$index] = true;
            }

            foreach (array_keys($indices) as $index) {
                $leftPercent = ($index / ($total - 1)) * 100;
                $xAxisTicks[] = [
                    'leftPercent' => $leftPercent,
                    'label' => formatAxisDate((int) $points[$index]['timestamp'], $period),
                    'x' => $padding + ($usableWidth * ($leftPercent / 100)),
                ];
            }
        }
    }

    $firstPrice = (float) $points[0]['price'];
    $lastPrice = (float) $points[array_key_last($points)]['price'];
    $changePercent = $firstPrice !== 0.0 ? (($lastPrice - $firstPrice) / $firstPrice) * 100 : 0.0;

    return [
        'width' => $width,
        'height' => $height,
        'padding' => $padding,
        'plotEndX' => $width - $padding,
        'plotEndY' => $height - $padding,
        'path' => count($pointViews) >= 2 ? implode(' ', $pathParts) : '',
        'pointViews' => $pointViews,
        'yAxisTicks' => $yAxisTicks,
        'xAxisTicks' => $xAxisTicks,
        'minPrice' => $minPrice,
        'maxPrice' => $maxPrice,
        'firstPrice' => $firstPrice,
        'lastPrice' => $lastPrice,
        'changePercent' => $changePercent,
    ];
}
