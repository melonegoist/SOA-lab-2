import {el} from './dom.js';
import {formatDate, formatEnum, formatNumber} from './format.js';

const text = value => value;

/**
 * The columns of a vehicle. {@code field} is the name the vehicle service
 * sorts and filters by; the tooltip of a cell shows the exact value.
 */
export const COLUMNS = [
    {field: 'id', title: 'ID', value: v => v.id, format: text, numeric: true},
    {field: 'name', title: 'Name', value: v => v.name, format: text},
    {field: 'coordinates.x', title: 'X', value: v => v.coordinates.x, format: formatNumber, numeric: true},
    {field: 'coordinates.y', title: 'Y', value: v => v.coordinates.y, format: formatNumber, numeric: true},
    {field: 'creationDate', title: 'Created', value: v => v.creationDate, format: formatDate},
    {field: 'enginePower', title: 'Engine power', value: v => v.enginePower, format: formatNumber, numeric: true},
    {field: 'numberOfWheels', title: 'Wheels', value: v => v.numberOfWheels, format: formatNumber, numeric: true},
    {
        field: 'mileage',
        title: 'Mileage',
        value: v => v.mileage,
        format: value => formatNumber(value, 'km'),
        numeric: true
    },
    {field: 'type', title: 'Type', value: v => v.type, format: formatEnum},
    {field: 'fuelType', title: 'Fuel', value: v => v.fuelType, format: formatEnum},
];

/**
 * @param header  column => content of its header cell (the title by default)
 * @param actions vehicle => buttons for an extra last column (none by default)
 */
export function vehicleTable(vehicles, {header = column => column.title, actions} = {}) {
    const numeric = column => (column.numeric ? 'numeric' : undefined);
    const head = el('tr', {},
        COLUMNS.map(column => el('th', {class: numeric(column), scope: 'col'}, header(column))),
        actions ? el('th', {scope: 'col'}, el('span', {class: 'visually-hidden'}, 'Actions')) : null);
    const rows = vehicles.length === 0
        ? [el('tr', {}, el('td', {class: 'empty', colSpan: COLUMNS.length + (actions ? 1 : 0)}, 'No vehicles.'))]
        : vehicles.map(vehicle => el('tr', {},
            COLUMNS.map(column => {
                const value = column.value(vehicle);
                return el('td', {class: numeric(column), title: String(value)}, column.format(value));
            }),
            actions ? el('td', {class: 'actions'}, actions(vehicle)) : null));
    return el('div', {class: 'table-wrapper'}, el('table', {}, el('thead', {}, head), el('tbody', {}, rows)));
}
