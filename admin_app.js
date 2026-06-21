document.addEventListener('DOMContentLoaded', () => {
    const shell = document.querySelector('[data-sidebar-shell]');
    const toggle = document.querySelector('[data-sidebar-toggle]');
    const asyncSelects = Array.from(document.querySelectorAll('[data-async-select]'));

    if (shell && toggle) {
        toggle.addEventListener('click', () => {
            shell.classList.toggle('sidebar-open');
        });

        document.addEventListener('click', (event) => {
            if (window.innerWidth > 920 || !shell.classList.contains('sidebar-open')) {
                return;
            }

            const insideSidebar = event.target.closest('#sidebar');
            const insideToggle = event.target.closest('[data-sidebar-toggle]');
            if (!insideSidebar && !insideToggle) {
                shell.classList.remove('sidebar-open');
            }
        });
    }

    setTimeout(() => {
        document.querySelectorAll('.alert').forEach((element) => {
            element.style.transition = 'opacity .25s ease, transform .25s ease';
            element.style.opacity = '0';
            element.style.transform = 'translateY(-6px)';
            setTimeout(() => element.remove(), 250);
        });
    }, 4200);

    const closeAsyncSelect = (container) => {
        const panel = container.querySelector('[data-async-select-panel]');
        const trigger = container.querySelector('[data-async-select-trigger]');
        if (panel) {
            panel.hidden = true;
        }
        if (trigger) {
            trigger.setAttribute('aria-expanded', 'false');
        }
        container.classList.remove('is-open');
    };

    asyncSelects.forEach((container) => {
        const hiddenInput = container.querySelector('input[type="hidden"]');
        const trigger = container.querySelector('[data-async-select-trigger]');
        const panel = container.querySelector('[data-async-select-panel]');
        const searchInput = container.querySelector('[data-async-select-search]');
        const results = container.querySelector('[data-async-select-results]');
        const status = container.querySelector('[data-async-select-status]');
        const label = container.querySelector('[data-async-select-label]');
        const meta = container.querySelector('[data-async-select-meta]');
        const clearButton = container.querySelector('[data-async-select-clear]');
        const endpoint = container.dataset.endpoint || '';
        const emptyLabel = container.dataset.emptyLabel || 'Select an option';
        const searchPlaceholder = container.dataset.searchPlaceholder || 'Search';
        const kind = container.dataset.kind || '';

        if (!hiddenInput || !trigger || !panel || !searchInput || !results || !status || endpoint === '') {
            return;
        }

        searchInput.placeholder = searchPlaceholder;

        const state = {
            page: 1,
            query: '',
            loading: false,
            hasMore: true,
            loadedOnce: false,
            debounceTimer: null,
            items: [],
        };

        const escapeHtml = (value) => {
            const div = document.createElement('div');
            div.textContent = value ?? '';
            return div.innerHTML;
        };

        const syncSelection = (item) => {
            const isEmpty = !item || !item.id || Number(item.id) === 0;
            hiddenInput.value = isEmpty ? '0' : String(item.id);
            if (label) {
                label.textContent = isEmpty ? emptyLabel : (item.label || emptyLabel);
            }
            if (meta) {
                meta.textContent = isEmpty
                    ? (kind === 'catalog'
                        ? 'Leave unselected if you want to create a fresh catalog item.'
                        : 'Search supplier records instead of loading the full list.')
                    : (item.meta || '');
            }
            if (kind === 'catalog') {
                const form = container.closest('form');
                if (form && !isEmpty) {
                    const setValue = (name, value) => {
                        const field = form.querySelector(`[name="${name}"]`);
                        if (field && typeof value !== 'undefined' && value !== null) {
                            field.value = String(value);
                        }
                    };

                    setValue('catalog_name', item.name || '');
                    setValue('emoji', item.emoji || '');
                    setValue('description', item.description || '');
                    setValue('packaging', item.packaging || '');
                    setValue('unit_type', item.unit_type || '');
                    setValue('image_url', item.image_url || '');

                    const categoryField = form.querySelector('[name="category_id"]');
                    if (categoryField && item.category_id) {
                        categoryField.value = String(item.category_id);
                    }
                }
            }
        };

        const renderItems = () => {
            if (!state.items.length) {
                results.innerHTML = '<div class="async-select__empty">No matching records found.</div>';
                return;
            }

            const html = state.items.map((item) => `
                <button type="button" class="async-select__option ${String(hiddenInput.value) === String(item.id) ? 'is-selected' : ''}" data-option-id="${escapeHtml(String(item.id))}">
                    <strong>${escapeHtml(item.label || '')}</strong>
                    <small>${escapeHtml(item.meta || '')}</small>
                </button>
            `).join('');

            results.innerHTML = html;
        };

        const setStatus = (message) => {
            status.textContent = message;
        };

        const loadPage = async (page = 1, append = false) => {
            if (state.loading) {
                return;
            }

            state.loading = true;
            setStatus(page === 1 ? 'Loading...' : 'Loading more...');

            try {
                const url = new URL(endpoint, window.location.origin);
                url.searchParams.set('page', String(page));
                url.searchParams.set('limit', '25');
                if (state.query.trim() !== '') {
                    url.searchParams.set('search', state.query.trim());
                }

                const response = await fetch(url.toString(), {
                    credentials: 'same-origin',
                    headers: {
                        Accept: 'application/json',
                    },
                });

                const payload = await response.json();
                const data = payload.data || {};
                const items = Array.isArray(data.items) ? data.items : [];
                const pagination = data.pagination || {};

                state.page = Number(pagination.page || page);
                state.hasMore = Boolean(pagination.has_more);
                state.loadedOnce = true;
                state.items = append ? state.items.concat(items) : items;
                renderItems();

                if (!state.items.length) {
                    setStatus('No results');
                } else if (state.hasMore) {
                    setStatus('Scroll to load more');
                } else {
                    setStatus(`Showing ${state.items.length} result${state.items.length === 1 ? '' : 's'}`);
                }
            } catch (error) {
                setStatus('Unable to load options right now.');
            } finally {
                state.loading = false;
            }
        };

        trigger.addEventListener('click', () => {
            const isOpen = !panel.hidden;
            asyncSelects.forEach((item) => closeAsyncSelect(item));
            if (isOpen) {
                return;
            }
            panel.hidden = false;
            container.classList.add('is-open');
            trigger.setAttribute('aria-expanded', 'true');
            searchInput.focus();

            if (!state.loadedOnce) {
                void loadPage(1, false);
            }
        });

        searchInput.addEventListener('input', () => {
            window.clearTimeout(state.debounceTimer);
            state.debounceTimer = window.setTimeout(() => {
                state.query = searchInput.value.trim();
                state.page = 1;
                state.hasMore = true;
                state.items = [];
                void loadPage(1, false);
            }, 250);
        });

        results.addEventListener('scroll', () => {
            if (!state.hasMore || state.loading) {
                return;
            }

            const nearBottom = results.scrollTop + results.clientHeight >= results.scrollHeight - 60;
            if (nearBottom) {
                void loadPage(state.page + 1, true);
            }
        });

        results.addEventListener('click', (event) => {
            const option = event.target.closest('[data-option-id]');
            if (!option) {
                return;
            }

            const selectedItem = state.items.find((item) => String(item.id) === String(option.dataset.optionId));
            syncSelection(selectedItem || null);
            renderItems();
            closeAsyncSelect(container);
        });

        clearButton?.addEventListener('click', () => {
            syncSelection({ id: 0, label: emptyLabel, meta: '' });
            closeAsyncSelect(container);
        });
    });

    document.addEventListener('click', (event) => {
        asyncSelects.forEach((container) => {
            if (!container.contains(event.target)) {
                closeAsyncSelect(container);
            }
        });
    });
});
