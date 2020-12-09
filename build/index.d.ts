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
interface PaymentInterface {
    open(): Promise<boolean>;
    close(): Promise<boolean>;
    getPendingTransactions(): Promise<IPendingTransaction[]>;
    purchase(productId: string, developerPayload?: string | undefined): Promise<ITransactionDetails>;
    finishTransaction(productIdOrTransactionId: string): Promise<boolean>;
    getProducts(productIds: string[]): Promise<IProduct[]>;
    isPurchased(productId: string): Promise<boolean>;
}
declare const _default: PaymentInterface;
export default _default;
