// Opened from the shop domain itself (through the tunnel or on helios), the
// services are on the same host; opened from se.ifmo.ru, they are on helios.
const servicesHost = location.port === '28681' ? location.hostname : 'helios.cs.ifmo.ru';

export const SHOP_SERVICE_URL = `https://${servicesHost}:28681/shop-service`;
export const VEHICLE_SERVICE_URL = `https://${servicesHost}:28581/vehicle-service`;
export const SWAGGER_URL = `https://${servicesHost}:28681/swagger/`;
