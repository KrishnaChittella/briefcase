/*
 * Briefcase web UI. A small hash-routed single page app with no build step and no
 * dependencies; every screen talks to the same REST API documented in endpoints.md.
 */
(() => {
    'use strict';

    const SESSION_KEY = 'briefcase.session';
    const app = document.getElementById('app');
    const modalRoot = document.getElementById('modal-root');
    const toastEl = document.getElementById('toast');

    // ---------------------------------------------------------------- session

    let session = loadSession();

    function loadSession() {
        try {
            const s = JSON.parse(localStorage.getItem(SESSION_KEY));
            return s && s.expiresAt > Date.now() ? s : null;
        } catch {
            return null;
        }
    }

    function saveSession(auth) {
        session = {
            token: auth.token,
            username: auth.username,
            role: auth.role,
            expiresAt: Date.now() + auth.expiresInMs,
        };
        try { localStorage.setItem(SESSION_KEY, JSON.stringify(session)); } catch { /* private mode */ }
    }

    function logout(message) {
        session = null;
        try { localStorage.removeItem(SESSION_KEY); } catch { /* ignore */ }
        closeModal();
        location.hash = '#/login';
        if (message) toast(message, true);
    }

    // Mirrors com.krishna.briefcase.common.security.Roles so buttons match what the API allows.
    const ADMIN_ROLES = ['SUPER_ADMIN', 'MANAGING_PARTNER'];
    const ATTORNEY_ROLES = [...ADMIN_ROLES, 'SENIOR_ATTORNEY', 'JUNIOR_ATTORNEY'];
    const STAFF_ROLES = [...ATTORNEY_ROLES, 'STAFF'];
    const can = {
        admin: () => ADMIN_ROLES.includes(session?.role),
        attorney: () => ATTORNEY_ROLES.includes(session?.role),
        staff: () => STAFF_ROLES.includes(session?.role),
    };

    // ---------------------------------------------------------------- api

    class ApiError extends Error {
        constructor(status, message) {
            super(message);
            this.status = status;
        }
    }

    async function api(path, { method = 'GET', body, form, blob } = {}) {
        const headers = {};
        if (session) headers.Authorization = `Bearer ${session.token}`;
        if (body !== undefined) headers['Content-Type'] = 'application/json';

        const res = await fetch(path, {
            method,
            headers,
            body: form ?? (body !== undefined ? JSON.stringify(body) : undefined),
        });

        if (res.status === 401 && session && !path.startsWith('/api/auth/')) {
            logout('Your session has expired. Please sign in again.');
            throw new ApiError(401, 'Session expired');
        }
        if (!res.ok) {
            let message = `${res.status} ${res.statusText}`;
            try {
                const err = await res.json();
                message = err.message || message;
                if (err.fieldErrors?.length) {
                    message += ': ' + err.fieldErrors.map(f => `${f.field} ${f.message}`).join(', ');
                }
            } catch { /* non-JSON error body */ }
            if (res.status === 403) message = "You don't have permission to do that.";
            throw new ApiError(res.status, message);
        }
        if (blob) return res.blob();
        if (res.status === 204) return null;
        const text = await res.text();
        return text ? JSON.parse(text) : null;
    }

    /** Spring Data pages serialize either flat or nested under "page", depending on version. */
    function pageOf(data) {
        const meta = data.page ?? data;
        return {
            items: data.content ?? [],
            total: meta.totalElements ?? 0,
            totalPages: Math.max(meta.totalPages ?? 1, 1),
            number: meta.number ?? 0,
        };
    }

    // ---------------------------------------------------------------- helpers

    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    const moneyFmt = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' });
    const money = v => (v === null || v === undefined || v === '') ? '—' : moneyFmt.format(Number(v));
    const date = v => v ? new Date(v).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '—';
    const dateTime = v => v ? new Date(v).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';
    const label = v => esc(String(v ?? '').replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()));
    const badge = (v, cls = v) => `<span class="badge ${esc(cls)}">${label(v)}</span>`;
    const pct = v => v === null || v === undefined ? '—' : `${(Number(v) * 100).toFixed(1).replace(/\.0$/, '')}%`;
    const fileSize = n => n < 1024 ? `${n} B` : n < 1048576 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1048576).toFixed(1)} MB`;
    const options = (values, selected) => values.map(v =>
        `<option value="${esc(v)}" ${v === selected ? 'selected' : ''}>${label(v)}</option>`).join('');

    const BILLING_TYPES = ['CONTINGENCY', 'HOURLY', 'FLAT_FEE'];
    const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
    const NEXT_STATUS = { OPEN: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED', RESOLVED: 'CLOSED' };
    const ROLES = ['SUPER_ADMIN', 'MANAGING_PARTNER', 'SENIOR_ATTORNEY', 'JUNIOR_ATTORNEY', 'PARALEGAL', 'STAFF'];

    function pricing(c) {
        switch (c.billingType) {
            case 'CONTINGENCY': return `${pct(c.contingencyRate)} of settlement`;
            case 'HOURLY': return `${money(c.hourlyRate)}/hr × ${c.loggedHours ?? 0} hrs`;
            case 'FLAT_FEE': return `${money(c.flatFee)} flat`;
            default: return '—';
        }
    }

    let toastTimer;
    function toast(message, isError = false) {
        toastEl.textContent = message;
        toastEl.className = `toast show${isError ? ' error' : ''}`;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toastEl.className = 'toast'; }, 3500);
    }

    /** Turns a form's named fields into an object; blank strings become null. */
    function formData(form) {
        const out = {};
        for (const el of form.elements) {
            if (!el.name) continue;
            if (el.type === 'checkbox') out[el.name] = el.checked;
            else out[el.name] = el.value.trim() === '' ? null : el.value.trim();
        }
        return out;
    }

    // ---------------------------------------------------------------- modal

    function closeModal() {
        modalRoot.innerHTML = '';
    }

    /**
     * Opens a form in a modal. onSubmit receives the field values; throwing shows the
     * message inside the modal and keeps it open, returning closes it.
     */
    function openForm(title, fieldsHtml, onSubmit, { submitLabel = 'Save', afterRender } = {}) {
        modalRoot.innerHTML = `
            <div class="modal-backdrop">
                <div class="card modal" role="dialog" aria-modal="true" aria-label="${esc(title)}">
                    <h2>${esc(title)}</h2>
                    <form class="form" novalidate>
                        ${fieldsHtml}
                        <div class="alert" data-error></div>
                        <div class="form-actions">
                            <button type="button" class="btn" data-cancel>Cancel</button>
                            <button type="submit" class="btn primary">${esc(submitLabel)}</button>
                        </div>
                    </form>
                </div>
            </div>`;
        const backdrop = modalRoot.firstElementChild;
        const form = modalRoot.querySelector('form');
        const errorBox = form.querySelector('[data-error]');
        const submit = form.querySelector('[type=submit]');

        backdrop.addEventListener('click', e => { if (e.target === backdrop) closeModal(); });
        form.querySelector('[data-cancel]').addEventListener('click', closeModal);
        form.addEventListener('submit', async e => {
            e.preventDefault();
            errorBox.textContent = '';
            submit.disabled = true;
            try {
                await onSubmit(formData(form), form);
                closeModal();
            } catch (err) {
                errorBox.textContent = err.message;
            } finally {
                submit.disabled = false;
            }
        });
        afterRender?.(form);
        form.querySelector('input, select, textarea')?.focus();
    }

    async function confirmAction(message, action) {
        if (!window.confirm(message)) return;
        try {
            await action();
        } catch (err) {
            toast(err.message, true);
        }
    }

    // ---------------------------------------------------------------- router

    const routes = [
        { pattern: /^#\/login$/, page: renderLogin, public: true },
        { pattern: /^#\/dashboard$/, page: renderDashboard, nav: 'dashboard' },
        { pattern: /^#\/cases$/, page: renderCases, nav: 'cases' },
        { pattern: /^#\/cases\/(\d+)$/, page: renderCaseDetail, nav: 'cases' },
        { pattern: /^#\/clients$/, page: renderClients, nav: 'clients' },
        { pattern: /^#\/attorneys$/, page: renderAttorneys, nav: 'attorneys' },
        { pattern: /^#\/leaderboard$/, page: renderLeaderboard, nav: 'leaderboard' },
    ];

    let renderSeq = 0;

    async function route() {
        const hash = location.hash || '#/dashboard';
        const match = routes.map(r => ({ r, m: hash.match(r.pattern) })).find(x => x.m);
        if (!match) { location.hash = '#/dashboard'; return; }
        if (!match.r.public && !session) { location.hash = '#/login'; return; }
        if (match.r.public && session) { location.hash = '#/dashboard'; return; }

        closeModal();
        const seq = ++renderSeq;
        // Each page renders through this guard so a slow response can't overwrite a newer page.
        const view = {
            stale: () => seq !== renderSeq,
            shell: html => { if (seq === renderSeq) renderShell(match.r.nav, html); },
        };
        try {
            await match.r.page(view, ...match.m.slice(1));
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) return;
            view.shell(`<div class="card"><h2>Something went wrong</h2><p class="muted">${esc(err.message)}</p></div>`);
        }
    }

    window.addEventListener('hashchange', route);

    function renderShell(active, content) {
        const link = (key, text) => `<a href="#/${key}" class="${active === key ? 'active' : ''}">${text}</a>`;
        app.innerHTML = `
            <div class="shell">
                <aside class="sidebar">
                    <div class="brand"><img src="/favicon.svg" alt="">Briefcase</div>
                    <nav class="nav">
                        ${link('dashboard', 'Dashboard')}
                        ${link('cases', 'Cases')}
                        ${link('clients', 'Clients')}
                        ${link('attorneys', 'Attorneys')}
                        ${link('leaderboard', 'Leaderboard')}
                    </nav>
                    <div class="me">
                        <strong>${esc(session.username)}</strong>
                        <span class="muted">${label(session.role)}</span><br>
                        <a href="#" data-logout>Sign out</a>
                    </div>
                </aside>
                <main>${content}</main>
            </div>`;
        app.querySelector('[data-logout]').addEventListener('click', e => { e.preventDefault(); logout(); });
    }

    const loading = '<div class="card empty">Loading…</div>';

    // ---------------------------------------------------------------- login

    function renderLogin() {
        app.innerHTML = `
            <div class="auth-page">
                <div class="card auth-card">
                    <div class="brand"><img src="/favicon.svg" alt="">Briefcase</div>
                    <p class="tagline">Cases, billing and fee splits for your firm</p>
                    <div class="tabs">
                        <button type="button" data-tab="login" class="active">Sign in</button>
                        <button type="button" data-tab="register">Create account</button>
                    </div>
                    <form class="form" data-form="login">
                        <label class="field">Username<input name="username" autocomplete="username" required></label>
                        <label class="field">Password<input name="password" type="password" autocomplete="current-password" required></label>
                        <div class="alert" data-error></div>
                        <button class="btn primary" type="submit">Sign in</button>
                    </form>
                    <form class="form" data-form="register" hidden>
                        <label class="field">Username<span class="hint">3 to 100 characters</span><input name="username" autocomplete="username" required></label>
                        <label class="field">Password<span class="hint">At least 6 characters</span><input name="password" type="password" autocomplete="new-password" required></label>
                        <label class="field">Role<select name="role">${options(ROLES, 'STAFF')}</select></label>
                        <div class="alert" data-error></div>
                        <button class="btn primary" type="submit">Create account</button>
                    </form>
                </div>
            </div>`;

        const tabs = app.querySelectorAll('[data-tab]');
        tabs.forEach(tab => tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.toggle('active', t === tab));
            app.querySelectorAll('[data-form]').forEach(f => { f.hidden = f.dataset.form !== tab.dataset.tab; });
        }));

        app.querySelectorAll('[data-form]').forEach(form => form.addEventListener('submit', async e => {
            e.preventDefault();
            const errorBox = form.querySelector('[data-error]');
            const button = form.querySelector('[type=submit]');
            errorBox.textContent = '';
            button.disabled = true;
            try {
                const auth = await api(`/api/auth/${form.dataset.form}`, { method: 'POST', body: formData(form) });
                saveSession(auth);
                location.hash = '#/dashboard';
            } catch (err) {
                errorBox.textContent = err.status === 401 ? 'Wrong username or password.' : err.message;
            } finally {
                button.disabled = false;
            }
        }));
        app.querySelector('input').focus();
    }

    // ---------------------------------------------------------------- dashboard

    async function renderDashboard(view) {
        view.shell(`<div class="page-head"><h1>Dashboard</h1></div>${loading}`);

        const counts = await Promise.all(STATUSES.map(s =>
            api(`/api/cases?status=${s}&size=1`).then(d => pageOf(d).total)));
        const [recent, leaders] = await Promise.all([
            api('/api/cases?size=5&sort=id,desc').then(pageOf),
            api('/api/leaderboard?metric=REVENUE&period=ALL_TIME'),
        ]);
        if (view.stale()) return;

        view.shell(`
            <div class="page-head">
                <div><h1>Dashboard</h1><div class="sub muted">Welcome back, ${esc(session.username)}.</div></div>
                ${can.staff() ? '<a class="btn primary" href="#/cases" data-new-case>New case</a>' : ''}
            </div>
            <div class="grid grid-4 stats">
                ${STATUSES.map((s, i) => `
                    <a class="card stat" href="#/cases" data-status="${s}">
                        <div class="label">${label(s)}</div>
                        <div class="value">${counts[i]}</div>
                    </a>`).join('')}
            </div>
            <div class="grid grid-2" style="margin-top:16px">
                <div class="card">
                    <div class="card-head"><h2>Latest cases</h2><a href="#/cases">View all</a></div>
                    ${recent.items.length ? `
                        <table><tbody>
                            ${recent.items.map(c => `
                                <tr class="clickable" data-href="#/cases/${c.id}">
                                    <td><strong>${esc(c.title)}</strong><br><span class="muted mono">${esc(c.caseNumber)}</span></td>
                                    <td class="actions">${badge(c.status)}</td>
                                </tr>`).join('')}
                        </tbody></table>` : '<div class="empty">No cases yet.</div>'}
                </div>
                <div class="card">
                    <div class="card-head"><h2>Top earners, all time</h2><a href="#/leaderboard">Leaderboard</a></div>
                    ${leaders.length ? `
                        <table><tbody>
                            ${leaders.slice(0, 5).map(r => `
                                <tr><td>${r.rank}. ${esc(r.attorneyName)}</td><td class="num">${money(r.score)}</td></tr>`).join('')}
                        </tbody></table>` : '<div class="empty">No fees paid out yet.</div>'}
                </div>
            </div>`);

        app.querySelectorAll('[data-status]').forEach(a => a.addEventListener('click', () => {
            casesFilter = { ...casesFilter, status: a.dataset.status, page: 0 };
        }));
        app.querySelector('[data-new-case]')?.addEventListener('click', () => { openNewCaseOnLoad = true; });
        bindRowLinks();
    }

    function bindRowLinks() {
        app.querySelectorAll('tr[data-href]').forEach(tr =>
            tr.addEventListener('click', () => { location.hash = tr.dataset.href; }));
    }

    // ---------------------------------------------------------------- cases

    let casesFilter = { q: '', status: '', billingType: '', page: 0 };
    let openNewCaseOnLoad = false;

    async function renderCases(view) {
        const f = casesFilter;
        const params = new URLSearchParams({ page: f.page, size: 20, sort: 'id,desc' });
        if (f.q) params.set('q', f.q);
        if (f.status) params.set('status', f.status);
        if (f.billingType) params.set('billingType', f.billingType);

        const page = pageOf(await api(`/api/cases?${params}`));
        if (view.stale()) return;

        view.shell(`
            <div class="page-head">
                <div><h1>Cases</h1><div class="sub muted">${page.total} ${page.total === 1 ? 'case' : 'cases'}</div></div>
                ${can.staff() ? '<button class="btn primary" data-new>New case</button>' : ''}
            </div>
            <div class="card">
                <form class="toolbar" data-filter>
                    <input name="q" placeholder="Search title or number" value="${esc(f.q)}">
                    <select name="status"><option value="">All statuses</option>${options(STATUSES, f.status)}</select>
                    <select name="billingType"><option value="">All billing types</option>${options(BILLING_TYPES, f.billingType)}</select>
                    <button class="btn" type="submit">Filter</button>
                </form>
                ${page.items.length ? `
                    <div class="table-wrap"><table>
                        <thead><tr><th>Number</th><th>Title</th><th>Client</th><th>Billing</th><th>Status</th><th>Opened</th></tr></thead>
                        <tbody>
                            ${page.items.map(c => `
                                <tr class="clickable" data-href="#/cases/${c.id}">
                                    <td class="mono">${esc(c.caseNumber)}</td>
                                    <td><strong>${esc(c.title)}</strong></td>
                                    <td>${esc(c.clientName)}</td>
                                    <td>${label(c.billingType)}</td>
                                    <td>${badge(c.status)}</td>
                                    <td>${date(c.openedAt)}</td>
                                </tr>`).join('')}
                        </tbody>
                    </table></div>
                    <div class="pager">
                        <span class="muted">Page ${page.number + 1} of ${page.totalPages}</span>
                        <button class="btn small" data-page="-1" ${page.number === 0 ? 'disabled' : ''}>Previous</button>
                        <button class="btn small" data-page="1" ${page.number + 1 >= page.totalPages ? 'disabled' : ''}>Next</button>
                    </div>` : '<div class="empty">No cases match these filters.</div>'}
            </div>`);

        app.querySelector('[data-filter]').addEventListener('submit', e => {
            e.preventDefault();
            const v = formData(e.target);
            casesFilter = { q: v.q ?? '', status: v.status ?? '', billingType: v.billingType ?? '', page: 0 };
            route();
        });
        app.querySelectorAll('[data-page]').forEach(b => b.addEventListener('click', () => {
            casesFilter.page += Number(b.dataset.page);
            route();
        }));
        app.querySelector('[data-new]')?.addEventListener('click', openCaseForm);
        bindRowLinks();

        if (openNewCaseOnLoad && can.staff()) {
            openNewCaseOnLoad = false;
            openCaseForm();
        }
    }

    /** Shows only the pricing inputs the chosen billing type needs. */
    function bindBillingFields(form) {
        const select = form.querySelector('[name=billingType]');
        const sync = () => form.querySelectorAll('[data-billing]').forEach(el => {
            el.hidden = el.dataset.billing !== select.value;
        });
        select.addEventListener('change', sync);
        sync();
    }

    function billingFieldsHtml(c = {}) {
        return `
            <label class="field">Billing type<select name="billingType">${options(BILLING_TYPES, c.billingType ?? 'HOURLY')}</select></label>
            <label class="field" data-billing="CONTINGENCY">Contingency rate<span class="hint">Fraction of the settlement, e.g. 0.33 for 33%</span>
                <input name="contingencyRate" type="number" step="0.0001" min="0" max="1" value="${esc(c.contingencyRate)}"></label>
            <div class="form-row" data-billing="HOURLY">
                <label class="field">Hourly rate (USD)<input name="hourlyRate" type="number" step="0.01" min="0" value="${esc(c.hourlyRate)}"></label>
                <label class="field">Logged hours<input name="loggedHours" type="number" step="0.25" min="0" value="${esc(c.loggedHours)}"></label>
            </div>
            <label class="field" data-billing="FLAT_FEE">Flat fee (USD)<input name="flatFee" type="number" step="0.01" min="0" value="${esc(c.flatFee)}"></label>`;
    }

    /** Drops pricing fields that don't belong to the chosen billing type. */
    function pricingFor(v) {
        return {
            contingencyRate: v.billingType === 'CONTINGENCY' ? v.contingencyRate : null,
            hourlyRate: v.billingType === 'HOURLY' ? v.hourlyRate : null,
            loggedHours: v.billingType === 'HOURLY' ? v.loggedHours : null,
            flatFee: v.billingType === 'FLAT_FEE' ? v.flatFee : null,
        };
    }

    async function openCaseForm() {
        const [clients, attorneys] = await Promise.all([
            api('/api/clients'),
            can.attorney() ? api('/api/attorneys') : Promise.resolve([]),
        ]).catch(err => { toast(err.message, true); return [null, null]; });
        if (!clients) return;
        if (!clients.length) {
            toast('Add a client first, then open a case for them.', true);
            return;
        }

        const leadField = can.attorney() ? `
            <label class="field">Lead attorney<span class="hint">Optional. Picking one opens the case and assigns the lead in one step.</span>
                <select name="leadAttorneyId"><option value="">Assign later</option>
                    ${attorneys.filter(a => a.active).map(a => `<option value="${a.id}">${esc(a.firstName)} ${esc(a.lastName)}</option>`).join('')}
                </select></label>` : '';

        openForm('New case', `
            <div class="form-row">
                <label class="field">Case number<input name="caseNumber" maxlength="50" required></label>
                <label class="field">Client<select name="clientId">${clients.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('')}</select></label>
            </div>
            <label class="field">Title<input name="title" maxlength="255" required></label>
            <label class="field">Description<textarea name="description"></textarea></label>
            ${billingFieldsHtml()}
            ${leadField}`,
        async v => {
            const body = {
                caseNumber: v.caseNumber, title: v.title, description: v.description,
                clientId: Number(v.clientId), billingType: v.billingType, ...pricingFor(v),
            };
            const created = v.leadAttorneyId
                ? await api('/api/cases/onboard', { method: 'POST', body: { ...body, leadAttorneyId: Number(v.leadAttorneyId) } })
                : await api('/api/cases', { method: 'POST', body });
            toast('Case opened.');
            location.hash = `#/cases/${created.id}`;
        }, { submitLabel: 'Open case', afterRender: bindBillingFields });
    }

    async function renderCaseDetail(view, id) {
        view.shell(loading);
        const c = await api(`/api/cases/${id}`);
        const optional = p => p.catch(err => (err.status === 404 ? null : Promise.reject(err)));
        const [team, documents, outcome, fees, audit, attorneys] = await Promise.all([
            api(`/api/cases/${id}/assignments`),
            api(`/api/cases/${id}/documents`),
            optional(api(`/api/cases/${id}/outcome`)),
            c.financiallyResolved ? api(`/api/cases/${id}/fee-distributions`) : Promise.resolve([]),
            can.admin() ? api(`/api/cases/${id}/audit-log`) : Promise.resolve(null),
            can.attorney() ? api('/api/attorneys') : Promise.resolve([]),
        ]);
        if (view.stale()) return;

        const next = NEXT_STATUS[c.status];
        const assignedIds = new Set(team.map(t => t.attorneyId));
        const available = attorneys.filter(a => a.active && !assignedIds.has(a.id));
        const reload = () => route();

        view.shell(`
            <div class="page-head">
                <div>
                    <a href="#/cases" class="muted">← All cases</a>
                    <h1 style="margin-top:6px">${esc(c.title)} ${badge(c.status)}</h1>
                    <div class="sub muted mono">${esc(c.caseNumber)}</div>
                </div>
                <div style="display:flex;gap:10px;flex-wrap:wrap">
                    ${can.attorney() ? '<button class="btn" data-edit>Edit</button>' : ''}
                    ${can.attorney() && next ? `<button class="btn primary" data-advance>Move to ${label(next)}</button>` : ''}
                    ${can.admin() ? '<button class="btn danger" data-delete>Delete</button>' : ''}
                </div>
            </div>

            <div class="grid grid-2">
                <div class="card">
                    <div class="card-head"><h2>Details</h2></div>
                    <dl class="facts">
                        <dt>Client</dt><dd>${esc(c.clientName)}</dd>
                        <dt>Billing</dt><dd>${label(c.billingType)}</dd>
                        <dt>Pricing</dt><dd>${pricing(c)}</dd>
                        <dt>Opened</dt><dd>${date(c.openedAt)}</dd>
                        <dt>Resolved</dt><dd>${date(c.resolvedAt)}</dd>
                        <dt>Closed</dt><dd>${date(c.closedAt)}</dd>
                        <dt>Description</dt><dd>${esc(c.description) || '<span class="muted">None</span>'}</dd>
                    </dl>
                </div>

                <div class="card">
                    <div class="card-head"><h2>Team</h2></div>
                    ${team.length ? `
                        <table><tbody>
                            ${team.map(t => `
                                <tr>
                                    <td>${esc(t.attorneyName)}</td>
                                    <td>${badge(t.assignmentRole, t.assignmentRole === 'LEAD' ? 'LEAD' : 'neutral')}</td>
                                    <td class="actions">${can.attorney() && !c.financiallyResolved
                                        ? `<button class="btn small danger" data-unassign="${t.attorneyId}">Remove</button>` : ''}</td>
                                </tr>`).join('')}
                        </tbody></table>` : '<div class="empty">No one is assigned yet.</div>'}
                    ${can.attorney() && !c.financiallyResolved && available.length ? `
                        <form class="inline-form" data-assign>
                            <select name="attorneyId">${available.map(a => `<option value="${a.id}">${esc(a.firstName)} ${esc(a.lastName)}</option>`).join('')}</select>
                            <select name="assignmentRole">${options(['LEAD', 'ASSOCIATE', 'PARALEGAL'], team.some(t => t.assignmentRole === 'LEAD') ? 'ASSOCIATE' : 'LEAD')}</select>
                            <button class="btn" type="submit">Assign</button>
                        </form>` : ''}
                </div>
            </div>

            <div class="card" style="margin-top:16px">
                <div class="card-head"><h2>Outcome and fees</h2></div>
                ${outcomeHtml(c, outcome, fees)}
            </div>

            <div class="card">
                <div class="card-head"><h2>Documents</h2></div>
                ${documents.length ? `
                    <div class="table-wrap"><table>
                        <thead><tr><th>File</th><th>Size</th><th>Uploaded by</th><th>Uploaded</th><th></th></tr></thead>
                        <tbody>
                            ${documents.map(d => `
                                <tr>
                                    <td>${esc(d.fileName)}</td>
                                    <td>${fileSize(d.sizeBytes)}</td>
                                    <td>${esc(d.uploadedBy)}</td>
                                    <td>${date(d.uploadedAt)}</td>
                                    <td class="actions"><button class="btn small" data-download="${d.id}" data-name="${esc(d.fileName)}">Download</button></td>
                                </tr>`).join('')}
                        </tbody>
                    </table></div>` : '<div class="empty">No documents uploaded.</div>'}
                ${can.staff() ? `
                    <form class="inline-form" data-upload>
                        <input type="file" name="file" required>
                        <button class="btn" type="submit">Upload</button>
                    </form>` : ''}
            </div>

            ${audit ? `
                <div class="card">
                    <div class="card-head"><h2>Audit log</h2></div>
                    ${audit.length ? `
                        <div class="table-wrap"><table>
                            <thead><tr><th>When</th><th>Action</th><th>By</th><th>Details</th></tr></thead>
                            <tbody>
                                ${audit.map(a => `
                                    <tr>
                                        <td>${dateTime(a.occurredAt)}</td>
                                        <td>${label(a.action)}</td>
                                        <td>${esc(a.performedBy)}</td>
                                        <td class="muted">${esc(a.details)}</td>
                                    </tr>`).join('')}
                            </tbody>
                        </table></div>` : '<div class="empty">No changes recorded yet.</div>'}
                </div>` : ''}`);

        app.querySelector('[data-edit]')?.addEventListener('click', () => openEditCase(c));
        app.querySelector('[data-advance]')?.addEventListener('click', () =>
            confirmAction(`Move this case to ${label(next)}?`, async () => {
                await api(`/api/cases/${id}/status`, { method: 'PATCH', body: { status: next } });
                toast(`Case moved to ${label(next)}.`);
                reload();
            }));
        app.querySelector('[data-delete]')?.addEventListener('click', () =>
            confirmAction('Delete this case permanently?', async () => {
                await api(`/api/cases/${id}`, { method: 'DELETE' });
                toast('Case deleted.');
                location.hash = '#/cases';
            }));

        app.querySelectorAll('[data-unassign]').forEach(b => b.addEventListener('click', () =>
            confirmAction('Remove this attorney from the case?', async () => {
                await api(`/api/cases/${id}/assignments/${b.dataset.unassign}`, { method: 'DELETE' });
                reload();
            })));
        app.querySelector('[data-assign]')?.addEventListener('submit', async e => {
            e.preventDefault();
            const v = formData(e.target);
            try {
                await api(`/api/cases/${id}/assignments`, {
                    method: 'POST', body: { attorneyId: Number(v.attorneyId), assignmentRole: v.assignmentRole },
                });
                reload();
            } catch (err) { toast(err.message, true); }
        });

        app.querySelector('[data-upload]')?.addEventListener('submit', async e => {
            e.preventDefault();
            const input = e.target.querySelector('[type=file]');
            if (!input.files.length) return;
            const form = new FormData();
            form.append('file', input.files[0]);
            try {
                await api(`/api/cases/${id}/documents`, { method: 'POST', form });
                toast('Document uploaded.');
                reload();
            } catch (err) { toast(err.message, true); }
        });
        app.querySelectorAll('[data-download]').forEach(b => b.addEventListener('click', async () => {
            try {
                const file = await api(`/api/cases/${id}/documents/${b.dataset.download}/download`, { blob: true });
                const url = URL.createObjectURL(file);
                const a = Object.assign(document.createElement('a'), { href: url, download: b.dataset.name });
                a.click();
                setTimeout(() => URL.revokeObjectURL(url), 1000);
            } catch (err) { toast(err.message, true); }
        }));

        app.querySelector('[data-outcome]')?.addEventListener('submit', async e => {
            e.preventDefault();
            const v = formData(e.target);
            try {
                await api(`/api/cases/${id}/outcome`, {
                    method: 'POST', body: { result: v.result, settlementAmount: v.settlementAmount },
                });
                toast('Outcome recorded and fees distributed.');
                reload();
            } catch (err) { toast(err.message, true); }
        });
    }

    function outcomeHtml(c, outcome, fees) {
        if (outcome) {
            return `
                <div class="grid grid-4" style="margin-bottom:16px">
                    <div class="stat"><div class="label">Result</div><div class="value">${badge(outcome.result)}</div></div>
                    <div class="stat"><div class="label">Gross revenue</div><div class="value">${money(outcome.grossRevenue)}</div></div>
                    <div class="stat"><div class="label">Firm cut</div><div class="value">${money(outcome.firmCut)}</div></div>
                    <div class="stat"><div class="label">To attorneys</div><div class="value">${money(outcome.netDistributable)}</div></div>
                </div>
                <p class="muted">Recorded by ${esc(outcome.recordedBy)} on ${date(outcome.recordedAt)}${
                    outcome.settlementAmount ? `, settlement ${money(outcome.settlementAmount)}` : ''}.</p>
                ${fees.length ? `
                    <table>
                        <thead><tr><th>Attorney</th><th>Share</th><th class="num">Amount</th></tr></thead>
                        <tbody>${fees.map(f => `
                            <tr><td>${esc(f.attorneyName)}</td><td>${label(f.shareType)}</td><td class="num">${money(f.amount)}</td></tr>`).join('')}
                        </tbody>
                    </table>` : ''}`;
        }
        if (c.status !== 'RESOLVED') {
            return `<div class="empty">Move the case to Resolved to record its outcome and pay out fees.</div>`;
        }
        if (!can.attorney()) {
            return `<div class="empty">Waiting for an attorney to record the outcome.</div>`;
        }
        return `
            <p class="muted">Recording the outcome is final: revenue is calculated once and split among the team.</p>
            <form class="inline-form" data-outcome>
                <label class="field">Result<select name="result">${options(['WON', 'SETTLED', 'LOST'], 'WON')}</select></label>
                ${c.billingType === 'CONTINGENCY' ? `
                    <label class="field">Settlement amount (USD)<input name="settlementAmount" type="number" step="0.01" min="0" required></label>` : ''}
                <button class="btn primary" type="submit">Record outcome</button>
            </form>`;
    }

    function openEditCase(c) {
        openForm('Edit case', `
            <label class="field">Title<input name="title" maxlength="255" value="${esc(c.title)}" required></label>
            <label class="field">Description<textarea name="description">${esc(c.description)}</textarea></label>
            ${billingFieldsHtml(c)}`,
        async v => {
            await api(`/api/cases/${c.id}`, {
                method: 'PUT',
                body: {
                    caseNumber: c.caseNumber, clientId: c.clientId, title: v.title,
                    description: v.description, billingType: v.billingType, ...pricingFor(v),
                },
            });
            toast('Case updated.');
            route();
        }, { afterRender: bindBillingFields });
    }

    // ---------------------------------------------------------------- clients

    async function renderClients(view) {
        const clients = await api('/api/clients');
        if (view.stale()) return;

        view.shell(`
            <div class="page-head">
                <div><h1>Clients</h1><div class="sub muted">${clients.length} on file</div></div>
                ${can.staff() ? '<button class="btn primary" data-new>Add client</button>' : ''}
            </div>
            <div class="card">
                ${clients.length ? `
                    <div class="table-wrap"><table>
                        <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Address</th><th></th></tr></thead>
                        <tbody>
                            ${clients.map(c => `
                                <tr>
                                    <td><strong>${esc(c.name)}</strong></td>
                                    <td>${esc(c.email) || '—'}</td>
                                    <td>${esc(c.phone) || '—'}</td>
                                    <td>${esc(c.address) || '—'}</td>
                                    <td class="actions">
                                        ${can.staff() ? `<button class="btn small" data-edit="${c.id}">Edit</button>` : ''}
                                        ${can.admin() ? `<button class="btn small danger" data-delete="${c.id}">Delete</button>` : ''}
                                    </td>
                                </tr>`).join('')}
                        </tbody>
                    </table></div>` : '<div class="empty">No clients yet.</div>'}
            </div>`);

        const byId = Object.fromEntries(clients.map(c => [c.id, c]));
        app.querySelector('[data-new]')?.addEventListener('click', () => openClientForm());
        app.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openClientForm(byId[b.dataset.edit])));
        app.querySelectorAll('[data-delete]').forEach(b => b.addEventListener('click', () =>
            confirmAction(`Delete ${byId[b.dataset.delete].name}?`, async () => {
                await api(`/api/clients/${b.dataset.delete}`, { method: 'DELETE' });
                toast('Client deleted.');
                route();
            })));
    }

    function openClientForm(c = {}) {
        openForm(c.id ? 'Edit client' : 'Add client', `
            <label class="field">Name<input name="name" maxlength="200" value="${esc(c.name)}" required></label>
            <div class="form-row">
                <label class="field">Email<input name="email" type="email" maxlength="255" value="${esc(c.email)}"></label>
                <label class="field">Phone<input name="phone" maxlength="50" value="${esc(c.phone)}"></label>
            </div>
            <label class="field">Address<input name="address" maxlength="500" value="${esc(c.address)}"></label>`,
        async v => {
            await api(c.id ? `/api/clients/${c.id}` : '/api/clients', { method: c.id ? 'PUT' : 'POST', body: v });
            toast(c.id ? 'Client updated.' : 'Client added.');
            route();
        });
    }

    // ---------------------------------------------------------------- attorneys

    async function renderAttorneys(view) {
        const attorneys = await api('/api/attorneys');
        if (view.stale()) return;

        view.shell(`
            <div class="page-head">
                <div><h1>Attorneys</h1><div class="sub muted">${attorneys.filter(a => a.active).length} active</div></div>
                ${can.admin() ? '<button class="btn primary" data-new>Add attorney</button>' : ''}
            </div>
            <div class="card">
                ${attorneys.length ? `
                    <div class="table-wrap"><table>
                        <thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Login linked</th><th>Since</th><th></th></tr></thead>
                        <tbody>
                            ${attorneys.map(a => `
                                <tr>
                                    <td><strong>${esc(a.firstName)} ${esc(a.lastName)}</strong></td>
                                    <td>${esc(a.email)}</td>
                                    <td>${a.active ? badge('Active', 'RESOLVED') : badge('Inactive', 'CLOSED')}</td>
                                    <td>${a.userId ? `User #${a.userId}` : '<span class="muted">No</span>'}</td>
                                    <td>${date(a.createdAt)}</td>
                                    <td class="actions">
                                        ${can.admin() ? `
                                            <button class="btn small" data-edit="${a.id}">Edit</button>
                                            <button class="btn small danger" data-delete="${a.id}">Delete</button>` : ''}
                                    </td>
                                </tr>`).join('')}
                        </tbody>
                    </table></div>` : '<div class="empty">No attorneys yet.</div>'}
            </div>`);

        const byId = Object.fromEntries(attorneys.map(a => [a.id, a]));
        app.querySelector('[data-new]')?.addEventListener('click', () => openAttorneyForm());
        app.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openAttorneyForm(byId[b.dataset.edit])));
        app.querySelectorAll('[data-delete]').forEach(b => b.addEventListener('click', () => {
            const a = byId[b.dataset.delete];
            confirmAction(`Delete ${a.firstName} ${a.lastName}?`, async () => {
                await api(`/api/attorneys/${a.id}`, { method: 'DELETE' });
                toast('Attorney deleted.');
                route();
            });
        }));
    }

    function openAttorneyForm(a = { active: true }) {
        openForm(a.id ? 'Edit attorney' : 'Add attorney', `
            <div class="form-row">
                <label class="field">First name<input name="firstName" maxlength="100" value="${esc(a.firstName)}" required></label>
                <label class="field">Last name<input name="lastName" maxlength="100" value="${esc(a.lastName)}" required></label>
            </div>
            <label class="field">Email<input name="email" type="email" maxlength="255" value="${esc(a.email)}" required></label>
            <label class="field">Login user ID<span class="hint">Optional. Links this attorney to a user account so they can see their own profile.</span>
                <input name="userId" type="number" min="1" value="${esc(a.userId)}"></label>
            <label class="field" style="flex-direction:row;align-items:center;gap:8px">
                <input name="active" type="checkbox" ${a.active ? 'checked' : ''}> Active</label>`,
        async v => {
            const body = { ...v, userId: v.userId ? Number(v.userId) : null };
            await api(a.id ? `/api/attorneys/${a.id}` : '/api/attorneys', { method: a.id ? 'PUT' : 'POST', body });
            toast(a.id ? 'Attorney updated.' : 'Attorney added.');
            route();
        });
    }

    // ---------------------------------------------------------------- leaderboard

    let board = { metric: 'REVENUE', period: 'ALL_TIME' };

    async function renderLeaderboard(view) {
        const rows = await api(`/api/leaderboard?metric=${board.metric}&period=${board.period}`);
        if (view.stale()) return;

        const score = {
            REVENUE: s => money(s),
            WIN_RATE: s => `${Number(s).toFixed(1)}%`,
            CASES_LED: s => Number(s).toLocaleString(),
        }[board.metric];

        view.shell(`
            <div class="page-head"><h1>Leaderboard</h1></div>
            <div class="card">
                <form class="toolbar" data-board>
                    <select name="metric">${options(['REVENUE', 'WIN_RATE', 'CASES_LED'], board.metric)}</select>
                    <select name="period">${options(['MONTH', 'QUARTER', 'YEAR', 'ALL_TIME'], board.period)}</select>
                </form>
                ${rows.length ? `
                    <table>
                        <thead><tr><th style="width:70px">Rank</th><th>Attorney</th><th class="num">${label(board.metric)}</th></tr></thead>
                        <tbody>${rows.map(r => `
                            <tr><td><strong>${r.rank}</strong></td><td>${esc(r.attorneyName)}</td><td class="num">${score(r.score)}</td></tr>`).join('')}
                        </tbody>
                    </table>` : '<div class="empty">No results for this period yet.</div>'}
            </div>`);

        app.querySelectorAll('[data-board] select').forEach(s => s.addEventListener('change', () => {
            board = formData(app.querySelector('[data-board]'));
            route();
        }));
    }

    route();
})();
