import {onExchange} from './api.js';
import {byId, el} from './dom.js';

// Every request of the page and the status it got: shows which service does
// what, and that the page only calls the documented API.

const LIMIT = 20;

export function initExchangeLog() {
    const log = byId('exchange-log');
    onExchange(({service, method, path, body, status}) => {
        const ok = typeof status === 'number' && status < 400;
        log.prepend(el('li', {class: ok ? 'ok' : 'failed'},
            el('time', {}, new Date().toLocaleTimeString('en-GB')),
            el('span', {class: `service ${service.name.split(' ')[0].toLowerCase()}`}, service.name),
            el('code', {}, `${method} ${readable(path)}`),
            el('strong', {}, String(status)),
            body === undefined ? null : el('code', {class: 'body'}, body)));
        while (log.children.length > LIMIT) {
            log.lastElementChild.remove();
        }
    });
}

/** The path as a person reads it: the query decoded ("+" is a space there, "%2B" a plus). */
function readable(path) {
    const [route, query] = path.split('?', 2);
    if (query === undefined) {
        return route;
    }
    try {
        return `${route}?${decodeURIComponent(query.replaceAll('+', ' '))}`;
    } catch {
        return path;
    }
}
