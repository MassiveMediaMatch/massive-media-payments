import { NativeModules } from 'react-native'

const { MassiveMediaPayments } = NativeModules

export enum PurchaseState {
    PurchasedSuccessfully = "PurchasedSuccessfully",
    Canceled = "Canceled",
    Refunded = "Refunded",
    SubscriptionExpired = "SubscriptionExpired"
}

export interface ITransactionDetails {
    productId: string
    orderId: string
    purchaseToken: string
    purchaseTime: string
    purchaseState: string
    receiptSignature: string
    receiptData: string
    autoRenewing: boolean
    developerPayload: string | undefined
}

export interface IProduct {
    productId: string
    title: string
    description: string
    price: number
    currency: string
    localizedPrice: string
    country?: string
    introductoryPrice?: number
    introductoryLocalizedPrice?: string
}

export interface IPendingTransaction {
    productId: string,
    orderId?: string,
    developerPayload?: string,
    receiptData?: string,
    receiptSignature?: string
    purchaseToken?: string
}

export interface IPurchaseConfig {
    ios?: IPurchaseConfigIos
}

export interface IPurchaseConfigIos {
    promotion?: IPurchaseConfigIosPromotion
}

export interface IPurchaseConfigIosPromotion {
    signature: string,
    nonce: string,
    timestamp: number,
    keyIdentifier: string
    identifier: string
}

export enum Proration {
    UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY = 0,
    IMMEDIATE_WITH_TIME_PRORATION = 1,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE = 2,
    IMMEDIATE_WITHOUT_PRORATION = 3,
    DEFERRED = 4,
    IMMEDIATE_AND_CHARGE_FULL_PRICE = 5,
}

interface PaymentInterface {
    open (): Promise<boolean>;
    close (): Promise<boolean>;
    getPendingTransactions (): Promise<IPendingTransaction[]>;
    purchase (productId: string, accoundId?: string | undefined): Promise<ITransactionDetails>;
    purchaseSubscription (productId: string, accoundId?: string | undefined, config?: IPurchaseConfig): Promise<ITransactionDetails>;
    purchaseProration (productId: string, originalProductId: string, originalPurchaseToken: string, prorationMode: Proration, accountId?: string): Promise<ITransactionDetails | null>;
    finishTransaction (productIdOrTransactionId: string): Promise<boolean>;
    getProducts (productIds: string[]): Promise<IProduct[]>;
    getSubscriptions (subIds: string[]): Promise<IProduct[]>;
    isPurchased (productId: string): Promise<boolean>;
}

export default MassiveMediaPayments as PaymentInterface

