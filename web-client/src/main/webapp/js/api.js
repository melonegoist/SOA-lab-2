import {SHOP_SERVICE_URL, VEHICLE_SERVICE_URL} from './config.js';

/** A service answered with an error status; {@code problem} is its RFC 9457 body. */
export class ServiceError extends Error {
    constructor(service, status, problem) {
        super(`${service.name} answered ${status}`);
        this.service = service;
        this.status = status;
        this.problem = problem;
    }

    /** Per-field details: [{field, message}], field being a JSON Pointer without the leading slash. */
    get fieldErrors() {
        return Array.isArray(this.problem.errors) ? this.problem.errors : [];
    }
}

/**
 * No answer reached the page: the service is down, or the browser refused
 * the connection, typically because it does not trust the self-signed
 * certificate yet. fetch() gives no details in either case.
 */
export class UnreachableError extends Error {
    constructor(service) {
        super(`${service.name} is unreachable`);
        this.service = service;
    }
}

const exchangeListeners = [];

/** Calls {@code listener({service, method, path, body, status})} after every request. */
export function onExchange(listener) {
    exchangeListeners.push(listener);
}

class Service {
    /**
     * @param probePath a harmless GET; opening it in the browser shows whether
     *                  the service is up and lets the user accept its certificate
     */
    constructor(name, baseUrl, probePath) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.probeUrl = baseUrl + probePath;
    }

    get(path) {
        return this.#send('GET', path);
    }

    /** @param json the request body as JSON text, sent exactly as given */
    post(path, json) {
        return this.#send('POST', path, json);
    }

    put(path, json) {
        return this.#send('PUT', path, json);
    }

    delete(path) {
        return this.#send('DELETE', path);
    }

    /** Resolves to the parsed JSON body (null when there is none); rejects with ServiceError or UnreachableError. */
    async #send(method, path, json) {
        const init = {method};
        if (json !== undefined) {
            init.headers = {'Content-Type': 'application/json'};
            init.body = json;
        }
        let response;
        try {
            response = await fetch(this.baseUrl + path, init);
        } catch {
            this.#report(method, path, json, 'no answer');
            throw new UnreachableError(this);
        }
        this.#report(method, path, json, response.status);
        const text = await response.text();
        const body = parseJson(text);
        if (response.ok) {
            return body;
        }
        const problem = body !== null && typeof body === 'object' && 'status' in body
            ? body
            : {status: response.status, title: response.statusText, detail: text.slice(0, 300) || undefined};
        throw new ServiceError(this, response.status, problem);
    }

    #report(method, path, body, status) {
        exchangeListeners.forEach(listener => listener({service: this, method, path, body, status}));
    }
}

function parseJson(text) {
    if (text === '') {
        return null;
    }
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

export const vehicleService = new Service('Vehicle service', VEHICLE_SERVICE_URL, '/vehicles?size=1');
export const shopService = new Service('Shop service', SHOP_SERVICE_URL, '/shop/search/by-type/CHOPPER');
