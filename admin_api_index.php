<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/helpers.php';
require_once dirname(__DIR__) . '/includes/services.php';
require_once dirname(__DIR__) . '/includes/auth.php';
ensure_runtime_schema();

header('Content-Type: application/json');

function api_input(): array
{
    static $payload;
    if ($payload !== null) {
        return $payload;
    }

    $payload = $_POST;
    $raw = file_get_contents('php://input');
    if ($raw) {
        $decoded = json_decode($raw, true);
        if (is_array($decoded)) {
            $payload = array_merge($payload, $decoded);
        }
    }

    return $payload;
}

function api_value(string $key, mixed $default = null): mixed
{
    $payload = api_input();
    return $payload[$key] ?? $_GET[$key] ?? $default;
}

function api_success(mixed $data = null, string $message = 'OK', int $status = 200): never
{
    json_response([
        'success' => true,
        'message' => $message,
        'data' => $data,
    ], $status);
}

function api_fail(string $message, int $status = 422): never
{
    json_response([
        'success' => false,
        'message' => $message,
    ], $status);
}

function api_endpoint(): string
{
    $endpoint = trim((string) ($_GET['endpoint'] ?? ($_SERVER['PATH_INFO'] ?? '')), '/');
    if ($endpoint !== '') {
        return $endpoint;
    }

    $uri = parse_url((string) ($_SERVER['REQUEST_URI'] ?? ''), PHP_URL_PATH);
    $base = '/Muhalli New panle Admin/api/';
    if ($uri && str_contains($uri, $base)) {
        return trim(str_replace($base, '', $uri), '/');
    }

    return '';
}

function bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['Authorization'] ?? '';
    if (preg_match('/Bearer\s+(.+)/i', (string) $header, $matches)) {
        return trim($matches[1]);
    }

    return null;
}

function api_identity(?string $expectedType = null): ?array
{
    $token = bearer_token();
    if (!$token) {
        return null;
    }

    $record = fetch_one(
        'SELECT * FROM api_tokens WHERE token = :token AND (expires_at IS NULL OR expires_at >= NOW()) ORDER BY id DESC LIMIT 1',
        ['token' => $token]
    );

    if (!$record) {
        return null;
    }

    if ($expectedType !== null && $record['user_type'] !== $expectedType) {
        return null;
    }

    return $record;
}

function require_api_identity(string $expectedType): array
{
    $identity = api_identity($expectedType);
    if ($identity) {
        return $identity;
    }

    $fallbackId = (int) api_value($expectedType . '_id', 0);
    if ($fallbackId > 0) {
        return ['user_type' => $expectedType, 'user_id' => $fallbackId];
    }

    api_fail('Unauthorized. Provide a bearer token or ' . $expectedType . '_id.', 401);
}

function order_payload_from_items(int $buyerId, int $supplierId, array $items, string $notes = ''): array
{
    if (empty($items)) {
        api_fail('Order items are required.');
    }

    $preparedItems = [];
    $subtotal = 0.0;

    foreach ($items as $item) {
        $listingId = (int) ($item['supplier_product_id'] ?? 0);
        $quantity = max(1, (int) ($item['quantity'] ?? 1));
        $listing = find_listing($listingId);

        if (!$listing || (int) $listing['supplier_id'] !== $supplierId) {
            api_fail('Invalid supplier product in order items.');
        }

        $lineTotal = (float) $listing['price'] * $quantity;
        $subtotal += $lineTotal;

        $preparedItems[] = [
            'supplier_product_id' => $listingId,
            'product_name' => $listing['catalog_name'],
            'unit_label' => $listing['unit_type'],
            'quantity' => $quantity,
            'unit_price' => (float) $listing['price'],
            'line_total' => $lineTotal,
        ];
    }

    $deliveryFee = (float) api_value('delivery_fee', 0);

    $deliveryName = trim((string) api_value('delivery_name', ''));
    $deliveryPhone = trim((string) api_value('delivery_phone', ''));
    $deliveryAddress = trim((string) api_value('delivery_address', ''));
    $deliveryLines = [];
    if ($deliveryName !== '') {
        $deliveryLines[] = 'Name: ' . $deliveryName;
    }
    if ($deliveryPhone !== '') {
        $deliveryLines[] = 'Phone: ' . $deliveryPhone;
    }
    if ($deliveryAddress !== '') {
        $deliveryLines[] = 'Address: ' . $deliveryAddress;
    }
    if (trim($notes) !== '') {
        $deliveryLines[] = 'Note: ' . trim($notes);
    }

    return [
        'order_number' => 'MW-' . date('Ymd') . '-' . random_int(100, 999),
        'buyer_id' => $buyerId,
        'supplier_id' => $supplierId,
        'subtotal' => $subtotal,
        'delivery_fee' => $deliveryFee,
        'total_amount' => $subtotal + $deliveryFee,
        'notes' => implode("\n", $deliveryLines),
        'status' => 'processing',
        'payment_status' => 'pending',
        'items' => $preparedItems,
    ];
}

$endpoint = api_endpoint();
$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

try {
    switch ($endpoint) {
        case 'auth/buyer/login':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $buyer = find_buyer_by_email((string) api_value('email', ''));
            if (!$buyer || !password_verify((string) api_value('password', ''), (string) $buyer['password_hash'])) {
                api_fail('Invalid buyer credentials.', 401);
            }
            api_success([
                'token' => issue_api_token('buyer', (int) $buyer['id']),
                'buyer' => [
                    'id' => (int) $buyer['id'],
                    'store_name' => $buyer['store_name'],
                    'buyer_name' => $buyer['buyer_name'],
                    'email' => $buyer['email'],
                    'city' => $buyer['city'],
                    'preferred_language' => $buyer['preferred_language'],
                ],
            ], 'Buyer login successful.');

        case 'auth/buyer/register':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $email = (string) api_value('email', '');
            if (find_buyer_by_email($email)) {
                api_fail('Buyer email already exists.');
            }
            $buyerId = persist_row('buyers', [
                'store_name' => (string) api_value('store_name', ''),
                'buyer_name' => (string) api_value('buyer_name', ''),
                'email' => $email,
                'phone' => (string) api_value('phone', ''),
                'city' => (string) api_value('city', ''),
                'address' => (string) api_value('address', ''),
                'password_hash' => password_hash((string) api_value('password', 'password'), PASSWORD_DEFAULT),
                'preferred_language' => (string) api_value('preferred_language', 'en'),
                'status' => 'active',
                'member_since' => date('Y-m-d'),
                'created_at' => date('Y-m-d H:i:s'),
                'updated_at' => date('Y-m-d H:i:s'),
            ]);
            api_success(['token' => issue_api_token('buyer', $buyerId), 'buyer_id' => $buyerId], 'Buyer registered.', 201);

        case 'auth/supplier/login':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $supplier = find_supplier_by_email((string) api_value('email', ''));
            if (!$supplier || !password_verify((string) api_value('password', ''), (string) $supplier['password_hash'])) {
                api_fail('Invalid supplier credentials.', 401);
            }
            api_success([
                'token' => issue_api_token('supplier', (int) $supplier['id']),
                'supplier' => [
                    'id' => (int) $supplier['id'],
                    'business_name' => $supplier['business_name'],
                    'owner_name' => $supplier['owner_name'],
                    'email' => $supplier['email'],
                    'city' => $supplier['city'],
                    'status' => $supplier['status'],
                ],
            ], 'Supplier login successful.');

        case 'auth/supplier/register':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $email = (string) api_value('email', '');
            if (find_supplier_by_email($email)) {
                api_fail('Supplier email already exists.');
            }
            $supplierId = persist_row('suppliers', [
                'business_name' => (string) api_value('business_name', ''),
                'owner_name' => (string) api_value('owner_name', ''),
                'email' => $email,
                'phone' => (string) api_value('phone', ''),
                'city' => (string) api_value('city', ''),
                'address' => (string) api_value('address', ''),
                'business_license_number' => (string) api_value('business_license_number', ''),
                'password_hash' => password_hash((string) api_value('password', 'password'), PASSWORD_DEFAULT),
                'minimum_order_quantity' => (int) api_value('minimum_order_quantity', 1),
                'minimum_order_amount' => (float) api_value('minimum_order_amount', 0),
                'delivery_time' => (string) api_value('delivery_time', '24-48 hours'),
                'payment_terms' => (string) api_value('payment_terms', 'Net 15'),
                'description' => (string) api_value('description', ''),
                'logo_url' => (string) api_value('logo_url', ''),
                'status' => 'pending',
                'is_verified' => 0,
                'created_at' => date('Y-m-d H:i:s'),
                'updated_at' => date('Y-m-d H:i:s'),
            ]);
            api_success(['token' => issue_api_token('supplier', $supplierId), 'supplier_id' => $supplierId], 'Supplier registered.', 201);

        case 'auth/request-otp':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $role = strtolower((string) api_value('role', ''));
            $purpose = strtolower((string) api_value('purpose', 'login'));
            $phone = normalize_phone_number((string) api_value('phone', ''));
            if (!in_array($role, ['buyer', 'supplier'], true)) {
                api_fail('A valid role is required.');
            }
            if (!in_array($purpose, ['login', 'register'], true)) {
                api_fail('A valid OTP purpose is required.');
            }
            if ($phone === '' || strlen(preg_replace('/\D/', '', $phone) ?? '') < 8) {
                api_fail('A valid phone number is required.');
            }

            if ($role === 'buyer') {
                $existingBuyer = find_buyer_by_phone($phone);
                if ($purpose === 'login' && !$existingBuyer) {
                    api_fail('Buyer account not found for this phone number.', 404);
                }
                if ($purpose === 'register' && $existingBuyer) {
                    api_fail('A buyer account already exists for this phone number.');
                }

                $email = trim((string) api_value('email', ''));
                if ($purpose === 'register' && $email !== '' && find_buyer_by_email($email)) {
                    api_fail('Buyer email already exists.');
                }

                $payload = $purpose === 'register'
                    ? [
                        'store_name' => (string) api_value('store_name', ''),
                        'buyer_name' => (string) api_value('buyer_name', api_value('store_name', '')),
                        'email' => $email,
                        'phone' => $phone,
                        'city' => (string) api_value('city', ''),
                        'address' => (string) api_value('address', ''),
                        'preferred_language' => (string) api_value('preferred_language', 'en'),
                    ]
                    : [];
            } else {
                $existingSupplier = find_supplier_by_phone($phone);
                if ($purpose === 'login' && !$existingSupplier) {
                    api_fail('Supplier account not found for this phone number.', 404);
                }
                if ($purpose === 'register' && $existingSupplier) {
                    api_fail('A supplier account already exists for this phone number.');
                }

                $email = trim((string) api_value('email', ''));
                if ($purpose === 'register' && $email !== '' && find_supplier_by_email($email)) {
                    api_fail('Supplier email already exists.');
                }

                $payload = $purpose === 'register'
                    ? [
                        'business_name' => (string) api_value('business_name', ''),
                        'owner_name' => (string) api_value('owner_name', ''),
                        'email' => $email,
                        'phone' => $phone,
                        'city' => (string) api_value('city', ''),
                        'address' => (string) api_value('address', ''),
                        'business_license_number' => (string) api_value('business_license_number', ''),
                        'minimum_order_quantity' => (int) api_value('minimum_order_quantity', 1),
                        'minimum_order_amount' => (float) api_value('minimum_order_amount', 0),
                        'delivery_time' => (string) api_value('delivery_time', setting_value('default_delivery_window', '24-48 hours')),
                        'payment_terms' => (string) api_value('payment_terms', 'Net 15'),
                        'description' => (string) api_value('description', ''),
                    ]
                    : [];
            }

            $otpRequest = create_otp_request_record($role, $purpose, $phone, $payload);
            api_success([
                'phone' => $otpRequest['phone'],
                'expires_at' => $otpRequest['expires_at'],
                'delivery' => $otpRequest['delivery'],
                'debug_code' => $otpRequest['debug_code'],
            ], 'OTP requested successfully.');

        case 'auth/verify-otp':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $role = strtolower((string) api_value('role', ''));
            $purpose = strtolower((string) api_value('purpose', 'login'));
            $phone = normalize_phone_number((string) api_value('phone', ''));
            $code = trim((string) api_value('code', ''));
            if (!in_array($role, ['buyer', 'supplier'], true)) {
                api_fail('A valid role is required.');
            }
            if (!in_array($purpose, ['login', 'register'], true)) {
                api_fail('A valid OTP purpose is required.');
            }
            if ($phone === '' || $code === '') {
                api_fail('Phone number and OTP code are required.');
            }

            $verification = verify_otp_request_code($role, $purpose, $phone, $code);
            $payload = $verification['payload'];

            if ($role === 'buyer') {
                if ($purpose === 'login') {
                    $buyer = find_buyer_by_phone($phone);
                    if (!$buyer) {
                        api_fail('Buyer account not found.', 404);
                    }
                } else {
                    $email = (string) ($payload['email'] ?? '');
                    if ($email !== '' && find_buyer_by_email($email)) {
                        api_fail('Buyer email already exists.');
                    }

                    $buyerId = persist_row('buyers', [
                        'store_name' => (string) ($payload['store_name'] ?? ''),
                        'buyer_name' => (string) ($payload['buyer_name'] ?? ($payload['store_name'] ?? '')),
                        'email' => $email,
                        'phone' => $phone,
                        'city' => (string) ($payload['city'] ?? ''),
                        'address' => (string) ($payload['address'] ?? ''),
                        'password_hash' => password_hash(bin2hex(random_bytes(16)), PASSWORD_DEFAULT),
                        'preferred_language' => (string) ($payload['preferred_language'] ?? 'en'),
                        'status' => 'active',
                        'member_since' => date('Y-m-d'),
                        'created_at' => date('Y-m-d H:i:s'),
                        'updated_at' => date('Y-m-d H:i:s'),
                    ]);
                    $buyer = find_buyer($buyerId);
                }

                api_success([
                    'token' => issue_api_token('buyer', (int) $buyer['id']),
                    'buyer_id' => (int) $buyer['id'],
                    'buyer' => [
                        'id' => (int) $buyer['id'],
                        'store_name' => $buyer['store_name'],
                        'buyer_name' => $buyer['buyer_name'],
                        'email' => $buyer['email'],
                        'phone' => $buyer['phone'],
                        'city' => $buyer['city'],
                        'preferred_language' => $buyer['preferred_language'],
                    ],
                ], 'Buyer authenticated successfully.');
            }

            if ($purpose === 'login') {
                $supplier = find_supplier_by_phone($phone);
                if (!$supplier) {
                    api_fail('Supplier account not found.', 404);
                }
            } else {
                $email = (string) ($payload['email'] ?? '');
                if ($email !== '' && find_supplier_by_email($email)) {
                    api_fail('Supplier email already exists.');
                }

                $supplierId = persist_row('suppliers', [
                    'business_name' => (string) ($payload['business_name'] ?? ''),
                    'owner_name' => (string) ($payload['owner_name'] ?? ''),
                    'email' => $email,
                    'phone' => $phone,
                    'city' => (string) ($payload['city'] ?? ''),
                    'address' => (string) ($payload['address'] ?? ''),
                    'business_license_number' => (string) ($payload['business_license_number'] ?? ''),
                    'password_hash' => password_hash(bin2hex(random_bytes(16)), PASSWORD_DEFAULT),
                    'minimum_order_quantity' => (int) ($payload['minimum_order_quantity'] ?? 1),
                    'minimum_order_amount' => (float) ($payload['minimum_order_amount'] ?? 0),
                    'delivery_time' => (string) ($payload['delivery_time'] ?? setting_value('default_delivery_window', '24-48 hours')),
                    'payment_terms' => (string) ($payload['payment_terms'] ?? 'Net 15'),
                    'description' => (string) ($payload['description'] ?? ''),
                    'logo_url' => '',
                    'status' => 'pending',
                    'is_verified' => 0,
                    'created_at' => date('Y-m-d H:i:s'),
                    'updated_at' => date('Y-m-d H:i:s'),
                ]);
                $supplier = find_supplier($supplierId);
            }

            api_success([
                'token' => issue_api_token('supplier', (int) $supplier['id']),
                'supplier_id' => (int) $supplier['id'],
                'supplier' => [
                    'id' => (int) $supplier['id'],
                    'business_name' => $supplier['business_name'],
                    'owner_name' => $supplier['owner_name'],
                    'email' => $supplier['email'],
                    'phone' => $supplier['phone'],
                    'city' => $supplier['city'],
                    'status' => $supplier['status'],
                ],
            ], 'Supplier authenticated successfully.');

        case 'settings/public':
            api_success(public_config_payload());

        case 'admin/catalog-options':
            if ($method !== 'GET') {
                api_fail('Method not allowed.', 405);
            }
            if (!is_admin_logged_in()) {
                api_fail('Unauthorized.', 401);
            }
            api_success(paginated_catalog_options(
                (string) api_value('search', ''),
                (int) api_value('page', 1),
                (int) api_value('limit', 25)
            ));

        case 'admin/supplier-options':
            if ($method !== 'GET') {
                api_fail('Method not allowed.', 405);
            }
            if (!is_admin_logged_in()) {
                api_fail('Unauthorized.', 401);
            }
            api_success(paginated_supplier_options(
                (string) api_value('search', ''),
                (int) api_value('page', 1),
                (int) api_value('limit', 25)
            ));

        case 'buyer/home':
            api_success(buyer_home_payload());

        case 'buyer/categories':
            api_success(buyer_categories_payload());

        case 'buyer/suppliers':
            $supplierIdentity = api_identity('buyer');
            $supplierCity = (string) api_value('city', '');
            if ($supplierIdentity) {
                $buyer = find_buyer((int) $supplierIdentity['user_id']);
                $supplierCity = (string) ($buyer['city'] ?? $supplierCity);
            }
            api_success(buyer_suppliers_payload([
                'search' => (string) api_value('search', ''),
                'city' => $supplierCity,
                'sort' => (string) api_value('sort', 'default'),
            ]));

        case 'buyer/products':
            $productIdentity = api_identity('buyer');
            $productCity = (string) api_value('city', '');
            if ($productIdentity) {
                $buyer = find_buyer((int) $productIdentity['user_id']);
                $productCity = (string) ($buyer['city'] ?? $productCity);
            }
            api_success(buyer_products_payload([
                'search' => (string) api_value('search', ''),
                'supplier_id' => (int) api_value('supplier_id', 0),
                'category_id' => (int) api_value('category_id', 0),
                'city' => $productCity,
                'sort' => (string) api_value('sort', 'default'),
            ]));

        case 'buyer/offers':
            $offersIdentity = api_identity('buyer');
            $offerCity = (string) api_value('city', '');
            if ($offersIdentity) {
                $buyer = find_buyer((int) $offersIdentity['user_id']);
                $offerCity = (string) ($buyer['city'] ?? $offerCity);
            }
            api_success(active_offers_payload($offerCity));

        case 'buyer/notifications':
            $identity = require_api_identity('buyer');
            api_success(buyer_notifications_payload((int) $identity['user_id']));

        case 'buyer/notifications/register-device':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('buyer');
            register_buyer_device_token(
                (int) $identity['user_id'],
                (string) api_value('firebase_token', ''),
                (string) api_value('platform', 'android')
            );
            api_success(['registered' => true], 'Buyer device token saved.');

        case 'buyer/referrals':
            $identity = require_api_identity('buyer');
            api_success(buyer_referral_summary_payload((int) $identity['user_id']));

        case 'buyer/referrals/apply':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('buyer');
            $claim = apply_buyer_referral_code(
                (int) $identity['user_id'],
                (string) api_value('referral_code', '')
            );
            api_success($claim, 'Referral code applied.');

        case 'buyer/orders':
            $identity = require_api_identity('buyer');
            api_success(buyer_orders_payload((int) $identity['user_id']));

        case 'buyer/profile':
            $identity = require_api_identity('buyer');
            $buyer = find_buyer((int) $identity['user_id']);
            if (!$buyer) {
                api_fail('Buyer not found.', 404);
            }
            api_success($buyer);

        case 'buyer/profile/update':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('buyer');
            $buyer = find_buyer((int) $identity['user_id']);
            if (!$buyer) {
                api_fail('Buyer not found.', 404);
            }
            persist_row('buyers', [
                'store_name' => (string) api_value('store_name', $buyer['store_name']),
                'buyer_name' => (string) api_value('buyer_name', $buyer['buyer_name']),
                'email' => (string) api_value('email', $buyer['email']),
                'phone' => (string) api_value('phone', $buyer['phone']),
                'city' => (string) api_value('city', $buyer['city']),
                'address' => (string) api_value('address', $buyer['address']),
                'password_hash' => (string) $buyer['password_hash'],
                'preferred_language' => (string) api_value('preferred_language', $buyer['preferred_language']),
                'status' => (string) $buyer['status'],
                'member_since' => (string) $buyer['member_since'],
                'created_at' => (string) $buyer['created_at'],
                'updated_at' => date('Y-m-d H:i:s'),
            ], (int) $buyer['id']);
            api_success(find_buyer((int) $buyer['id']), 'Buyer profile updated.');

        case 'buyer/chats':
            $identity = require_api_identity('buyer');
            api_success(buyer_chats_payload((int) $identity['user_id']));

        case 'buyer/chats/thread':
            $identity = require_api_identity('buyer');
            $threadId = (int) api_value('thread_id', 0);
            $thread = find_thread($threadId);
            if (!$thread || (int) $thread['buyer_id'] !== (int) $identity['user_id']) {
                api_fail('Thread not found.', 404);
            }
            execute_query('UPDATE chat_threads SET buyer_unread_count = 0, updated_at = NOW() WHERE id = :id', [
                'id' => $threadId,
            ]);
            api_success(find_thread($threadId));

        case 'buyer/chats/send':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('buyer');
            $threadId = (int) api_value('thread_id', 0);
            $thread = find_thread($threadId);
            if (!$thread || (int) $thread['buyer_id'] !== (int) $identity['user_id']) {
                api_fail('Thread not found.', 404);
            }
            $body = trim((string) api_value('message_body', ''));
            if ($body === '') {
                api_fail('message_body is required.');
            }
            persist_row('chat_messages', [
                'thread_id' => $threadId,
                'sender_type' => 'buyer',
                'sender_name' => (string) $thread['store_name'],
                'message_body' => $body,
                'message_type' => (string) api_value('message_type', 'text'),
                'created_at' => date('Y-m-d H:i:s'),
            ]);
            execute_query(
                'UPDATE chat_threads
                 SET last_message = :last_message,
                     last_message_at = NOW(),
                     buyer_unread_count = 0,
                     supplier_unread_count = supplier_unread_count + 1,
                     updated_at = NOW()
                 WHERE id = :id',
                [
                    'last_message' => $body,
                    'id' => $threadId,
                ]
            );
            api_success(find_thread($threadId), 'Message sent.');

        case 'buyer/orders/create':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('buyer');
            $supplierId = (int) api_value('supplier_id', 0);
            $items = api_value('items', []);
            if (!is_array($items)) {
                api_fail('Items must be an array.');
            }
            $payload = order_payload_from_items((int) $identity['user_id'], $supplierId, $items, (string) api_value('notes', ''));
            $orderId = create_order_with_items($payload);
            api_success(find_order($orderId), 'Order created.', 201);

        case 'supplier/dashboard':
            $identity = require_api_identity('supplier');
            api_success(supplier_dashboard_payload((int) $identity['user_id']));

        case 'supplier/profile':
            $identity = require_api_identity('supplier');
            $supplier = find_supplier((int) $identity['user_id']);
            if (!$supplier) {
                api_fail('Supplier not found.', 404);
            }
            api_success($supplier);

        case 'supplier/profile/update':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('supplier');
            $supplier = find_supplier((int) $identity['user_id']);
            if (!$supplier) {
                api_fail('Supplier not found.', 404);
            }
            persist_row('suppliers', [
                'business_name' => (string) api_value('business_name', $supplier['business_name']),
                'owner_name' => (string) api_value('owner_name', $supplier['owner_name']),
                'email' => (string) api_value('email', $supplier['email']),
                'phone' => (string) api_value('phone', $supplier['phone']),
                'city' => (string) api_value('city', $supplier['city']),
                'address' => (string) api_value('address', $supplier['address']),
                'business_license_number' => (string) api_value('business_license_number', $supplier['business_license_number']),
                'password_hash' => (string) $supplier['password_hash'],
                'minimum_order_quantity' => (int) api_value('minimum_order_quantity', $supplier['minimum_order_quantity']),
                'minimum_order_amount' => (float) api_value('minimum_order_amount', $supplier['minimum_order_amount']),
                'delivery_time' => (string) api_value('delivery_time', $supplier['delivery_time']),
                'payment_terms' => (string) api_value('payment_terms', $supplier['payment_terms']),
                'description' => (string) api_value('description', $supplier['description']),
                'logo_url' => (string) api_value('logo_url', $supplier['logo_url']),
                'status' => (string) $supplier['status'],
                'is_verified' => (int) $supplier['is_verified'],
                'created_at' => (string) $supplier['created_at'],
                'updated_at' => date('Y-m-d H:i:s'),
            ], (int) $supplier['id']);
            api_success(find_supplier((int) $supplier['id']), 'Supplier profile updated.');

        case 'supplier/catalog':
            api_success(supplier_catalog_payload());

        case 'supplier/products':
            $identity = require_api_identity('supplier');
            api_success(supplier_products_payload((int) $identity['user_id']));

        case 'supplier/products/create':
        case 'supplier/products/update':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('supplier');
            $listingId = (int) api_value('listing_id', 0);
            $catalogId = (int) api_value('catalog_product_id', 0);
            if ($listingId > 0 && $catalogId === 0) {
                $existingListing = find_listing($listingId);
                if ($existingListing) {
                    $catalogId = (int) $existingListing['catalog_id'];
                }
            }
            if ($catalogId === 0) {
                api_fail('catalog_product_id is required.');
            }
            $imageDataUrl = (string) api_value('image_data_url', '');
            $imageUrl = trim((string) api_value('image_url', ''));
            if ($imageDataUrl !== '' || $imageUrl !== '') {
                $catalogProduct = fetch_one('SELECT * FROM catalog_products WHERE id = :id LIMIT 1', ['id' => $catalogId]);
                if (!$catalogProduct) {
                    api_fail('Catalog product not found.', 404);
                }

                $resolvedImageUrl = $catalogProduct['image_url'] ?? '';
                if ($imageDataUrl !== '') {
                    $resolvedImageUrl = store_data_url_image($imageDataUrl, 'products');
                } elseif ($imageUrl !== '') {
                    $resolvedImageUrl = $imageUrl;
                }

                persist_row('catalog_products', [
                    'category_id' => (int) $catalogProduct['category_id'],
                    'name' => (string) $catalogProduct['name'],
                    'slug' => (string) $catalogProduct['slug'],
                    'emoji' => (string) $catalogProduct['emoji'],
                    'description' => (string) $catalogProduct['description'],
                    'packaging' => (string) $catalogProduct['packaging'],
                    'unit_type' => (string) $catalogProduct['unit_type'],
                    'image_url' => (string) $resolvedImageUrl,
                    'status' => (string) $catalogProduct['status'],
                    'created_at' => (string) $catalogProduct['created_at'],
                    'updated_at' => date('Y-m-d H:i:s'),
                ], $catalogId);
            }
            $payload = [
                'catalog_product_id' => $catalogId,
                'supplier_id' => (int) $identity['user_id'],
                'sku' => (string) api_value('sku', 'SKU-' . strtoupper(substr(md5((string) microtime()), 0, 8))),
                'price' => (float) api_value('price', 0),
                'stock_quantity' => (int) api_value('stock_quantity', 0),
                'min_order_qty' => (int) api_value('min_order_qty', 1),
                'min_order_amount' => (float) api_value('min_order_amount', 0),
                'delivery_time' => (string) api_value('delivery_time', '24-48 hours'),
                'status' => (string) api_value('status', 'active'),
                'is_featured' => (int) api_value('is_featured', 0),
                'updated_at' => date('Y-m-d H:i:s'),
            ];
            if ($listingId === 0) {
                $payload['created_at'] = date('Y-m-d H:i:s');
            }
            $savedId = persist_row('supplier_products', $payload, $listingId ?: null);
            api_success(find_listing($savedId), 'Supplier product saved.', $listingId ? 200 : 201);

        case 'supplier/offers/create':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('supplier');
            $listingId = (int) api_value('listing_id', 0);
            $listing = find_listing($listingId);
            if (!$listing || (int) $listing['supplier_id'] !== (int) $identity['user_id']) {
                api_fail('Supplier product not found.', 404);
            }

            $offerPrice = (float) api_value('offer_price', 0);
            if ($offerPrice <= 0) {
                api_fail('offer_price is required.');
            }

            $existingOffer = fetch_one(
                'SELECT * FROM offers
                 WHERE supplier_id = :supplier_id
                   AND supplier_product_id = :supplier_product_id
                   AND status IN ("active", "draft")
                 ORDER BY id DESC
                 LIMIT 1',
                [
                    'supplier_id' => (int) $identity['user_id'],
                    'supplier_product_id' => $listingId,
                ]
            );

            $maximumQuantity = (int) api_value('maximum_quantity', 0);
            $offerId = persist_row('offers', [
                'title' => (string) api_value('title', $listing['catalog_name']),
                'description' => (string) api_value('description', 'Supplier special offer'),
                'badge_label' => (string) api_value('badge_label', 'Special Offer'),
                'discount_label' => (string) api_value('discount_label', number_format($offerPrice, 2) . ' PKR'),
                'image_url' => (string) ($listing['image_url'] ?? ''),
                'supplier_id' => (int) $identity['user_id'],
                'supplier_product_id' => $listingId,
                'catalog_product_id' => (int) $listing['catalog_product_id'],
                'offer_price' => $offerPrice,
                'maximum_quantity' => $maximumQuantity > 0 ? $maximumQuantity : null,
                'city' => (string) ($listing['supplier_city'] ?? api_value('city', '')),
                'status' => 'active',
                'starts_at' => null,
                'ends_at' => null,
                'created_at' => (string) ($existingOffer['created_at'] ?? date('Y-m-d H:i:s')),
                'updated_at' => date('Y-m-d H:i:s'),
            ], $existingOffer ? (int) $existingOffer['id'] : null);

            api_success(find_offer($offerId), 'Offer saved.', $existingOffer ? 200 : 201);

        case 'supplier/orders':
            $identity = require_api_identity('supplier');
            api_success(supplier_orders_payload((int) $identity['user_id']));

        case 'supplier/orders/detail':
            $identity = require_api_identity('supplier');
            $order = find_order((int) api_value('order_id', 0));
            if (!$order || (int) $order['supplier_id'] !== (int) $identity['user_id']) {
                api_fail('Order not found.', 404);
            }
            $order['delivery_address'] = $order['buyer_address'] ?? '';
            api_success($order);

        case 'supplier/orders/status':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('supplier');
            $order = find_order((int) api_value('order_id', 0));
            if (!$order || (int) $order['supplier_id'] !== (int) $identity['user_id']) {
                api_fail('Order not found.', 404);
            }
            persist_row('orders', [
                'status' => (string) api_value('status', $order['status']),
                'payment_status' => (string) api_value('payment_status', $order['payment_status']),
                'delivery_date' => (string) api_value('delivery_date', $order['delivery_date']) ?: null,
                'notes' => (string) api_value('notes', $order['notes']),
                'updated_at' => date('Y-m-d H:i:s'),
            ], (int) $order['id']);
            api_success(find_order((int) $order['id']), 'Order status updated.');

        case 'supplier/earnings':
            $identity = require_api_identity('supplier');
            api_success(supplier_earnings_payload((int) $identity['user_id']));

        case 'supplier/messages':
            $identity = require_api_identity('supplier');
            api_success(supplier_messages_payload((int) $identity['user_id']));

        case 'supplier/messages/thread':
            $identity = require_api_identity('supplier');
            $threadId = (int) api_value('thread_id', 0);
            $thread = find_thread($threadId);
            if (!$thread || (int) $thread['supplier_id'] !== (int) $identity['user_id']) {
                api_fail('Thread not found.', 404);
            }
            execute_query('UPDATE chat_threads SET supplier_unread_count = 0, updated_at = NOW() WHERE id = :id', [
                'id' => $threadId,
            ]);
            api_success(find_thread($threadId));

        case 'supplier/messages/send':
            if ($method !== 'POST') {
                api_fail('Method not allowed.', 405);
            }
            $identity = require_api_identity('supplier');
            $threadId = (int) api_value('thread_id', 0);
            $thread = find_thread($threadId);
            if (!$thread || (int) $thread['supplier_id'] !== (int) $identity['user_id']) {
                api_fail('Thread not found.', 404);
            }
            $body = (string) api_value('message_body', '');
            if ($body === '') {
                api_fail('message_body is required.');
            }
            persist_row('chat_messages', [
                'thread_id' => $threadId,
                'sender_type' => 'supplier',
                'sender_name' => (string) $thread['business_name'],
                'message_body' => $body,
                'message_type' => (string) api_value('message_type', 'text'),
                'created_at' => date('Y-m-d H:i:s'),
            ]);
            execute_query(
                'UPDATE chat_threads
                 SET last_message = :last_message,
                     last_message_at = NOW(),
                     buyer_unread_count = buyer_unread_count + 1,
                     supplier_unread_count = 0,
                     updated_at = NOW()
                 WHERE id = :id',
                [
                    'last_message' => $body,
                    'id' => $threadId,
                ]
            );
            api_success(find_thread($threadId), 'Message sent.');

        default:
            api_fail('Endpoint not found: ' . ($endpoint ?: '[empty]'), 404);
    }
} catch (Throwable $exception) {
    api_fail($exception->getMessage(), 500);
}
