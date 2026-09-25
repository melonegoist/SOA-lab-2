/**
 * Creates an element. Children given as strings become text nodes, never
 * HTML: vehicle names are user input and must not be able to inject markup.
 */
export function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
        if (value === undefined || value === null || value === false) {
            continue;
        }
        if (key.startsWith('on')) {
            node.addEventListener(key.slice(2), value);
        } else if (key === 'class') {
            node.className = value;
        } else if (key === 'dataset') {
            Object.assign(node.dataset, value);
        } else if (key in node) {
            node[key] = value;
        } else {
            node.setAttribute(key, value);
        }
    }
    node.append(...children.flat().filter(child => child !== undefined && child !== null && child !== false));
    return node;
}

export function byId(id) {
    return document.getElementById(id);
}
