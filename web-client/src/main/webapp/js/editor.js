import {ServiceError, vehicleService} from './api.js';
import {byId} from './dom.js';
import {formatDate} from './format.js';
import {clearMessages, showError, showNotice} from './messages.js';

// Create (POST), open (GET by id) and replace (PUT) a vehicle. There is no
// validation in the browser on purpose: the form sends exactly what was
// typed, and the errors shown are those of the vehicle service.

/** The vehicle being edited as the service returned it; null for a new one. */
let editing = null;
let onSaved = () => {
};

const messages = () => byId('editor-messages');
const input = name => byId(`vehicle-${name}`);

export function initEditor(callbacks) {
    ({onSaved} = callbacks);
    byId('vehicle-form').addEventListener('submit', event => {
        event.preventDefault();
        save();
    });
    byId('new-vehicle').addEventListener('click', () => {
        edit(null);
        input('name').focus();
    });
    byId('open-form').addEventListener('submit', event => {
        event.preventDefault();
        open(byId('open-id').value);
    });
    edit(null);
}

export function edit(vehicle) {
    editing = vehicle;
    clearFieldErrors();
    clearMessages(messages());
    byId('editor-title').textContent = vehicle === null ? 'New vehicle' : `Vehicle #${vehicle.id}`;
    byId('editor-created').textContent = vehicle === null ? '' : `Created ${formatDate(vehicle.creationDate)}`;
    byId('save-vehicle').textContent = vehicle === null ? 'Create (POST)' : 'Save (PUT)';
    input('name').value = vehicle?.name ?? '';
    input('x').value = asText(vehicle?.coordinates.x);
    input('y').value = asText(vehicle?.coordinates.y);
    input('enginePower').value = asText(vehicle?.enginePower);
    input('numberOfWheels').value = asText(vehicle?.numberOfWheels);
    input('mileage').value = asText(vehicle?.mileage);
    input('type').value = asText(vehicle?.type);
    input('fuelType').value = asText(vehicle?.fuelType);
}

async function open(id) {
    if (id === '') {
        // Not a value at all: GET /vehicles/ is another resource, the collection.
        showNotice(messages(), 'Type the ID of the vehicle to open.');
        byId('open-id').focus();
        return;
    }
    try {
        edit(await vehicleService.get(`/vehicles/${encodeURIComponent(id)}`));
        showNotice(messages(), 'Loaded from the vehicle service.');
    } catch (error) {
        showError(messages(), error);
    }
}

async function save() {
    clearFieldErrors();
    const body = requestBody();
    try {
        const saved = editing === null
            ? await vehicleService.post('/vehicles', body)
            : await vehicleService.put(`/vehicles/${editing.id}`, body);
        const done = editing === null ? 'Created' : 'Saved';
        edit(saved);
        showNotice(messages(), `${done}. The form shows the vehicle as the service stored it.`);
        onSaved(saved);
    } catch (error) {
        showError(messages(), error);
        if (error instanceof ServiceError) {
            markFields(error.fieldErrors);
        }
    }
}

/**
 * The request body as JSON text, written by hand rather than with
 * JSON.stringify of parsed values, so that it carries what was typed:
 * "4.0" stays 4.0 (not 4), "1e400" stays 1e400 (not null), "abc" in a
 * numeric field is sent as the string "abc" for the service to reject.
 */
function requestBody() {
    const members = [
        ['name', JSON.stringify(input('name').value)],
        ['coordinates', `{"x":${number(input('x').value)},"y":${number(input('y').value)}}`],
        ['enginePower', number(input('enginePower').value)],
        ['numberOfWheels', number(input('numberOfWheels').value)],
        ['mileage', number(input('mileage').value)],
        ['type', optionalString(input('type').value)],
        ['fuelType', optionalString(input('fuelType').value)],
    ];
    return `{${members.map(([name, value]) => `"${name}":${value}`).join(',')}}`;
}

const JSON_NUMBER = /^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?$/;

/** An empty field is null; a JSON number goes as it is; anything else as a string. */
function number(typed) {
    const value = typed.trim();
    if (value === '') {
        return 'null';
    }
    return JSON_NUMBER.test(value) ? value : JSON.stringify(typed);
}

function optionalString(typed) {
    return typed.trim() === '' ? 'null' : JSON.stringify(typed);
}

function asText(value) {
    return value === null || value === undefined ? '' : String(value);
}

/** Puts every per-field error next to its input; the error box lists them all anyway. */
function markFields(fieldErrors) {
    const fields = [...byId('vehicle-form').querySelectorAll('.field')];
    for (const {field, message} of fieldErrors) {
        const target = fields.find(candidate => candidate.dataset.field === field);
        if (target !== undefined) {
            target.classList.add('invalid');
            const slot = target.querySelector('.field-error');
            slot.textContent = slot.textContent === '' ? message : `${slot.textContent}; ${message}`;
        }
    }
}

function clearFieldErrors() {
    for (const field of byId('vehicle-form').querySelectorAll('.field')) {
        field.classList.remove('invalid');
        field.querySelector('.field-error').textContent = '';
    }
}
