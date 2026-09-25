import {ServiceError, UnreachableError} from './api.js';
import {el} from './dom.js';

/** Shows what went wrong, as the service reported it. */
export function showError(area, error) {
    area.replaceChildren(errorBox(error));
}

export function showNotice(area, text) {
    area.replaceChildren(el('div', {class: 'notice', role: 'status'}, text));
}

export function clearMessages(area) {
    area.replaceChildren();
}

function errorBox(error) {
    if (error instanceof ServiceError) {
        const {problem} = error;
        return el('div', {class: 'problem', role: 'alert'},
            el('strong', {}, `${error.service.name}: ${error.status} ${problem.title ?? ''}`),
            problem.detail ? el('p', {}, problem.detail) : null,
            error.fieldErrors.length === 0 ? null : el('ul', {},
                error.fieldErrors.map(({field, message}) =>
                    el('li', {}, el('code', {}, field === '' ? '(request body)' : field), ` ${message}`))));
    }
    if (error instanceof UnreachableError) {
        const url = error.service.probeUrl;
        return el('div', {class: 'problem', role: 'alert'},
            el('strong', {}, `${error.service.name} did not answer.`),
            el('p', {},
                'Either it is not running, or the browser does not trust its self-signed certificate yet. Open ',
                el('a', {href: url, target: '_blank', rel: 'noopener'}, url),
                ' once, accept the certificate and try again.'));
    }
    console.error(error);
    return el('div', {class: 'problem', role: 'alert'},
        el('strong', {}, 'Unexpected error in the client: '), String(error?.message ?? error));
}

/** "1 vehicle", "3 vehicles". */
export function count(n, noun) {
    return `${n} ${noun}${n === 1 ? '' : 's'}`;
}
