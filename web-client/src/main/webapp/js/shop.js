import {shopService} from './api.js';
import {byId, el} from './dom.js';
import {formatNumber} from './format.js';
import {count, showError, showNotice} from './messages.js';
import {vehicleTable} from './table.js';

// The two operations of the shop service. It answers from the vehicle
// service's data, so its errors include those of the vehicle service
// being unavailable (500).

let onChanged = () => {
};

export function initShop(callbacks) {
    ({onChanged} = callbacks);
    byId('search-form').addEventListener('submit', event => {
        event.preventDefault();
        search(byId('search-type').value);
    });
    byId('fix-form').addEventListener('submit', event => {
        event.preventDefault();
        fixDistance(byId('fix-id').value);
    });
}

async function search(type) {
    const results = byId('search-results');
    try {
        const vehicles = await shopService.get(`/shop/search/by-type/${encodeURIComponent(type)}`);
        results.replaceChildren(el('p', {}, `${count(vehicles.length, 'vehicle')} of type ${type}:`), vehicleTable(vehicles));
    } catch (error) {
        showError(results, error);
    }
}

async function fixDistance(id) {
    const messages = byId('fix-messages');
    try {
        const vehicle = await shopService.post(`/shop/fix-distance/${encodeURIComponent(id)}`);
        showNotice(messages, `Mileage of vehicle #${vehicle.id} is now ${formatNumber(vehicle.mileage, 'km')}.`);
        onChanged();
    } catch (error) {
        showError(messages, error);
    }
}

