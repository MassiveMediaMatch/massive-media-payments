export declare enum PurchaseState {
    PurchasedSuccessfully = "PurchasedSuccessfully",
    Canceled = "Canceled",
    Refunded = "Refunded",
    SubscriptionExpired = "SubscriptionExpired"
}
export interface ITransactionDetails {
    productId: string;
    orderId: string;
    purchaseToken: string;
    purchaseTime: string;
    purchaseState: string;
    receiptSignature: string;
    receiptData: string;
    autoRenewing: boolean;
    developerPayload: string | undefined;
}
export interface IProduct {
    productId: string;
    title: string;
    description: string;
    price: string;
    currency: string;
    localizedPrice: string;
    country?: string;
}
export interface IPendingTransaction {
    productId: string;
    orderId?: string;
    developerPayload?: string;
    receiptData?: string;
    receiptSignature?: string;
    purchaseToken?: string;
}
export declare enum Proration {
    UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY = 0,
    IMMEDIATE_WITH_TIME_PRORATION = 1,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE = 2,
    IMMEDIATE_WITHOUT_PRORATION = 3,
    DEFERRED = 4
}
interface PaymentInterface {
    open(): Promise<boolean>;
    close(): Promise<boolean>;
    getPendingTransactions(): Promise<IPendingTransaction[]>;
    purchase(productId: string, accoundId?: string | undefined): Promise<ITransactionDetails>;
    purchaseSubscription(productId: string, accoundId?: string | undefined): Promise<ITransactionDetails>;
    purchaseProration(productId: string, originalProductId: string, originalPurchaseToken: string, prorationMode: Proration, accountId?: string): Promise<ITransactionDetails>;
    finishTransaction(productIdOrTransactionId: string): Promise<boolean>;
    getProducts(productIds: string[]): Promise<IProduct[]>;
    getSubscriptions(subIds: string[]): Promise<IProduct[]>;
    isPurchased(productId: string): Promise<boolean>;
}
declare const _default: PaymentInterface;
export default _default;
