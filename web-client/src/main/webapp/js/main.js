import {SHOP_SERVICE_URL, SWAGGER_URL, VEHICLE_SERVICE_URL} from './config.js';
import {initCollection, reloadCollection} from './collection.js';
import {byId} from './dom.js';
import {edit, initEditor} from './editor.js';
import {initExchangeLog} from './exchange-log.js';
import {initShop} from './shop.js';
import {initStatistics, refreshStatistics} from './statistics.js';

byId('vehicle-service-url').textContent = VEHICLE_SERVICE_URL;
byId('shop-service-url').textContent = SHOP_SERVICE_URL;
byId('swagger-link').href = SWAGGER_URL;

/** The collection changed: everything that shows it is loaded again. */
function collectionChanged() {
    reloadCollection();
    refreshStatistics();
}

initExchangeLog();
initCollection({
    onChanged: refreshStatistics,
    onEdit: vehicle => {
        edit(vehicle);
        byId('editor').scrollIntoView({behavior: 'smooth', block: 'start'});
    },
});
initEditor({onSaved: collectionChanged});
initShop({onChanged: collectionChanged});
initStatistics();
collectionChanged();
