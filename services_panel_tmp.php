<?php

declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

function db(): PDO
{
    static $pdo;

    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $cfg = db_config();
    $dsn = sprintf(
        'mysql:host=%s;port=%d;dbname=%s;charset=%s',
        $cfg['host'],
        $cfg['port'],
        $cfg['database'],
        $cfg['charset']
    );

    try {
        $pdo = new PDO($dsn, $cfg['username'], $cfg['password'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
    } catch (Throwable $exception) {
        $message = 'Database connection failed. Import database/schema.sql and confirm config/database.php matches your MySQL setup.';
        if (str_contains($_SERVER['SCRIPT_NAME'] ?? '', '/api/')) {
            json_response(['success' => false, 'message' => $message], 500);
        }

        http_response_code(500);
        echo '<!doctype html><html><head><meta charset="utf-8"><title>Database Setup Needed</title><style>body{font-family:Segoe UI,sans-serif;background:#f4f7fb;padding:40px;color:#11243a}main{max-width:720px;margin:0 auto;background:#fff;border-radius:24px;padding:32px;box-shadow:0 25px 60px rgba(16,32,56,.12)}a{color:#2f6bff}</style></head><body><main><h1>Database setup needed</h1><p>' . e($message) . '</p><p>Expected database: <strong>' . e((string) $cfg['database']) . '</strong></p><p>Schema file: <a href="' . e(base_url('database/schema.sql')) . '">' . e(base_path('database/schema.sql')) . '</a></p></main></body></html>';
        exit;
    }

    return $pdo;
}

function ensure_runtime_schema(): void
{
    static $initialized = false;
    if ($initialized) {
        return;
    }

    execute_query(
        'CREATE TABLE IF NOT EXISTS otp_requests (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            user_role VARCHAR(30) NOT NULL,
            purpose VARCHAR(30) NOT NULL,
            phone VARCHAR(40) NOT NULL,
            channel VARCHAR(30) NOT NULL DEFAULT "sms",
            provider VARCHAR(60) NOT NULL DEFAULT "demo",
            code_hash VARCHAR(255) NOT NULL,
            payload_json LONGTEXT DEFAULT NULL,
            delivery_response LONGTEXT DEFAULT NULL,
            status VARCHAR(30) NOT NULL DEFAULT "pending",
            expires_at DATETIME NOT NULL,
            verified_at DATETIME DEFAULT NULL,
            consumed_at DATETIME DEFAULT NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            INDEX idx_otp_lookup (user_role, purpose, phone, status),
            INDEX idx_otp_expiry (expires_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    execute_query(
        'CREATE TABLE IF NOT EXISTS offers (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR(190) NOT NULL,
            description TEXT DEFAULT NULL,
            badge_label VARCHAR(80) DEFAULT NULL,
            discount_label VARCHAR(80) DEFAULT NULL,
            image_url VARCHAR(255) DEFAULT NULL,
            supplier_id INT UNSIGNED DEFAULT NULL,
            catalog_product_id INT UNSIGNED DEFAULT NULL,
            city VARCHAR(120) DEFAULT NULL,
            status VARCHAR(30) NOT NULL DEFAULT "active",
            starts_at DATETIME DEFAULT NULL,
            ends_at DATETIME DEFAULT NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            INDEX idx_offer_status (status),
            INDEX idx_offer_city (city)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    execute_query(
        'CREATE TABLE IF NOT EXISTS app_notifications (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR(190) NOT NULL,
            message TEXT NOT NULL,
            target_type VARCHAR(30) NOT NULL DEFAULT "all",
            target_value VARCHAR(120) DEFAULT NULL,
            link_type VARCHAR(40) DEFAULT NULL,
            link_value VARCHAR(190) DEFAULT NULL,
            status VARCHAR(30) NOT NULL DEFAULT "active",
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            INDEX idx_notification_target (target_type, target_value),
            INDEX idx_notification_status (status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    execute_query(
        'CREATE TABLE IF NOT EXISTS buyer_devices (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            buyer_id INT UNSIGNED NOT NULL,
            firebase_token VARCHAR(255) NOT NULL,
            platform VARCHAR(40) NOT NULL DEFAULT "android",
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            UNIQUE KEY uniq_buyer_device (buyer_id, firebase_token),
            INDEX idx_device_token (firebase_token)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    execute_query(
        'CREATE TABLE IF NOT EXISTS buyer_referral_codes (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            buyer_id INT UNSIGNED NOT NULL,
            referral_code VARCHAR(40) NOT NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            UNIQUE KEY uniq_buyer_referral_buyer (buyer_id),
            UNIQUE KEY uniq_buyer_referral_code (referral_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    execute_query(
        'CREATE TABLE IF NOT EXISTS buyer_referral_claims (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            referrer_buyer_id INT UNSIGNED NOT NULL,
            referred_buyer_id INT UNSIGNED NOT NULL,
            referral_code VARCHAR(40) NOT NULL,
            used_by_phone VARCHAR(40) DEFAULT NULL,
            reward_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
            referee_reward_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
            status VARCHAR(30) NOT NULL DEFAULT "completed",
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            UNIQUE KEY uniq_referred_buyer (referred_buyer_id),
            INDEX idx_referral_code (referral_code),
            INDEX idx_referrer_buyer (referrer_buyer_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );

    ensure_settings_exist([
        'support_whatsapp' => [
            'value' => setting_value('support_phone', '+92 300 7000000'),
            'group' => 'public',
            'label' => 'Support WhatsApp number',
        ],
        'support_whatsapp_message' => [
            'value' => 'Hello Muhalli support, I need help with my buyer account.',
            'group' => 'public',
            'label' => 'Support WhatsApp default message',
        ],
        'map_default_city' => [
            'value' => 'Karachi',
            'group' => 'public',
            'label' => 'Default map city',
        ],
        'referral_enabled' => [
            'value' => '1',
            'group' => 'public',
            'label' => 'Referral program enabled',
        ],
        'referral_reward_amount' => [
            'value' => '20',
            'group' => 'public',
            'label' => 'Referrer reward amount',
        ],
        'referral_referee_reward_amount' => [
            'value' => '10',
            'group' => 'public',
            'label' => 'Referred buyer reward amount',
        ],
        'otp_provider' => [
            'value' => 'brqsms',
            'group' => 'system',
            'label' => 'OTP provider',
        ],
        'otp_api_url' => [
            'value' => 'https://dash.brqsms.com/api/http/sms/send',
            'group' => 'system',
            'label' => 'OTP API URL',
        ],
        'otp_api_token' => [
            'value' => '',
            'group' => 'system',
            'label' => 'OTP API token',
        ],
        'otp_delivery_channel' => [
            'value' => 'sms',
            'group' => 'system',
            'label' => 'OTP delivery channel',
        ],
        'otp_sender_id' => [
            'value' => 'Muhalli',
            'group' => 'system',
            'label' => 'OTP sender ID',
        ],
        'otp_expiry_minutes' => [
            'value' => '10',
            'group' => 'system',
            'label' => 'OTP expiry minutes',
        ],
        'otp_message_template' => [
            'value' => 'Your Muhalli verification code is {{CODE}}. It expires in {{MINUTES}} minutes.',
            'group' => 'system',
            'label' => 'OTP message template',
        ],
    ]);

    $initialized = true;
}

function fetch_all(string $sql, array $params = []): array
{
    $stmt = db()->prepare($sql);
    $stmt->execute($params);
    return $stmt->fetchAll();
}

function fetch_one(string $sql, array $params = []): ?array
{
    $stmt = db()->prepare($sql);
    $stmt->execute($params);
    $row = $stmt->fetch();
    return $row ?: null;
}

function execute_query(string $sql, array $params = []): bool
{
    $stmt = db()->prepare($sql);
    return $stmt->execute($params);
}

function persist_row(string $table, array $data, ?int $id = null): int
{
    $pdo = db();
    $columns = array_keys($data);

    if ($id === null) {
        $placeholders = implode(', ', array_map(fn ($column) => ':' . $column, $columns));
        $sql = sprintf('INSERT INTO %s (%s) VALUES (%s)', $table, implode(', ', $columns), $placeholders);
        $stmt = $pdo->prepare($sql);
        $stmt->execute($data);
        return (int) $pdo->lastInsertId();
    }

    $assignments = implode(', ', array_map(fn ($column) => $column . ' = :' . $column, $columns));
    $data['id'] = $id;
    $sql = sprintf('UPDATE %s SET %s WHERE id = :id', $table, $assignments);
    $stmt = $pdo->prepare($sql);
    $stmt->execute($data);
    return $id;
}

function delete_row(string $table, int $id): void
{
    execute_query(sprintf('DELETE FROM %s WHERE id = :id', $table), ['id' => $id]);
}

function find_admin_by_email(string $email): ?array
{
    return fetch_one('SELECT * FROM admins WHERE email = :email LIMIT 1', ['email' => $email]);
}

function admin_profile(int $adminId): ?array
{
    return fetch_one('SELECT * FROM admins WHERE id = :id LIMIT 1', ['id' => $adminId]);
}

function category_options(): array
{
    return fetch_all('SELECT id, name FROM categories WHERE status != "archived" ORDER BY sort_order ASC, name ASC');
}

function supplier_options(): array
{
    return fetch_all('SELECT id, business_name, owner_name, status FROM suppliers ORDER BY business_name ASC');
}

function buyer_options(): array
{
    return fetch_all('SELECT id, store_name, buyer_name, status FROM buyers ORDER BY store_name ASC');
}

function catalog_options(): array
{
    return fetch_all('SELECT id, name, packaging, unit_type FROM catalog_products WHERE status = "active" ORDER BY name ASC');
}

function dashboard_metrics(): array
{
    $counts = [
        'buyers' => (int) fetch_one('SELECT COUNT(*) AS total FROM buyers WHERE status = "active"')['total'],
        'suppliers' => (int) fetch_one('SELECT COUNT(*) AS total FROM suppliers WHERE status = "active"')['total'],
        'products' => (int) fetch_one('SELECT COUNT(*) AS total FROM supplier_products WHERE status = "active"')['total'],
        'orders' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders')['total'],
        'revenue' => (float) fetch_one('SELECT COALESCE(SUM(total_amount), 0) AS total FROM orders WHERE status != "cancelled"')['total'],
        'pending_suppliers' => (int) fetch_one('SELECT COUNT(*) AS total FROM suppliers WHERE status = "pending"')['total'],
    ];

    $recentOrders = fetch_all(
        'SELECT o.id, o.order_number, o.status, o.total_amount, o.order_date, b.store_name, s.business_name
         FROM orders o
         JOIN buyers b ON b.id = o.buyer_id
         JOIN suppliers s ON s.id = o.supplier_id
         ORDER BY o.order_date DESC, o.id DESC
         LIMIT 6'
    );

    $lowStock = fetch_all(
        'SELECT sp.id, cp.name AS product_name, sp.stock_quantity, sp.min_order_qty, s.business_name
         FROM supplier_products sp
         JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         LEFT JOIN suppliers s ON s.id = sp.supplier_id
         WHERE sp.stock_quantity <= 10
         ORDER BY sp.stock_quantity ASC, cp.name ASC
         LIMIT 6'
    );

    $categoryMix = fetch_all(
        'SELECT c.name,
                COUNT(DISTINCT cp.id) AS catalog_count,
                COUNT(DISTINCT sp.id) AS total_listings
         FROM categories c
         LEFT JOIN catalog_products cp ON cp.category_id = c.id
         LEFT JOIN supplier_products sp ON sp.catalog_product_id = cp.id
         GROUP BY c.id, c.name
         ORDER BY total_listings DESC, c.name ASC
         LIMIT 6'
    );

    $monthlyRows = fetch_all(
        'SELECT DATE_FORMAT(order_date, "%Y-%m") AS period, COALESCE(SUM(total_amount), 0) AS revenue
         FROM orders
         WHERE status != "cancelled"
         GROUP BY DATE_FORMAT(order_date, "%Y-%m")
         ORDER BY period ASC'
    );

    return compact('counts', 'recentOrders', 'lowStock', 'categoryMix', 'monthlyRows');
}

function all_categories(string $search = ''): array
{
    $sql = 'SELECT c.*,
                   COUNT(DISTINCT cp.id) AS catalog_count,
                   COUNT(DISTINCT sp.id) AS listing_count
            FROM categories c
            LEFT JOIN catalog_products cp ON cp.category_id = c.id
            LEFT JOIN supplier_products sp ON sp.catalog_product_id = cp.id
            WHERE (:search = "" OR c.name LIKE :like OR c.description LIKE :like)
            GROUP BY c.id
            ORDER BY c.sort_order ASC, c.name ASC';

    return fetch_all($sql, ['search' => $search, 'like' => '%' . $search . '%']);
}

function find_category(int $id): ?array
{
    return fetch_one('SELECT * FROM categories WHERE id = :id LIMIT 1', ['id' => $id]);
}

function all_suppliers(string $search = '', string $status = '', array $filters = []): array
{
    $city = trim((string) array_get($filters, 'city', ''));
    $sort = strtolower(trim((string) array_get($filters, 'sort', 'default')));
    $allowedSorts = ['default', 'cheapest', 'low_min_order'];
    if (!in_array($sort, $allowedSorts, true)) {
        $sort = 'default';
    }

    $orderBy = match ($sort) {
        'cheapest' => 'COALESCE(lowest_price, 99999999) ASC, s.minimum_order_amount ASC, s.business_name ASC',
        'low_min_order' => 's.minimum_order_amount ASC, COALESCE(lowest_price, 99999999) ASC, s.business_name ASC',
        default => 's.is_verified DESC, FIELD(s.status, "pending", "active", "suspended"), s.business_name ASC',
    };

    $sql = 'SELECT s.*,
                   (SELECT COUNT(*) FROM supplier_products sp WHERE sp.supplier_id = s.id) AS product_count,
                   (SELECT COUNT(*) FROM orders o WHERE o.supplier_id = s.id) AS order_count,
                   (SELECT COALESCE(SUM(CASE WHEN o.status != "cancelled" THEN o.total_amount ELSE 0 END), 0)
                    FROM orders o
                    WHERE o.supplier_id = s.id) AS revenue_total,
                   (SELECT MIN(sp.price)
                    FROM supplier_products sp
                    WHERE sp.supplier_id = s.id
                      AND sp.status = "active") AS lowest_price
            FROM suppliers s
            WHERE (:search = ""
                OR s.business_name LIKE :like
                OR s.owner_name LIKE :like
                OR s.email LIKE :like
                OR s.city LIKE :like
                OR EXISTS (
                    SELECT 1
                    FROM supplier_products sp2
                    JOIN catalog_products cp2 ON cp2.id = sp2.catalog_product_id
                    LEFT JOIN categories c2 ON c2.id = cp2.category_id
                    WHERE sp2.supplier_id = s.id
                      AND (
                        cp2.name LIKE :like
                        OR cp2.packaging LIKE :like
                        OR c2.name LIKE :like
                      )
                )
            )
              AND (:status = "" OR s.status = :status)
              AND (:city = "" OR s.city = :city)
            ORDER BY ' . $orderBy;

    return fetch_all($sql, [
        'search' => $search,
        'like' => '%' . $search . '%',
        'status' => $status,
        'city' => $city,
    ]);
}

function find_supplier(int $id): ?array
{
    $supplier = fetch_one('SELECT * FROM suppliers WHERE id = :id LIMIT 1', ['id' => $id]);
    if (!$supplier) {
        return null;
    }

    $supplier['products'] = fetch_all(
        'SELECT sp.*, cp.name AS product_name, cp.packaging, cp.unit_type, c.name AS category_name
         FROM supplier_products sp
         JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         LEFT JOIN categories c ON c.id = cp.category_id
         WHERE sp.supplier_id = :supplier_id
         ORDER BY cp.name ASC',
        ['supplier_id' => $id]
    );

    $supplier['recent_orders'] = fetch_all(
        'SELECT order_number, status, total_amount, order_date
         FROM orders
         WHERE supplier_id = :supplier_id
         ORDER BY order_date DESC
         LIMIT 5',
        ['supplier_id' => $id]
    );

    return $supplier;
}

function all_buyers(string $search = '', string $status = ''): array
{
    $sql = 'SELECT b.*,
                   COUNT(DISTINCT o.id) AS order_count,
                   COALESCE(SUM(CASE WHEN o.status != "cancelled" THEN o.total_amount ELSE 0 END), 0) AS spend_total
            FROM buyers b
            LEFT JOIN orders o ON o.buyer_id = b.id
            WHERE (:search = "" OR b.store_name LIKE :like OR b.buyer_name LIKE :like OR b.email LIKE :like OR b.city LIKE :like)
              AND (:status = "" OR b.status = :status)
            GROUP BY b.id
            ORDER BY b.store_name ASC';

    return fetch_all($sql, ['search' => $search, 'like' => '%' . $search . '%', 'status' => $status]);
}

function find_buyer(int $id): ?array
{
    $buyer = fetch_one('SELECT * FROM buyers WHERE id = :id LIMIT 1', ['id' => $id]);
    if (!$buyer) {
        return null;
    }

    $buyer['orders'] = fetch_all(
        'SELECT o.order_number, o.status, o.total_amount, o.order_date, s.business_name
         FROM orders o
         JOIN suppliers s ON s.id = o.supplier_id
         WHERE o.buyer_id = :buyer_id
         ORDER BY o.order_date DESC
         LIMIT 6',
        ['buyer_id' => $id]
    );

    $buyer['threads'] = fetch_all(
        'SELECT t.subject, t.last_message, t.last_message_at, s.business_name
         FROM chat_threads t
         JOIN suppliers s ON s.id = t.supplier_id
         WHERE t.buyer_id = :buyer_id
         ORDER BY t.last_message_at DESC
         LIMIT 5',
        ['buyer_id' => $id]
    );

    return $buyer;
}

function all_product_listings(array $filters = []): array
{
    $search = (string) array_get($filters, 'search', '');
    $status = (string) array_get($filters, 'status', '');
    $supplierId = (int) array_get($filters, 'supplier_id', 0);
    $categoryId = (int) array_get($filters, 'category_id', 0);
    $city = trim((string) array_get($filters, 'city', ''));
    $sort = strtolower(trim((string) array_get($filters, 'sort', 'default')));
    $allowedSorts = ['default', 'cheapest', 'low_min_order'];
    if (!in_array($sort, $allowedSorts, true)) {
        $sort = 'default';
    }

    $orderBy = match ($sort) {
        'cheapest' => 'sp.price ASC, s.minimum_order_amount ASC, cp.name ASC',
        'low_min_order' => 's.minimum_order_amount ASC, sp.price ASC, cp.name ASC',
        default => 'sp.is_featured DESC, sp.created_at DESC, sp.id DESC',
    };

    $sql = 'SELECT sp.*,
                   cp.name AS catalog_name,
                   cp.emoji,
                   cp.packaging,
                   cp.unit_type,
                   cp.description,
                   cp.image_url,
                   c.name AS category_name,
                   s.business_name AS supplier_name,
                   s.city AS supplier_city,
                   s.minimum_order_amount AS supplier_minimum_order_amount,
                   s.minimum_order_quantity AS supplier_minimum_order_quantity
            FROM supplier_products sp
            JOIN catalog_products cp ON cp.id = sp.catalog_product_id
            LEFT JOIN categories c ON c.id = cp.category_id
            LEFT JOIN suppliers s ON s.id = sp.supplier_id
            WHERE (:search = ""
                OR cp.name LIKE :like
                OR cp.packaging LIKE :like
                OR s.business_name LIKE :like
                OR c.name LIKE :like
                OR s.city LIKE :like)
              AND (:status = "" OR sp.status = :status)
              AND (:supplier_id = 0 OR sp.supplier_id = :supplier_id)
              AND (:category_id = 0 OR cp.category_id = :category_id)
              AND (:city = "" OR s.city = :city)
            ORDER BY ' . $orderBy;

    return fetch_all($sql, [
        'search' => $search,
        'like' => '%' . $search . '%',
        'status' => $status,
        'supplier_id' => $supplierId,
        'category_id' => $categoryId,
        'city' => $city,
    ]);
}

function find_listing(int $id): ?array
{
    return fetch_one(
        'SELECT sp.*, cp.id AS catalog_id, cp.name AS catalog_name, cp.slug, cp.emoji, cp.description, cp.packaging, cp.unit_type, cp.image_url, cp.category_id,
                s.business_name AS supplier_name, c.name AS category_name
         FROM supplier_products sp
         JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         LEFT JOIN suppliers s ON s.id = sp.supplier_id
         LEFT JOIN categories c ON c.id = cp.category_id
         WHERE sp.id = :id LIMIT 1',
        ['id' => $id]
    );
}

function all_orders(array $filters = []): array
{
    $search = (string) array_get($filters, 'search', '');
    $status = (string) array_get($filters, 'status', '');

    $sql = 'SELECT o.*,
                   b.store_name,
                   b.buyer_name,
                   s.business_name,
                   COUNT(oi.id) AS item_count
            FROM orders o
            JOIN buyers b ON b.id = o.buyer_id
            JOIN suppliers s ON s.id = o.supplier_id
            LEFT JOIN order_items oi ON oi.order_id = o.id
            WHERE (:search = "" OR o.order_number LIKE :like OR b.store_name LIKE :like OR s.business_name LIKE :like)
              AND (:status = "" OR o.status = :status)
            GROUP BY o.id
            ORDER BY o.order_date DESC, o.id DESC';

    return fetch_all($sql, ['search' => $search, 'like' => '%' . $search . '%', 'status' => $status]);
}

function order_summary_cards(): array
{
    return [
        'all' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders')['total'],
        'pending' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders WHERE status = "pending"')['total'],
        'processing' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders WHERE status = "processing"')['total'],
        'shipped' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders WHERE status = "shipped"')['total'],
        'delivered' => (int) fetch_one('SELECT COUNT(*) AS total FROM orders WHERE status = "delivered"')['total'],
    ];
}

function find_order(int $id): ?array
{
    $order = fetch_one(
        'SELECT o.*, b.store_name, b.buyer_name, b.phone AS buyer_phone, b.city AS buyer_city, b.address AS buyer_address,
                s.business_name, s.owner_name, s.phone AS supplier_phone
         FROM orders o
         JOIN buyers b ON b.id = o.buyer_id
         JOIN suppliers s ON s.id = o.supplier_id
         WHERE o.id = :id LIMIT 1',
        ['id' => $id]
    );

    if (!$order) {
        return null;
    }

    $order['items'] = fetch_all(
        'SELECT oi.*,
                sp.sku,
                COALESCE(NULLIF(oi.product_name, ""), cp.name, "Product item") AS product_name,
                COALESCE(NULLIF(oi.unit_label, ""), cp.unit_type, "") AS unit_label
         FROM order_items oi
         LEFT JOIN supplier_products sp ON sp.id = oi.supplier_product_id
         LEFT JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         WHERE oi.order_id = :order_id
         ORDER BY oi.id ASC',
        ['order_id' => $id]
    );

    return $order;
}

function all_threads(string $search = ''): array
{
    return fetch_all(
        'SELECT t.*, b.store_name, b.buyer_name, s.business_name, s.owner_name,
                COUNT(m.id) AS message_count
         FROM chat_threads t
         JOIN buyers b ON b.id = t.buyer_id
         JOIN suppliers s ON s.id = t.supplier_id
         LEFT JOIN chat_messages m ON m.thread_id = t.id
         WHERE (:search = "" OR b.store_name LIKE :like OR s.business_name LIKE :like OR t.subject LIKE :like OR t.last_message LIKE :like)
         GROUP BY t.id
         ORDER BY t.last_message_at DESC, t.id DESC',
        ['search' => $search, 'like' => '%' . $search . '%']
    );
}

function find_thread(int $id): ?array
{
    $thread = fetch_one(
        'SELECT t.*, b.store_name, b.buyer_name, s.business_name, s.owner_name
         FROM chat_threads t
         JOIN buyers b ON b.id = t.buyer_id
         JOIN suppliers s ON s.id = t.supplier_id
         WHERE t.id = :id LIMIT 1',
        ['id' => $id]
    );

    if (!$thread) {
        return null;
    }

    $thread['messages'] = fetch_all(
        'SELECT * FROM chat_messages WHERE thread_id = :thread_id ORDER BY created_at ASC, id ASC',
        ['thread_id' => $id]
    );

    return $thread;
}

function settings_map(?string $group = null): array
{
    $rows = $group
        ? fetch_all('SELECT * FROM settings WHERE setting_group = :grp ORDER BY id ASC', ['grp' => $group])
        : fetch_all('SELECT * FROM settings ORDER BY setting_group ASC, id ASC');

    $map = [];
    foreach ($rows as $row) {
        $map[$row['setting_key']] = $row['setting_value'];
    }

    return $map;
}

function settings_rows(?string $group = null): array
{
    return $group
        ? fetch_all('SELECT * FROM settings WHERE setting_group = :grp ORDER BY id ASC', ['grp' => $group])
        : fetch_all('SELECT * FROM settings ORDER BY setting_group ASC, id ASC');
}

function update_setting_value(string $key, string $value): void
{
    execute_query('UPDATE settings SET setting_value = :value, updated_at = NOW() WHERE setting_key = :setting_key', [
        'value' => $value,
        'setting_key' => $key,
    ]);
}

function setting_value(string $key, string $default = ''): string
{
    $row = fetch_one('SELECT setting_value FROM settings WHERE setting_key = :setting_key LIMIT 1', [
        'setting_key' => $key,
    ]);

    if (!$row) {
        return $default;
    }

    return (string) ($row['setting_value'] ?? $default);
}

function ensure_settings_exist(array $defaults): void
{
    foreach ($defaults as $settingKey => $definition) {
        $existing = fetch_one('SELECT id FROM settings WHERE setting_key = :setting_key LIMIT 1', [
            'setting_key' => $settingKey,
        ]);
        if ($existing) {
            continue;
        }

        persist_row('settings', [
            'setting_key' => $settingKey,
            'setting_value' => (string) array_get($definition, 'value', ''),
            'setting_group' => (string) array_get($definition, 'group', 'system'),
            'label' => (string) array_get($definition, 'label', $settingKey),
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s'),
        ]);
    }
}

function public_config_payload(): array
{
    return settings_map('public');
}

function all_offers(array $filters = []): array
{
    $search = trim((string) array_get($filters, 'search', ''));
    $status = trim((string) array_get($filters, 'status', ''));
    $city = trim((string) array_get($filters, 'city', ''));

    return fetch_all(
        'SELECT o.*,
                s.business_name AS supplier_name,
                cp.name AS product_name
         FROM offers o
         LEFT JOIN suppliers s ON s.id = o.supplier_id
         LEFT JOIN catalog_products cp ON cp.id = o.catalog_product_id
         WHERE (:search = ""
                OR o.title LIKE :like
                OR o.description LIKE :like
                OR s.business_name LIKE :like
                OR cp.name LIKE :like)
           AND (:status = "" OR o.status = :status)
           AND (:city = "" OR o.city = :city)
         ORDER BY FIELD(o.status, "active", "draft", "expired"), COALESCE(o.starts_at, o.created_at) DESC, o.id DESC',
        [
            'search' => $search,
            'like' => '%' . $search . '%',
            'status' => $status,
            'city' => $city,
        ]
    );
}

function find_offer(int $id): ?array
{
    return fetch_one(
        'SELECT o.*,
                s.business_name AS supplier_name,
                cp.name AS product_name
         FROM offers o
         LEFT JOIN suppliers s ON s.id = o.supplier_id
         LEFT JOIN catalog_products cp ON cp.id = o.catalog_product_id
         WHERE o.id = :id
         LIMIT 1',
        ['id' => $id]
    );
}

function all_admin_notifications(array $filters = []): array
{
    $search = trim((string) array_get($filters, 'search', ''));
    $status = trim((string) array_get($filters, 'status', ''));

    return fetch_all(
        'SELECT n.*
         FROM app_notifications n
         WHERE (:search = "" OR n.title LIKE :like OR n.message LIKE :like OR n.target_value LIKE :like)
           AND (:status = "" OR n.status = :status)
         ORDER BY n.created_at DESC, n.id DESC',
        [
            'search' => $search,
            'like' => '%' . $search . '%',
            'status' => $status,
        ]
    );
}

function find_admin_notification(int $id): ?array
{
    return fetch_one('SELECT * FROM app_notifications WHERE id = :id LIMIT 1', ['id' => $id]);
}

function referral_settings_rows(): array
{
    return array_filter(
        settings_rows('public'),
        static fn (array $row): bool => str_starts_with((string) $row['setting_key'], 'referral_')
    );
}

function referral_overview(): array
{
    return [
        'settings' => referral_settings_rows(),
        'codes' => fetch_all(
            'SELECT rc.referral_code, b.store_name, b.buyer_name, b.city, rc.updated_at
             FROM buyer_referral_codes rc
             JOIN buyers b ON b.id = rc.buyer_id
             ORDER BY rc.updated_at DESC, rc.id DESC
             LIMIT 100'
        ),
        'claims' => fetch_all(
            'SELECT c.*, rb.store_name AS referrer_store_name, nb.store_name AS referred_store_name
             FROM buyer_referral_claims c
             JOIN buyers rb ON rb.id = c.referrer_buyer_id
             JOIN buyers nb ON nb.id = c.referred_buyer_id
             ORDER BY c.created_at DESC, c.id DESC
             LIMIT 100'
        ),
    ];
}

function active_offers_payload(string $city = ''): array
{
    return fetch_all(
        'SELECT o.*,
                s.business_name AS supplier_name,
                cp.name AS product_name
         FROM offers o
         LEFT JOIN suppliers s ON s.id = o.supplier_id
         LEFT JOIN catalog_products cp ON cp.id = o.catalog_product_id
         WHERE o.status = "active"
           AND (o.starts_at IS NULL OR o.starts_at <= NOW())
           AND (o.ends_at IS NULL OR o.ends_at >= NOW())
           AND (:city = "" OR o.city IS NULL OR o.city = "" OR o.city = :city)
         ORDER BY COALESCE(o.starts_at, o.created_at) DESC, o.id DESC
         LIMIT 12',
        ['city' => $city]
    );
}

function buyer_notifications_payload(int $buyerId): array
{
    $buyer = find_buyer($buyerId);
    $city = trim((string) ($buyer['city'] ?? ''));

    return fetch_all(
        'SELECT *
         FROM app_notifications
         WHERE status = "active"
           AND (
                target_type = "all"
                OR (target_type = "city" AND target_value = :city)
                OR (target_type = "buyer" AND target_value = :buyer_id)
           )
         ORDER BY created_at DESC, id DESC
         LIMIT 100',
        [
            'city' => $city,
            'buyer_id' => (string) $buyerId,
        ]
    );
}

function register_buyer_device_token(int $buyerId, string $firebaseToken, string $platform = 'android'): void
{
    $firebaseToken = trim($firebaseToken);
    if ($firebaseToken === '') {
        throw new RuntimeException('Firebase token is required.');
    }

    $existing = fetch_one(
        'SELECT id
         FROM buyer_devices
         WHERE buyer_id = :buyer_id
           AND firebase_token = :firebase_token
         LIMIT 1',
        [
            'buyer_id' => $buyerId,
            'firebase_token' => $firebaseToken,
        ]
    );

    persist_row('buyer_devices', [
        'buyer_id' => $buyerId,
        'firebase_token' => $firebaseToken,
        'platform' => $platform,
        'created_at' => date('Y-m-d H:i:s'),
        'updated_at' => date('Y-m-d H:i:s'),
    ], $existing ? (int) $existing['id'] : null);
}

function referral_program_enabled(): bool
{
    return setting_value('referral_enabled', '1') === '1';
}

function referral_reward_amount(): float
{
    return (float) setting_value('referral_reward_amount', '20');
}

function referral_referee_reward_amount(): float
{
    return (float) setting_value('referral_referee_reward_amount', '10');
}

function generate_referral_code_seed(array $buyer): string
{
    $base = strtoupper(substr(preg_replace('/[^A-Z0-9]/', '', strtoupper((string) ($buyer['store_name'] ?? 'BUYER'))) ?: 'BUYER', 0, 4));
    $base = str_pad($base, 4, 'X');
    return 'MUH' . str_pad((string) ((int) ($buyer['id'] ?? 0)), 4, '0', STR_PAD_LEFT) . $base;
}

function ensure_buyer_referral_code(int $buyerId): array
{
    $existing = fetch_one(
        'SELECT *
         FROM buyer_referral_codes
         WHERE buyer_id = :buyer_id
         LIMIT 1',
        ['buyer_id' => $buyerId]
    );
    if ($existing) {
        return $existing;
    }

    $buyer = find_buyer($buyerId);
    if (!$buyer) {
        throw new RuntimeException('Buyer not found.');
    }

    $candidate = generate_referral_code_seed($buyer);
    $counter = 1;
    while (fetch_one(
        'SELECT id FROM buyer_referral_codes WHERE referral_code = :referral_code LIMIT 1',
        ['referral_code' => $candidate]
    )) {
        $candidate = generate_referral_code_seed($buyer) . $counter;
        $counter++;
    }

    $id = persist_row('buyer_referral_codes', [
        'buyer_id' => $buyerId,
        'referral_code' => $candidate,
        'created_at' => date('Y-m-d H:i:s'),
        'updated_at' => date('Y-m-d H:i:s'),
    ]);

    return fetch_one('SELECT * FROM buyer_referral_codes WHERE id = :id LIMIT 1', ['id' => $id]) ?? [];
}

function buyer_referral_summary_payload(int $buyerId): array
{
    $code = ensure_buyer_referral_code($buyerId);
    $stats = fetch_one(
        'SELECT COUNT(*) AS total_claims,
                COALESCE(SUM(reward_amount), 0) AS earned_amount
         FROM buyer_referral_claims
         WHERE referrer_buyer_id = :buyer_id',
        ['buyer_id' => $buyerId]
    ) ?: ['total_claims' => 0, 'earned_amount' => 0];

    return [
        'enabled' => referral_program_enabled(),
        'referral_code' => (string) ($code['referral_code'] ?? ''),
        'reward_amount' => referral_reward_amount(),
        'referee_reward_amount' => referral_referee_reward_amount(),
        'total_claims' => (int) ($stats['total_claims'] ?? 0),
        'earned_amount' => (float) ($stats['earned_amount'] ?? 0),
        'recent_claims' => fetch_all(
            'SELECT c.*, b.store_name AS referred_store_name, b.city AS referred_city
             FROM buyer_referral_claims c
             JOIN buyers b ON b.id = c.referred_buyer_id
             WHERE c.referrer_buyer_id = :buyer_id
             ORDER BY c.created_at DESC, c.id DESC
             LIMIT 20',
            ['buyer_id' => $buyerId]
        ),
    ];
}

function apply_buyer_referral_code(int $buyerId, string $referralCode): array
{
    if (!referral_program_enabled()) {
        throw new RuntimeException('Referral program is currently disabled.');
    }

    $referralCode = strtoupper(trim($referralCode));
    if ($referralCode === '') {
        throw new RuntimeException('Referral code is required.');
    }

    $buyer = find_buyer($buyerId);
    if (!$buyer) {
        throw new RuntimeException('Buyer not found.');
    }

    $referrerCode = fetch_one(
        'SELECT * FROM buyer_referral_codes WHERE referral_code = :referral_code LIMIT 1',
        ['referral_code' => $referralCode]
    );
    if (!$referrerCode) {
        throw new RuntimeException('Referral code not found.');
    }
    if ((int) $referrerCode['buyer_id'] === $buyerId) {
        throw new RuntimeException('You cannot apply your own referral code.');
    }

    $existing = fetch_one(
        'SELECT id
         FROM buyer_referral_claims
         WHERE referred_buyer_id = :referred_buyer_id
         LIMIT 1',
        ['referred_buyer_id' => $buyerId]
    );
    if ($existing) {
        throw new RuntimeException('A referral code was already applied to this buyer account.');
    }

    $claimId = persist_row('buyer_referral_claims', [
        'referrer_buyer_id' => (int) $referrerCode['buyer_id'],
        'referred_buyer_id' => $buyerId,
        'referral_code' => $referralCode,
        'used_by_phone' => normalize_phone_number((string) ($buyer['phone'] ?? '')),
        'reward_amount' => referral_reward_amount(),
        'referee_reward_amount' => referral_referee_reward_amount(),
        'status' => 'completed',
        'created_at' => date('Y-m-d H:i:s'),
        'updated_at' => date('Y-m-d H:i:s'),
    ]);

    return fetch_one(
        'SELECT c.*, rb.store_name AS referrer_store_name
         FROM buyer_referral_claims c
         JOIN buyers rb ON rb.id = c.referrer_buyer_id
         WHERE c.id = :id
         LIMIT 1',
        ['id' => $claimId]
    ) ?? [];
}

function find_buyer_by_email(string $email): ?array
{
    return fetch_one('SELECT * FROM buyers WHERE email = :email LIMIT 1', ['email' => $email]);
}

function find_buyer_by_phone(string $phone): ?array
{
    return fetch_one('SELECT * FROM buyers WHERE phone = :phone LIMIT 1', ['phone' => normalize_phone_number($phone)]);
}

function find_supplier_by_email(string $email): ?array
{
    return fetch_one('SELECT * FROM suppliers WHERE email = :email LIMIT 1', ['email' => $email]);
}

function find_supplier_by_phone(string $phone): ?array
{
    return fetch_one('SELECT * FROM suppliers WHERE phone = :phone LIMIT 1', ['phone' => normalize_phone_number($phone)]);
}

function issue_api_token(string $userType, int $userId): string
{
    $token = bin2hex(random_bytes(32));
    execute_query(
        'INSERT INTO api_tokens (user_type, user_id, token, expires_at, created_at) VALUES (:user_type, :user_id, :token, DATE_ADD(NOW(), INTERVAL 30 DAY), NOW())',
        ['user_type' => $userType, 'user_id' => $userId, 'token' => $token]
    );
    return $token;
}

function buyer_home_payload(): array
{
    $city = trim((string) api_value('city', ''));
    $buyerId = (int) api_value('buyer_id', 0);
    if ($buyerId > 0) {
        $buyer = find_buyer($buyerId);
        $city = trim((string) ($buyer['city'] ?? $city));
    }

    $featuredCategories = fetch_all(
        'SELECT id, name, icon, accent_color, description
         FROM categories
         WHERE status = "active"
         ORDER BY sort_order ASC, name ASC
         LIMIT 8'
    );

    $featuredSuppliers = fetch_all(
        'SELECT id, business_name, owner_name, city, minimum_order_amount, minimum_order_quantity, delivery_time, is_verified, status,
                (SELECT MIN(sp.price) FROM supplier_products sp WHERE sp.supplier_id = suppliers.id AND sp.status = "active") AS lowest_price
         FROM suppliers
         WHERE status = "active"
           AND (:city = "" OR city = :city)
         ORDER BY is_verified DESC, business_name ASC
         LIMIT 8'
        ,
        ['city' => $city]
    );

    $featuredProducts = fetch_all(
        'SELECT sp.id, cp.name, cp.emoji, cp.packaging, cp.unit_type, cp.description, cp.image_url,
                sp.price, sp.stock_quantity, sp.delivery_time, s.business_name, c.name AS category_name,
                s.minimum_order_amount AS supplier_minimum_order_amount, s.city AS supplier_city
         FROM supplier_products sp
         JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         LEFT JOIN suppliers s ON s.id = sp.supplier_id
         LEFT JOIN categories c ON c.id = cp.category_id
         WHERE sp.status = "active"
           AND (:city = "" OR s.city = :city)
         ORDER BY sp.is_featured DESC, sp.created_at DESC
         LIMIT 10'
        ,
        ['city' => $city]
    );

    return [
        'featured_categories' => $featuredCategories,
        'featured_suppliers' => $featuredSuppliers,
        'featured_products' => $featuredProducts,
        'offers' => active_offers_payload($city),
        'public_settings' => public_config_payload(),
    ];
}

function buyer_categories_payload(): array
{
    return all_categories();
}

function buyer_suppliers_payload(array $filters = []): array
{
    return all_suppliers(
        (string) array_get($filters, 'search', ''),
        'active',
        [
            'city' => (string) array_get($filters, 'city', ''),
            'sort' => (string) array_get($filters, 'sort', 'default'),
        ]
    );
}

function buyer_products_payload(array $filters = []): array
{
    $filters['status'] = 'active';
    return all_product_listings($filters);
}

function buyer_orders_payload(int $buyerId): array
{
    return fetch_all(
        'SELECT o.*, s.business_name, COUNT(oi.id) AS item_count
         FROM orders o
         JOIN suppliers s ON s.id = o.supplier_id
         LEFT JOIN order_items oi ON oi.order_id = o.id
         WHERE o.buyer_id = :buyer_id
         GROUP BY o.id
         ORDER BY o.order_date DESC',
        ['buyer_id' => $buyerId]
    );
}

function buyer_chats_payload(int $buyerId): array
{
    return fetch_all(
        'SELECT t.*, s.business_name, s.owner_name
         FROM chat_threads t
         JOIN suppliers s ON s.id = t.supplier_id
         WHERE t.buyer_id = :buyer_id
         ORDER BY t.last_message_at DESC',
        ['buyer_id' => $buyerId]
    );
}

function supplier_dashboard_payload(int $supplierId): array
{
    $stats = fetch_one(
        'SELECT
            COUNT(DISTINCT sp.id) AS total_products,
            SUM(CASE WHEN sp.stock_quantity <= 10 THEN 1 ELSE 0 END) AS low_stock_count,
            COUNT(DISTINCT CASE WHEN o.status = "pending" THEN o.id END) AS pending_orders,
            COUNT(DISTINCT CASE WHEN DATE(o.order_date) = CURDATE() THEN o.id END) AS today_orders,
            COALESCE(SUM(CASE WHEN o.status != "cancelled" AND DATE_FORMAT(o.order_date, "%Y-%m") = DATE_FORMAT(CURDATE(), "%Y-%m") THEN o.total_amount ELSE 0 END), 0) AS month_revenue
         FROM suppliers s
         LEFT JOIN supplier_products sp ON sp.supplier_id = s.id
         LEFT JOIN orders o ON o.supplier_id = s.id
         WHERE s.id = :supplier_id
         GROUP BY s.id',
        ['supplier_id' => $supplierId]
    );

    return [
        'stats' => $stats,
        'recent_orders' => fetch_all(
            'SELECT order_number, status, total_amount, order_date, delivery_date
             FROM orders
             WHERE supplier_id = :supplier_id
             ORDER BY order_date DESC
             LIMIT 5',
            ['supplier_id' => $supplierId]
        ),
        'products' => fetch_all(
            'SELECT sp.id, cp.name, cp.packaging, cp.unit_type, cp.image_url, sp.price, sp.stock_quantity, sp.delivery_time, sp.status
             FROM supplier_products sp
             JOIN catalog_products cp ON cp.id = sp.catalog_product_id
             WHERE sp.supplier_id = :supplier_id
             ORDER BY cp.name ASC',
            ['supplier_id' => $supplierId]
        ),
    ];
}

function supplier_products_payload(int $supplierId): array
{
    return fetch_all(
        'SELECT sp.*, cp.name, cp.packaging, cp.unit_type, cp.emoji, cp.image_url, c.name AS category_name
         FROM supplier_products sp
         JOIN catalog_products cp ON cp.id = sp.catalog_product_id
         LEFT JOIN categories c ON c.id = cp.category_id
         WHERE sp.supplier_id = :supplier_id
         ORDER BY cp.name ASC',
        ['supplier_id' => $supplierId]
    );
}

function supplier_catalog_payload(): array
{
    return fetch_all(
        'SELECT cp.*, c.name AS category_name
         FROM catalog_products cp
         LEFT JOIN categories c ON c.id = cp.category_id
         ORDER BY c.name ASC, cp.name ASC'
    );
}

function supplier_orders_payload(int $supplierId): array
{
    $orders = fetch_all(
        'SELECT o.*, b.store_name, b.buyer_name, b.phone AS buyer_phone, b.city AS buyer_city, b.address AS buyer_address,
                COUNT(oi.id) AS item_count
         FROM orders o
         JOIN buyers b ON b.id = o.buyer_id
         LEFT JOIN order_items oi ON oi.order_id = o.id
         WHERE o.supplier_id = :supplier_id
         GROUP BY o.id
         ORDER BY o.order_date DESC',
        ['supplier_id' => $supplierId]
    );

    foreach ($orders as &$order) {
        $order['delivery_address'] = $order['buyer_address'] ?? '';
        $order['items'] = fetch_all(
            'SELECT oi.supplier_product_id,
                    COALESCE(NULLIF(oi.product_name, ""), cp.name, "Product item") AS product_name,
                    COALESCE(NULLIF(oi.unit_label, ""), cp.unit_type, "") AS unit_label,
                    oi.quantity,
                    oi.unit_price,
                    oi.line_total
             FROM order_items oi
             LEFT JOIN supplier_products sp ON sp.id = oi.supplier_product_id
             LEFT JOIN catalog_products cp ON cp.id = sp.catalog_product_id
             WHERE oi.order_id = :order_id
             ORDER BY id ASC',
            ['order_id' => (int) $order['id']]
        );
    }

    return $orders;
}

function supplier_earnings_payload(int $supplierId): array
{
    $summary = fetch_one(
        'SELECT
            COALESCE(SUM(CASE WHEN status != "cancelled" THEN total_amount ELSE 0 END), 0) AS all_time,
            COALESCE(SUM(CASE WHEN status != "cancelled" AND DATE_FORMAT(order_date, "%Y-%m") = DATE_FORMAT(CURDATE(), "%Y-%m") THEN total_amount ELSE 0 END), 0) AS this_month
         FROM orders
         WHERE supplier_id = :supplier_id',
        ['supplier_id' => $supplierId]
    );

    $transactions = fetch_all(
        'SELECT order_number, total_amount, order_date, status
         FROM orders
         WHERE supplier_id = :supplier_id AND status != "cancelled"
         ORDER BY order_date DESC',
        ['supplier_id' => $supplierId]
    );

    return ['summary' => $summary, 'transactions' => $transactions];
}

function supplier_messages_payload(int $supplierId): array
{
    return fetch_all(
        'SELECT t.*, b.store_name, b.buyer_name
         FROM chat_threads t
         JOIN buyers b ON b.id = t.buyer_id
         WHERE t.supplier_id = :supplier_id
         ORDER BY t.last_message_at DESC',
        ['supplier_id' => $supplierId]
    );
}

function create_order_with_items(array $payload): int
{
    $pdo = db();
    $pdo->beginTransaction();

    try {
        $orderId = persist_row('orders', [
            'order_number' => $payload['order_number'],
            'buyer_id' => $payload['buyer_id'],
            'supplier_id' => $payload['supplier_id'],
            'status' => $payload['status'] ?? 'processing',
            'payment_status' => $payload['payment_status'] ?? 'pending',
            'subtotal' => $payload['subtotal'],
            'delivery_fee' => $payload['delivery_fee'],
            'total_amount' => $payload['total_amount'],
            'notes' => $payload['notes'] ?? '',
            'order_date' => $payload['order_date'] ?? date('Y-m-d'),
            'delivery_date' => $payload['delivery_date'] ?? null,
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        foreach ($payload['items'] as $item) {
            persist_row('order_items', [
                'order_id' => $orderId,
                'supplier_product_id' => $item['supplier_product_id'],
                'product_name' => $item['product_name'],
                'unit_label' => $item['unit_label'],
                'quantity' => $item['quantity'],
                'unit_price' => $item['unit_price'],
                'line_total' => $item['line_total'],
            ]);
        }

        $pdo->commit();
        return $orderId;
    } catch (Throwable $exception) {
        $pdo->rollBack();
        throw $exception;
    }
}

function normalize_phone_number(string $phone): string
{
    $normalized = preg_replace('/[^\d+]/', '', trim($phone)) ?? '';
    if ($normalized !== '' && $normalized[0] !== '+' && ctype_digit($normalized)) {
        $normalized = '+' . $normalized;
    }

    return $normalized;
}

function otp_expiry_minutes(): int
{
    return max(1, (int) setting_value('otp_expiry_minutes', '10'));
}

function latest_pending_otp_request(string $role, string $purpose, string $phone): ?array
{
    return fetch_one(
        'SELECT *
         FROM otp_requests
         WHERE user_role = :user_role
           AND purpose = :purpose
           AND phone = :phone
           AND consumed_at IS NULL
           AND status IN ("pending", "sent")
         ORDER BY id DESC
         LIMIT 1',
        [
            'user_role' => $role,
            'purpose' => $purpose,
            'phone' => normalize_phone_number($phone),
        ]
    );
}

function create_otp_request_record(string $role, string $purpose, string $phone, array $payload = []): array
{
    $normalizedPhone = normalize_phone_number($phone);
    $code = (string) random_int(100000, 999999);
    $expiresAt = date('Y-m-d H:i:s', strtotime('+' . otp_expiry_minutes() . ' minutes'));
    $channel = strtolower(setting_value('otp_delivery_channel', 'sms'));
    $provider = strtolower(setting_value('otp_provider', 'demo'));

    execute_query(
        'UPDATE otp_requests
         SET status = "superseded", consumed_at = NOW(), updated_at = NOW()
         WHERE user_role = :user_role
           AND purpose = :purpose
           AND phone = :phone
           AND consumed_at IS NULL
           AND status IN ("pending", "sent")',
        [
            'user_role' => $role,
            'purpose' => $purpose,
            'phone' => $normalizedPhone,
        ]
    );

    $requestId = persist_row('otp_requests', [
        'user_role' => $role,
        'purpose' => $purpose,
        'phone' => $normalizedPhone,
        'channel' => $channel,
        'provider' => $provider,
        'code_hash' => hash('sha256', $code),
        'payload_json' => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        'status' => 'pending',
        'expires_at' => $expiresAt,
        'verified_at' => null,
        'consumed_at' => null,
        'created_at' => date('Y-m-d H:i:s'),
        'updated_at' => date('Y-m-d H:i:s'),
    ]);

    $delivery = deliver_otp_code($normalizedPhone, $code);
    persist_row('otp_requests', [
        'status' => !empty($delivery['delivered']) ? 'sent' : 'pending',
        'delivery_response' => json_encode($delivery, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        'updated_at' => date('Y-m-d H:i:s'),
    ], $requestId);

    return [
        'id' => $requestId,
        'phone' => $normalizedPhone,
        'expires_at' => $expiresAt,
        'delivery' => $delivery,
        'debug_code' => array_get($delivery, 'debug_code'),
    ];
}

function verify_otp_request_code(string $role, string $purpose, string $phone, string $code): array
{
    $request = latest_pending_otp_request($role, $purpose, $phone);
    if (!$request) {
        throw new RuntimeException('No active OTP request found. Please request a new code.');
    }

    if (strtotime((string) $request['expires_at']) < time()) {
        persist_row('otp_requests', [
            'status' => 'expired',
            'updated_at' => date('Y-m-d H:i:s'),
        ], (int) $request['id']);
        throw new RuntimeException('OTP expired. Please request a new code.');
    }

    if (!hash_equals((string) $request['code_hash'], hash('sha256', trim($code)))) {
        throw new RuntimeException('Invalid OTP code.');
    }

    persist_row('otp_requests', [
        'status' => 'verified',
        'verified_at' => date('Y-m-d H:i:s'),
        'consumed_at' => date('Y-m-d H:i:s'),
        'updated_at' => date('Y-m-d H:i:s'),
    ], (int) $request['id']);

    $payload = json_decode((string) ($request['payload_json'] ?? '{}'), true);
    if (!is_array($payload)) {
        $payload = [];
    }

    return [
        'request' => $request,
        'payload' => $payload,
    ];
}

function deliver_otp_code(string $phone, string $code): array
{
    $provider = strtolower(setting_value('otp_provider', 'demo'));
    $expiryMinutes = otp_expiry_minutes();
    $messageTemplate = setting_value(
        'otp_message_template',
        'Your Muhalli verification code is {{CODE}}. It expires in {{MINUTES}} minutes.'
    );
    $message = str_replace(
        ['{{CODE}}', '{{MINUTES}}'],
        [$code, (string) $expiryMinutes],
        $messageTemplate
    );

    if ($provider === 'brqsms') {
        return send_brqsms_otp($phone, $message, $code);
    }

    $logPath = public_upload_path('otp-debug.log');
    ensure_directory(dirname($logPath));
    $logLine = sprintf("[%s] %s => %s\n", date('Y-m-d H:i:s'), $phone, $message);
    file_put_contents($logPath, $logLine, FILE_APPEND);

    return [
        'provider' => $provider,
        'delivered' => false,
        'debug_code' => $code,
        'message' => $message,
    ];
}

function send_brqsms_otp(string $phone, string $message, string $code): array
{
    $url = setting_value('otp_api_url', 'https://dash.brqsms.com/api/http/sms/send');
    $token = trim((string) setting_value('otp_api_token', ''));
    $senderId = trim((string) setting_value('otp_sender_id', ''));
    $recipient = ltrim(trim($phone), '+');

    if ($token === '') {
        return [
            'provider' => 'brqsms',
            'delivered' => false,
            'debug_code' => $code,
            'message' => $message,
            'warning' => 'BRQSMS token is missing.',
        ];
    }

    if ($senderId === '') {
        return [
            'provider' => 'brqsms',
            'delivered' => false,
            'debug_code' => $code,
            'message' => $message,
            'warning' => 'BRQSMS sender ID is missing.',
        ];
    }

    $payload = [
        'api_token' => $token,
        'recipient' => $recipient,
        'sender_id' => $senderId,
        'type' => 'plain',
        'message' => $message,
    ];

    $response = http_post_json($url, $payload, [
        'Accept: application/json',
    ]);

    $decoded = json_decode((string) $response['body'], true);
    if (!is_array($decoded)) {
        $decoded = [];
    }

    $providerStatus = strtolower((string) ($decoded['status'] ?? ''));
    $providerMessage = (string) ($decoded['message'] ?? '');

    $delivered = $response['status'] >= 200
        && $response['status'] < 300
        && $providerStatus === 'success';

    return [
        'provider' => 'brqsms',
        'delivered' => $delivered,
        'status' => $response['status'],
        'body' => $response['body'],
        'provider_status' => $providerStatus,
        'provider_message' => $providerMessage,
        'debug_code' => $delivered ? null : $code,
    ];
}

function http_post_json(string $url, array $payload, array $headers = []): array
{
    $ch = curl_init($url);
    if ($ch === false) {
        throw new RuntimeException('Unable to start OTP provider request.');
    }

    $requestHeaders = array_merge(
        ['Content-Type: application/json'],
        $headers
    );

    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_HTTPHEADER => $requestHeaders,
        CURLOPT_POSTFIELDS => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ]);

    $body = curl_exec($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($body === false) {
        throw new RuntimeException($error ?: 'OTP provider request failed.');
    }

    return [
        'status' => $status,
        'body' => $body,
    ];
}

function http_post_form(string $url, array $payload, array $headers = []): array
{
    $ch = curl_init($url);
    if ($ch === false) {
        throw new RuntimeException('Unable to start OTP provider request.');
    }

    $requestHeaders = array_merge(
        ['Content-Type: application/x-www-form-urlencoded'],
        $headers
    );

    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_HTTPHEADER => $requestHeaders,
        CURLOPT_POSTFIELDS => http_build_query($payload),
    ]);

    $body = curl_exec($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($body === false) {
        throw new RuntimeException($error ?: 'OTP provider request failed.');
    }

    return [
        'status' => $status,
        'body' => $body,
    ];
}
