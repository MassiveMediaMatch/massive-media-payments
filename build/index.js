import { NativeModules } from 'react-native';
var MassiveMediaPayments = NativeModules.MassiveMediaPayments;
export var PurchaseState;
(function (PurchaseState) {
    PurchaseState["PurchasedSuccessfully"] = "PurchasedSuccessfully";
    PurchaseState["Canceled"] = "Canceled";
    PurchaseState["Refunded"] = "Refunded";
    PurchaseState["SubscriptionExpired"] = "SubscriptionExpired";
})(PurchaseState || (PurchaseState = {}));
export default MassiveMediaPayments;
//# sourceMappingURL=index.js.map