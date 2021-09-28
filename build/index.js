import { NativeModules } from 'react-native';
var MassiveMediaPayments = NativeModules.MassiveMediaPayments;
export var PurchaseState;
(function (PurchaseState) {
    PurchaseState["PurchasedSuccessfully"] = "PurchasedSuccessfully";
    PurchaseState["Canceled"] = "Canceled";
    PurchaseState["Refunded"] = "Refunded";
    PurchaseState["SubscriptionExpired"] = "SubscriptionExpired";
})(PurchaseState || (PurchaseState = {}));
export var Proration;
(function (Proration) {
    Proration[Proration["UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY"] = 0] = "UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY";
    Proration[Proration["IMMEDIATE_WITH_TIME_PRORATION"] = 1] = "IMMEDIATE_WITH_TIME_PRORATION";
    Proration[Proration["IMMEDIATE_AND_CHARGE_PRORATED_PRICE"] = 2] = "IMMEDIATE_AND_CHARGE_PRORATED_PRICE";
    Proration[Proration["IMMEDIATE_WITHOUT_PRORATION"] = 3] = "IMMEDIATE_WITHOUT_PRORATION";
    Proration[Proration["DEFERRED"] = 4] = "DEFERRED";
    Proration[Proration["IMMEDIATE_AND_CHARGE_FULL_PRICE"] = 5] = "IMMEDIATE_AND_CHARGE_FULL_PRICE";
})(Proration || (Proration = {}));
export default MassiveMediaPayments;
//# sourceMappingURL=index.js.map