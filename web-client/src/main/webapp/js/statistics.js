import {vehicleService} from './api.js';
import {byId, el} from './dom.js';
import {formatNumber} from './format.js';
import {count, showError} from './messages.js';
import {vehicleTable} from './table.js';

// The three statistics of the vehicle service, each shown on its own: one
// failing (max-name answers 404 for an empty collection) does not hide the others.

export function initStatistics() {
    byId('refresh-statistics').addEventListener('click', refreshStatistics);
}

export function refreshStatistics() {
    return Promise.all([
        show('statistics-power', '/vehicles/statistics/engine-power-sum', ({sum, consideredElements}) =>
            el('p', {}, el('span', {class: 'value'}, formatNumber(sum)),
                ` over ${count(consideredElements, 'vehicle')}`)),
        show('statistics-wheels', '/vehicles/statistics/number-of-wheels-average', ({average, consideredElements}) =>
            average === null
                ? el('p', {}, 'No vehicle has the number of wheels set.')
                : el('p', {}, el('span', {class: 'value'}, formatNumber(average)),
                    ` over ${count(consideredElements, 'vehicle')} with the number of wheels set`)),
        show('statistics-max-name', '/vehicles/statistics/max-name', vehicle => vehicleTable([vehicle])),
    ]);
}

async function show(areaId, path, render) {
    const area = byId(areaId);
    try {
        area.replaceChildren(render(await vehicleService.get(path)));
    } catch (error) {
        showError(area, error);
    }
}
