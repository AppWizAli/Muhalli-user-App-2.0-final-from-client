<?php
$action = current_action();
$id = (int) query('id', 0);
$filters = [
    'search' => (string) query('search', ''),
    'status' => (string) query('status', ''),
    'supplier_id' => (int) query('supplier_id', 0),
    'category_id' => (int) query('category_id', 0),
];

$categories = category_options();
$suppliers = $action === 'list' ? supplier_options() : [];
$listings = all_product_listings($filters);
$listing = $id ? find_listing($id) : null;
$formListing = in_array($action, ['add', 'edit'], true) ? $listing : null;
$selectedCatalogId = (int) old('catalog_id', (string) ($formListing['catalog_id'] ?? 0));
$selectedSupplierId = (int) old('supplier_id', (string) ($formListing['supplier_id'] ?? 0));
$selectedCatalog = $selectedCatalogId > 0 ? find_catalog_option($selectedCatalogId) : null;
$selectedSupplier = $selectedSupplierId > 0 ? find_supplier_option($selectedSupplierId) : null;

if (in_array($action, ['view', 'edit'], true) && !$listing) {
    $action = 'list';
}
?>

<?php if ($action === 'list'): ?>
    <section class="module-screen">
        <div class="screen-toolbar">
            <form method="get" class="toolbar-filters">
                <input type="hidden" name="page" value="products">
                <div class="search-input">
                    <input type="search" name="search" value="<?= e($filters['search']) ?>" placeholder="Search products...">
                </div>
                <select name="status">
                    <option value="">All Status</option>
                    <?php foreach (['active', 'inactive', 'draft'] as $status): ?>
                        <option value="<?= e($status) ?>" <?= $filters['status'] === $status ? 'selected' : '' ?>><?= e(ucfirst($status)) ?></option>
                    <?php endforeach; ?>
                </select>
                <select name="supplier_id">
                    <option value="0">All Suppliers</option>
                    <?php foreach ($suppliers as $supplier): ?>
                        <option value="<?= e((string) $supplier['id']) ?>" <?= $filters['supplier_id'] === (int) $supplier['id'] ? 'selected' : '' ?>><?= e((string) $supplier['business_name']) ?></option>
                    <?php endforeach; ?>
                </select>
                <select name="category_id">
                    <option value="0">All Categories</option>
                    <?php foreach ($categories as $category): ?>
                        <option value="<?= e((string) $category['id']) ?>" <?= $filters['category_id'] === (int) $category['id'] ? 'selected' : '' ?>><?= e((string) $category['name']) ?></option>
                    <?php endforeach; ?>
                </select>
                <button class="ghost-button" type="submit">Filters</button>
            </form>
            <a class="primary-button" href="<?= e(module_url('products', 'add')) ?>">Add Product</a>
        </div>

        <section class="panel">
            <div class="page-block__header">
                <div>
                    <h3>Products</h3>
                    <p><?= e((string) count($listings)) ?> product listings in the marketplace.</p>
                </div>
            </div>
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Supplier</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Min Order</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php foreach ($listings as $item): ?>
                        <tr>
                            <td>
                                <div class="table-title">
                                    <strong><?= e((string) $item['catalog_name']) ?></strong>
                                    <small><?= e((string) $item['packaging']) ?> / <?= e((string) $item['unit_type']) ?></small>
                                </div>
                            </td>
                            <td><?= e((string) $item['category_name']) ?></td>
                            <td><?= e((string) ($item['supplier_name'] ?: 'Unassigned')) ?></td>
                            <td><?= e(money((float) $item['price'])) ?></td>
                            <td class="<?= (int) $item['stock_quantity'] <= 10 ? 'text-danger' : 'text-success' ?>"><?= e((string) $item['stock_quantity']) ?></td>
                            <td><?= e((string) $item['min_order_qty']) ?></td>
                            <td><span class="status-chip <?= e(status_badge_class((string) $item['status'])) ?>"><?= e(ucfirst((string) $item['status'])) ?></span></td>
                            <td>
                                <div class="row-actions">
                                    <a class="inline-link" href="<?= e(module_url('products', 'view', ['id' => $item['id']])) ?>">View</a>
                                    <a class="inline-link" href="<?= e(module_url('products', 'edit', ['id' => $item['id']])) ?>">Edit</a>
                                    <form method="post" onsubmit="return confirm('Delete this product listing?');">
                                        <?= csrf_field() ?>
                                        <input type="hidden" name="form_action" value="product_delete">
                                        <input type="hidden" name="id" value="<?= e((string) $item['id']) ?>">
                                        <button class="inline-link danger" type="submit">Delete</button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        </section>
    </section>
<?php elseif ($action === 'view' && $listing): ?>
    <section class="module-screen narrow">
        <div class="screen-head">
            <a class="back-link" href="<?= e(module_url('products')) ?>">&larr; Back to Products</a>
            <a class="primary-button" href="<?= e(module_url('products', 'edit', ['id' => $listing['id']])) ?>">Edit Product</a>
        </div>

        <section class="panel">
            <div class="page-block__header">
                <div>
                    <h3><?= e((string) $listing['catalog_name']) ?></h3>
                    <p><?= e((string) ($listing['description'] ?: 'No description added yet.')) ?></p>
                </div>
                <span class="status-chip <?= e(status_badge_class((string) $listing['status'])) ?>"><?= e(ucfirst((string) $listing['status'])) ?></span>
            </div>

            <?php if (!empty($listing['image_url'])): ?>
                <div style="margin-bottom: 20px;">
                    <img src="<?= e((string) $listing['image_url']) ?>" alt="<?= e((string) $listing['catalog_name']) ?>" style="max-width: 220px; width: 100%; border-radius: 18px; object-fit: cover;">
                </div>
            <?php endif; ?>

            <div class="detail-grid">
                <div><strong>Category</strong><span><?= e((string) $listing['category_name']) ?></span></div>
                <div><strong>Supplier</strong><span><?= e((string) ($listing['supplier_name'] ?: 'Unassigned')) ?></span></div>
                <div><strong>Price</strong><span><?= e(money((float) $listing['price'])) ?></span></div>
                <div><strong>Stock Quantity</strong><span><?= e((string) $listing['stock_quantity']) ?></span></div>
                <div><strong>Min Order Qty</strong><span><?= e((string) $listing['min_order_qty']) ?></span></div>
                <div><strong>Min Order Amount</strong><span><?= e(money((float) $listing['min_order_amount'])) ?></span></div>
                <div><strong>Delivery Time</strong><span><?= e((string) $listing['delivery_time']) ?></span></div>
                <div><strong>SKU</strong><span><?= e((string) $listing['sku']) ?></span></div>
                <div><strong>Packaging</strong><span><?= e((string) $listing['packaging']) ?></span></div>
                <div><strong>Unit Type</strong><span><?= e((string) $listing['unit_type']) ?></span></div>
                <div><strong>Catalog Slug</strong><span><?= e((string) $listing['slug']) ?></span></div>
                <div><strong>Featured</strong><span><?= (int) $listing['is_featured'] === 1 ? 'Yes' : 'No' ?></span></div>
            </div>
        </section>
    </section>
<?php else: ?>
    <?php $isEdit = $action === 'edit' && $formListing; ?>
    <section class="module-screen narrow">
        <div class="screen-head">
            <a class="back-link" href="<?= e(module_url('products')) ?>">&larr; Back to Products</a>
        </div>

        <section class="panel form-panel">
            <div class="page-block__header">
                <div>
                    <h3><?= $isEdit ? 'Edit Product' : 'Add New Product' ?></h3>
                    <p>Use the buyer and supplier app flow while managing the product listing and marketplace visibility.</p>
                </div>
            </div>

            <div class="notice-box">
                <strong>Product & Supplier Assignment</strong>
                <p>Suppliers can add their own products from the supplier app later. Here you can also create shared catalog items and manually assign them to suppliers.</p>
            </div>

            <form method="post" enctype="multipart/form-data" class="stack-form">
                <?= csrf_field() ?>
                <input type="hidden" name="form_action" value="product_save">
                <input type="hidden" name="listing_id" value="<?= e((string) ($formListing['id'] ?? 0)) ?>">

                <label>
                    <span>Existing Catalog Product</span>
                    <div
                        class="async-select"
                        data-async-select
                        data-endpoint="<?= e(api_url('admin/catalog-options')) ?>"
                        data-placeholder="Search catalog product by name, packaging, or category"
                        data-empty-label="Create new catalog item"
                        data-selected-id="<?= e((string) $selectedCatalogId) ?>"
                        data-selected-label="<?= e((string) ($selectedCatalog['label'] ?? 'Create new catalog item')) ?>"
                        data-search-placeholder="Type to search catalog products"
                        data-kind="catalog"
                    >
                        <input type="hidden" name="catalog_id" value="<?= e((string) $selectedCatalogId) ?>">
                        <button type="button" class="async-select__trigger" data-async-select-trigger aria-expanded="false">
                            <span class="async-select__trigger-copy">
                                <strong data-async-select-label><?= e((string) ($selectedCatalog['label'] ?? 'Create new catalog item')) ?></strong>
                                <small data-async-select-meta><?= e((string) ($selectedCatalog['meta'] ?? 'Leave unselected if you want to create a fresh catalog item.')) ?></small>
                            </span>
                            <span class="async-select__chevron" aria-hidden="true">▾</span>
                        </button>
                        <div class="async-select__panel" data-async-select-panel hidden>
                            <div class="async-select__search-row">
                                <input type="search" class="async-select__search" data-async-select-search placeholder="Type to search catalog products">
                                <button type="button" class="ghost-button async-select__clear" data-async-select-clear>Clear</button>
                            </div>
                            <div class="async-select__results" data-async-select-results></div>
                            <div class="async-select__status" data-async-select-status></div>
                        </div>
                    </div>
                </label>

                <div class="two-field">
                    <label>
                        <span>Product Name</span>
                        <input type="text" name="catalog_name" value="<?= e((string) ($formListing['catalog_name'] ?? old('catalog_name', ''))) ?>" required>
                    </label>
                    <label>
                        <span>Icon / Emoji</span>
                        <input type="text" name="emoji" value="<?= e((string) ($formListing['emoji'] ?? old('emoji', '📦'))) ?>">
                    </label>
                </div>

                <div class="two-field">
                    <label>
                        <span>Category</span>
                        <select name="category_id" required>
                            <option value="">Choose category</option>
                            <?php foreach ($categories as $category): ?>
                                <option value="<?= e((string) $category['id']) ?>" <?= (int) ($formListing['category_id'] ?? 0) === (int) $category['id'] ? 'selected' : '' ?>><?= e((string) $category['name']) ?></option>
                            <?php endforeach; ?>
                        </select>
                    </label>
                    <label>
                        <span>Supplier</span>
                        <div
                            class="async-select"
                            data-async-select
                            data-endpoint="<?= e(api_url('admin/supplier-options')) ?>"
                            data-placeholder="Search supplier by business name, owner, city, or phone"
                            data-empty-label="Leave empty / assign later"
                            data-selected-id="<?= e((string) $selectedSupplierId) ?>"
                            data-selected-label="<?= e((string) ($selectedSupplier['label'] ?? 'Leave empty / assign later')) ?>"
                            data-search-placeholder="Type to search suppliers"
                            data-kind="supplier"
                        >
                            <input type="hidden" name="supplier_id" value="<?= e((string) $selectedSupplierId) ?>">
                            <button type="button" class="async-select__trigger" data-async-select-trigger aria-expanded="false">
                                <span class="async-select__trigger-copy">
                                    <strong data-async-select-label><?= e((string) ($selectedSupplier['label'] ?? 'Leave empty / assign later')) ?></strong>
                                    <small data-async-select-meta><?= e((string) ($selectedSupplier['meta'] ?? 'Search supplier records instead of loading the full list.')) ?></small>
                                </span>
                                <span class="async-select__chevron" aria-hidden="true">▾</span>
                            </button>
                            <div class="async-select__panel" data-async-select-panel hidden>
                                <div class="async-select__search-row">
                                    <input type="search" class="async-select__search" data-async-select-search placeholder="Type to search suppliers">
                                    <button type="button" class="ghost-button async-select__clear" data-async-select-clear>Clear</button>
                                </div>
                                <div class="async-select__results" data-async-select-results></div>
                                <div class="async-select__status" data-async-select-status></div>
                            </div>
                        </div>
                    </label>
                </div>

                <div class="two-field">
                    <label>
                        <span>Packaging Details</span>
                        <input type="text" name="packaging" value="<?= e((string) ($formListing['packaging'] ?? old('packaging', '24 packs x 150g'))) ?>">
                    </label>
                    <label>
                        <span>Unit Type</span>
                        <input type="text" name="unit_type" value="<?= e((string) ($formListing['unit_type'] ?? old('unit_type', 'Carton'))) ?>">
                    </label>
                </div>

                <label>
                    <span>Description</span>
                    <textarea name="description" rows="4"><?= e((string) ($formListing['description'] ?? old('description', ''))) ?></textarea>
                </label>

                <div class="three-field">
                    <label>
                        <span>Price</span>
                        <input type="number" step="0.01" name="price" value="<?= e((string) ($formListing['price'] ?? old('price', '0'))) ?>">
                    </label>
                    <label>
                        <span>Stock Quantity</span>
                        <input type="number" name="stock_quantity" value="<?= e((string) ($formListing['stock_quantity'] ?? old('stock_quantity', '0'))) ?>">
                    </label>
                    <label>
                        <span>SKU</span>
                        <input type="text" name="sku" value="<?= e((string) ($formListing['sku'] ?? old('sku', ''))) ?>">
                    </label>
                </div>

                <div class="three-field">
                    <label>
                        <span>Min Order Qty</span>
                        <input type="number" name="min_order_qty" value="<?= e((string) ($formListing['min_order_qty'] ?? old('min_order_qty', '1'))) ?>">
                    </label>
                    <label>
                        <span>Min Order Amount</span>
                        <input type="number" step="0.01" name="min_order_amount" value="<?= e((string) ($formListing['min_order_amount'] ?? old('min_order_amount', '0'))) ?>">
                    </label>
                    <label>
                        <span>Delivery Time</span>
                        <input type="text" name="delivery_time" value="<?= e((string) ($formListing['delivery_time'] ?? old('delivery_time', '24-48 hours'))) ?>">
                    </label>
                </div>

                <div class="two-field">
                    <label>
                        <span>Status</span>
                        <select name="status">
                            <?php foreach (['active', 'inactive', 'draft'] as $status): ?>
                                <option value="<?= e($status) ?>" <?= (($formListing['status'] ?? 'active') === $status) ? 'selected' : '' ?>><?= e(ucfirst($status)) ?></option>
                            <?php endforeach; ?>
                        </select>
                    </label>
                    <label>
                        <span>Image URL</span>
                        <input type="text" name="image_url" value="<?= e((string) ($formListing['image_url'] ?? old('image_url', ''))) ?>">
                    </label>
                </div>

                <div class="two-field">
                    <label>
                        <span>Upload product image</span>
                        <input type="file" name="product_image" accept="image/png,image/jpeg,image/webp">
                        <small>JPG, PNG, or WEBP. Uploaded file will replace the image URL above.</small>
                    </label>
                    <label>
                        <span>Current image</span>
                        <?php $imageUrl = (string) ($formListing['image_url'] ?? old('image_url', '')); ?>
                        <?php if ($imageUrl !== ''): ?>
                            <div class="media-preview">
                                <img src="<?= e($imageUrl) ?>" alt="Product image preview" style="max-width: 100%; max-height: 180px; border-radius: 16px; object-fit: cover;">
                            </div>
                        <?php else: ?>
                            <div class="notice-box">
                                <p>No product image selected yet.</p>
                            </div>
                        <?php endif; ?>
                    </label>
                </div>

                <label class="check-row">
                    <input type="checkbox" name="is_featured" value="1" <?= ((int) ($formListing['is_featured'] ?? 0) === 1) ? 'checked' : '' ?>>
                    <span>Show this listing on the buyer home screen</span>
                </label>

                <div class="form-actions">
                    <a class="ghost-button" href="<?= e(module_url('products')) ?>">Cancel</a>
                    <button class="primary-button" type="submit"><?= $isEdit ? 'Update Product' : 'Create Product' ?></button>
                </div>
            </form>
        </section>
    </section>
<?php endif; ?>
