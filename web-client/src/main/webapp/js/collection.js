import {shopService, vehicleService} from './api.js';
import {byId, el} from './dom.js';
import {formatNumber} from './format.js';
import {clearMessages, count, showError, showNotice} from './messages.js';
import {COLUMNS, vehicleTable} from './table.js';

// GET /vehicles of the vehicle service: filters, sorting and pagination are
// all done by the service. Whatever is typed is sent as it is; the service
// decides what is valid.

const OPERATORS = [
    ['eq', 'equals'],
    ['neq', 'not equal'],
    ['gt', 'greater than'],
    ['gte', 'greater or equal'],
    ['lt', 'less than'],
    ['lte', 'less or equal'],
    ['like', 'contains'],
    ['in', 'one of a,b,c'],
    ['isnull', 'is null: true/false'],
];

/** Sorting keys in order of priority: [{field, descending}]. */
let sort = [];
let shown = null;
let onChanged = () => {
};
let onEdit = () => {
};

export function initCollection(callbacks) {
    ({onChanged, onEdit} = callbacks);
    byId('add-filter').addEventListener('click', () => addFilter());
    byId('query-form').addEventListener('submit', event => {
        event.preventDefault();
        reloadCollection();
    });
    byId('clear-sort').addEventListener('click', () => {
        sort = [];
        goTo(1);
    });
    byId('previous-page').addEventListener('click', () => goTo(shown.page - 1));
    byId('next-page').addEventListener('click', () => goTo(shown.page + 1));
    renderSortSummary();
}

export async function reloadCollection() {
    const messages = byId('collection-messages');
    const query = buildQuery();
    try {
        shown = await vehicleService.get('/vehicles' + (query === '' ? '' : `?${query}`));
        clearMessages(messages);
        render();
    } catch (error) {
        showError(messages, error);
    }
}

function goTo(page) {
    byId('page').value = String(page);
    reloadCollection();
}

function buildQuery() {
    const params = new URLSearchParams();
    for (const row of byId('filters').querySelectorAll('.filter')) {
        const [field, operator, value] = ['.filter-field', '.filter-operator', '.filter-value']
            .map(selector => row.querySelector(selector).value);
        params.append('filter', `${field}:${operator}:${value}`);
    }
    sort.forEach(key => params.append('sort', (key.descending ? '-' : '') + key.field));
    for (const name of ['page', 'size']) {
        const value = byId(name).value;
        if (value !== '') {
            params.set(name, value);
        }
    }
    return params.toString();
}

function addFilter() {
    const row = el('div', {class: 'filter'},
        el('select', {class: 'filter-field', 'aria-label': 'Field'},
            COLUMNS.map(column => el('option', {value: column.field}, column.field))),
        el('select', {class: 'filter-operator', 'aria-label': 'Operator'},
            OPERATORS.map(([operator, meaning]) => el('option', {value: operator}, `${operator} (${meaning})`))),
        el('input', {class: 'filter-value', 'aria-label': 'Value', placeholder: 'value'}),
        el('button', {type: 'button', class: 'icon', title: 'Remove this filter', onclick: () => row.remove()}, '×'));
    byId('filters').append(row);
    row.querySelector('select').focus();
}

/** Click: ascending → descending → off. Keys keep the order they were added in. */
function toggleSort(field) {
    const index = sort.findIndex(key => key.field === field);
    if (index === -1) {
        sort.push({field, descending: false});
    } else if (!sort[index].descending) {
        sort[index].descending = true;
    } else {
        sort.splice(index, 1);
    }
    renderSortSummary();
    goTo(1);
}

function sortHeader(column) {
    const index = sort.findIndex(key => key.field === column.field);
    const key = sort[index];
    const marker = key === undefined ? '' : `${key.descending ? '▼' : '▲'}${sort.length > 1 ? index + 1 : ''}`;
    return el('button', {
        type: 'button',
        class: 'sort',
        title: `Sort by ${column.field}`,
        onclick: () => toggleSort(column.field),
    }, column.title, marker === '' ? null : el('span', {class: 'sort-marker'}, ` ${marker}`));
}

function renderSortSummary() {
    byId('sort-summary').textContent = sort.length === 0
        ? 'Sorted by id (the service default). Click column titles to sort.'
        : `Sorted by ${sort.map(key => (key.descending ? '-' : '') + key.field).join(', ')}.`;
}

function render() {
    byId('collection-table').replaceChildren(vehicleTable(shown.items, {
        header: sortHeader,
        actions: vehicle => [
            el('button', {type: 'button', onclick: () => onEdit(vehicle)}, 'Edit'),
            el('button', {
                type: 'button',
                title: 'POST /shop/fix-distance (shop service)',
                onclick: () => resetMileage(vehicle)
            }, 'Reset mileage'),
            el('button', {type: 'button', class: 'danger', onclick: () => remove(vehicle)}, 'Delete'),
        ],
    }));
    byId('page-summary').textContent = shown.totalPages === 0
        ? 'No vehicles match.'
        : `Page ${shown.page} of ${shown.totalPages} · ${count(shown.totalElements, 'vehicle')}`;
    byId('previous-page').disabled = shown.page <= 1;
    byId('next-page').disabled = shown.page >= shown.totalPages;
}

async function remove(vehicle) {
    if (!confirm(`Delete vehicle #${vehicle.id} "${vehicle.name}"?`)) {
        return;
    }
    try {
        await vehicleService.delete(`/vehicles/${vehicle.id}`);
        await reloadCollection();
        showNotice(byId('collection-messages'), `Vehicle #${vehicle.id} deleted.`);
        onChanged();
    } catch (error) {
        showError(byId('collection-messages'), error);
    }
}

async function resetMileage(vehicle) {
    try {
        const fixed = await shopService.post(`/shop/fix-distance/${vehicle.id}`);
        await reloadCollection();
        showNotice(byId('collection-messages'),
            `Shop service reset the mileage of vehicle #${fixed.id} to ${formatNumber(fixed.mileage, 'km')}.`);
        onChanged();
    } catch (error) {
        showError(byId('collection-messages'), error);
    }
}
