//
//  MassiveMediaPayments.m
//  Twoo
//
//  Created by Jovan Stanimirovic on 28/08/19.
//  Copyright © 2019 Massive Media. All rights reserved.
//

#import "MassiveMediaPayments.h"
#import <StoreKit/StoreKit.h>
#import "MASProductResult.h"
#import "MASPaymentResult.h"


@interface MassiveMediaPayments ()    <SKPaymentTransactionObserver, SKProductsRequestDelegate>
@property (nonatomic, assign) BOOL isOpen;
@property (nonatomic, assign) BOOL hasReceivedPendingTransactions;
@property (nonatomic, assign) NSInteger hasReceivedPendingTransactionsRetries;
@property (nonatomic, strong) NSMutableDictionary *promises;
@property (nonatomic, strong) NSMutableArray *transactions;
@end


@implementation MassiveMediaPayments


RCT_EXPORT_MODULE();

- (id)init
{
    self = [super init];
    if(self != nil)
    {
        self.isOpen = NO;
        self.hasReceivedPendingTransactions = NO;
        self.hasReceivedPendingTransactionsRetries = 0;
        self.promises = [NSMutableDictionary new];
        self.transactions = [NSMutableArray new];
    }
    return self;
}

- (void)dealloc
{
    [[SKPaymentQueue defaultQueue] removeTransactionObserver:self];
}

- (NSDictionary *)constantsToExport
{
    return @{};
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[];
}


#pragma mark - open

RCT_EXPORT_METHOD(open:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (!self.isOpen) {
        /**
         * the observer needs to be added before we get any updates from apple about pending/new transactions
         * This also means if you have unfinishedTransactions that you first need to set the observer before receiving
         * any updates about these unfinished transactions. e.g. `getPendingTransactions` method
         * We chose for the flexability of setting the observer via this method and not put it in `AppDelegate` so that the user
         * is not prompted with password dialogs on application boot.
         * However following issue may occur when using Firebase or any other IAP framework gets priority as an observer.
         * Make sure this observer calls `addTransactionObserver` first before any other framework to receive it's updates here.
         * https://www.greensopinion.com/2017/03/22/This-In-App-Purchase-Has-Already-Been-Bought.html
         */
        [[SKPaymentQueue defaultQueue] addTransactionObserver:self];
        NSLog(@"%@", [SKPaymentQueue defaultQueue].transactions);
        self.isOpen = YES;
    }
    
    if (resolve) {
        resolve(@(YES));
    }
}

#pragma mark - close

RCT_EXPORT_METHOD(close:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    [[SKPaymentQueue defaultQueue] removeTransactionObserver:self];
    self.isOpen = NO;
    
    if (resolve) {
        resolve(@(YES));
    }
}


#pragma mark - purchase

RCT_EXPORT_METHOD(purchase:(NSString*)sku acountId:(NSString*)acountId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (!self.isOpen) {
        if (reject) {
            reject(0, @"Transaction observer is not set. Use `open` method first.", nil);
        }
    }
    
    // only resolve/reject when the payment process is completed/failed
    [self addPromiseForKey:sku resolve:resolve reject:reject];
    
    SKMutablePayment* payment = [[SKMutablePayment alloc] init];
    payment.productIdentifier = sku;
    payment.applicationUsername = acountId;
    payment.quantity = 1;
    [[SKPaymentQueue defaultQueue] addPayment:payment];
}

#pragma mark - purchaseSubscription

RCT_EXPORT_METHOD(purchaseSubscription:(NSString*)sku acountId:(NSString*)acountId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (!self.isOpen) {
        if (reject) {
            reject(0, @"Transaction observer is not set. Use `open` method first.", nil);
        }
    }
    
    // only resolve/reject when the payment process is completed/failed
    [self addPromiseForKey:sku resolve:resolve reject:reject];
    
    SKMutablePayment* payment = [[SKMutablePayment alloc] init];
    payment.productIdentifier = sku;
    payment.applicationUsername = acountId;
    payment.quantity = 1;
    [[SKPaymentQueue defaultQueue] addPayment:payment];
}

#pragma mark - finishTransaction

RCT_EXPORT_METHOD(finishTransaction:(NSString *)transactionId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (!self.isOpen) {
        if (reject) {
            reject(0, @"Transaction observer is not set. Use `open` method first.", nil);
        }
    }
    
    SKPaymentTransaction *transaction = [self transactionWithTransactionId:transactionId];
    // calling finishTransaction on transactions with state 'purchasing' results in a exception 'Cannot finish a purchasing transaction'
    if (transaction) {
        if (transaction.transactionState > SKPaymentTransactionStatePurchasing)
        {
            [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
            [self.transactions removeObject:transaction];
            if(resolve) {
                resolve(@(YES));
            }
        } else {
            if (reject) {
                reject(0, [NSString stringWithFormat:@"Cannot finish a purchasing transaction: %@", transactionId], nil);
            }
        }
    } else {
        if (reject) {
            reject(0, [NSString stringWithFormat:@"No transaction found for transactionId: %@", transactionId], nil);
        }
    }
}

#pragma mark - getPendingTransactions

RCT_EXPORT_METHOD(getPendingTransactions:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (!self.isOpen) {
        if (reject) {
            reject(0, @"Transaction observer is not set. Use `open` method first.", nil);
        }
    }
    
//    if (!self.hasReceivedPendingTransactions) {
//        if (self.hasReceivedPendingTransactionsRetries < 3) {
//            self.hasReceivedPendingTransactionsRetries++;
//            // precautionary wait if this method is called right after `open`. The transactions need to be returned first.
//            dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, 1 * NSEC_PER_SEC);
//            dispatch_after(delay, dispatch_get_main_queue(), ^(void){
//                [self getPendingTransactions:resolve reject:reject];
//            });
//            return;
//        } if (reject) {
//            self.hasReceivedPendingTransactionsRetries = 0;
//            reject(0, @"`getPendingTransactions` aborted after 3 retries", nil);
//            return;
//        }
//    }
    
    NSMutableArray *pendingTransactions = [NSMutableArray new];
    for (SKPaymentTransaction *transaction in [SKPaymentQueue defaultQueue].transactions) {
        if (![self hasPromiseForKey:transaction.payment.applicationUsername]) {
            MASPaymentResult *result = [self paymentResultWithTransaction:transaction];
            [pendingTransactions addObject:[result toDictionary]];
        }
    }
    
    if(resolve) {
        resolve(pendingTransactions);
    }
}

#pragma mark - productsRequest

RCT_EXPORT_METHOD(getProducts:(NSArray*)productIdentifiers resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    SKProductsRequest *productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers:[NSSet setWithArray:productIdentifiers]];
    productsRequest.delegate = self;
    
    NSString *key = RCTKeyForInstance(productsRequest);
    [self addPromiseForKey:key resolve:resolve reject:reject];
    
    [productsRequest start];
}

#pragma mark - subscriptionRequest

RCT_EXPORT_METHOD(getSubscriptions:(NSArray*)subscriptionIdentifiers resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if(resolve) {
        resolve(@(YES));
    }
}

#pragma mark - helpers

- (void)addPromiseForKey:(NSString*)key resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
    NSMutableArray* promises = [self.promises valueForKey:key];
    
    if (promises == nil) {
        promises = [NSMutableArray array];
    } else {
        [promises removeAllObjects];
    }
    if (resolve && reject) {
        [promises addObject:@[resolve, reject]];
    }
    
    [self.promises setValue:promises forKey:key];
}

- (BOOL)hasPromiseForKey:(NSString*)key
{
    NSMutableArray* promises = [self.promises valueForKey:key];
    return promises != nil;
}

- (BOOL)resolvePromiseForKey:(NSString*)key value:(id)value
{
    NSMutableArray* promises = [self.promises valueForKey:key];
    
    if (promises != nil) {
        for (NSMutableArray *tuple in promises) {
            RCTPromiseResolveBlock resolveBlck = tuple[0];
            resolveBlck(value);
        }
        [self.promises removeObjectForKey:key];
        return YES;
    } else {
        NSLog(@"No promise found to resolve!");
        return NO;
    }
}

- (BOOL)rejectPromiseForKey:(NSString*)key code:(NSString*)code message:(NSString*)message error:(NSError*) error
{
    NSMutableArray* promises = [self.promises valueForKey:key];
    
    if (promises != nil) {
        for (NSMutableArray *tuple in promises) {
            RCTPromiseRejectBlock reject = tuple[1];
            reject(code, message, error);
        }
        [self.promises removeObjectForKey:key];
        return YES;
    } else {
        NSLog(@"No promise found to reject!");
        return NO;
    }
}

- (SKPaymentTransaction*)transactionWithTransactionId:(NSString*)transactionId
{
    NSArray *transactions = [self.transactions arrayByAddingObjectsFromArray:[SKPaymentQueue defaultQueue].transactions];
    for (SKPaymentTransaction *transaction in transactions) {
        if ([transaction.transactionIdentifier isEqualToString:transactionId]) {
            return transaction;
        }
    }
    
    return nil;
}

- (MASPaymentResult*)paymentResultWithTransaction:(SKPaymentTransaction*)transaction
{
    // resolve with payment data
    MASPaymentResult *result = [MASPaymentResult new];
    result.productId = transaction.payment.productIdentifier;
    result.orderId = transaction.transactionIdentifier;
    result.purchaseTime = [NSString stringWithFormat:@"%f", transaction.transactionDate.timeIntervalSince1970];
    result.developerPayload = transaction.payment.applicationUsername;
    
    // receipt
    //    NSString *receiptBase64 = [transaction.transactionReceipt base64EncodedStringWithOptions:0];
    //    result.receiptData = receiptBase64;
    NSData *receiptData = [NSData dataWithContentsOfURL:[[NSBundle mainBundle] appStoreReceiptURL]];
    result.receiptData = [receiptData base64EncodedStringWithOptions:0];
    
    // state
    switch (transaction.transactionState) {
        case SKPaymentTransactionStatePurchased:
        {
            result.purchaseState = @"PurchasedSuccessfully";
            break;
        }
        case SKPaymentTransactionStateFailed:
        {
            result.purchaseState = transaction.error.code == SKErrorPaymentCancelled ? @"Canceled" : @"Error";
            break;
        }
        default:
        {
            break;
        }
    }
    
    return result;
}

#pragma mark - <SKPaymentTransactionObserver>

- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray*)transactions
{
    NSLog(@"Updated transactions: %@", transactions);
    for (SKPaymentTransaction* transaction in transactions)
    {
        NSLog(@"transaction state: %ld", (long)transaction.transactionState);
        NSString *developerPayload = transaction.payment.productIdentifier;
        switch (transaction.transactionState)
        {
            case SKPaymentTransactionStatePurchasing: // purchase started
            case SKPaymentTransactionStateDeferred: // Deferred (awaiting approval via parental controls, etc.
            {
                break;
            }
            case SKPaymentTransactionStatePurchased:
            {
                [self.transactions addObject:transaction];
                MASPaymentResult *result = [self paymentResultWithTransaction:transaction];
                [self resolvePromiseForKey:developerPayload value:[result toDictionary]];
                break;
            }
            case SKPaymentTransactionStateFailed:
            {
                [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
                switch (transaction.error.code) {
                    case SKErrorPaymentCancelled:
                    {
                        MASPaymentResult *result = [self paymentResultWithTransaction:transaction];
                        [self resolvePromiseForKey:developerPayload value:[result toDictionary]];
                        break;
                    }
                    default:
                    {
                        [self rejectPromiseForKey:developerPayload code:[NSString stringWithFormat:@"%ld", transaction.error.code] message:transaction.error.localizedDescription error:transaction.error];
                        break;
                    }
                }
                break;
            }
            case SKPaymentTransactionStateRestored:
            {
                NSLog(@"SKPaymentTransactionStateRestored");
                // [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
                break;
            }
        }
    }
    self.hasReceivedPendingTransactions = YES;
}


#pragma mark - <SKProductsRequestDelegate>

- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response
{
    if(response.products.count > 0)
    {
        NSMutableArray *products = [NSMutableArray new];
        for (SKProduct *product in response.products) {
            NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
            formatter.numberStyle = NSNumberFormatterCurrencyStyle;
            formatter.locale = product.priceLocale;
            NSString* localizedPrice = [formatter stringFromNumber:product.price];
            
            NSString* currencyCode = @"";
            if (@available(iOS 10.0, *)) {
                currencyCode = product.priceLocale.currencyCode;
            }
            
            MASProductResult *result = [MASProductResult new];
            result.productId = product.productIdentifier;
            result.localizedTitle = product.localizedTitle;
            result.localizedDescription = product.localizedDescription;
            result.country = (NSString*)CFLocaleGetValue((CFLocaleRef)product.priceLocale, kCFLocaleCountryCode);
            result.price = [product.price stringValue];
            result.localizedPrice = localizedPrice;
            result.currency = currencyCode;
            if (@available(iOS 10.0, *)) {
                result.priceLocale = product.priceLocale.languageCode;
            } else {
                result.priceLocale = result.country;
            }
            if (@available(iOS 12.0, *)) {
                result.subscriptionGroupIdentifier = product.subscriptionGroupIdentifier;
            }
            [products addObject:[result toDictionary]];
        }
        
        NSString *key = RCTKeyForInstance(request);
        [self resolvePromiseForKey:key value:products];
    }
    else if (response.invalidProductIdentifiers.count > 0) {
        NSString *key = RCTKeyForInstance(request);
        [self rejectPromiseForKey:key code:0 message:@"Invalid productIdentifier specified. No products were returned by Apple." error:nil];
    }
    else {
        NSString *key = RCTKeyForInstance(request);
        [self rejectPromiseForKey:key code:0 message:@"Something went wrong. No valid or invalid products were returned by Apple." error:nil];
    }
}

static NSString *RCTKeyForInstance(id instance)
{
    return [NSString stringWithFormat:@"%p", instance];
}

@end

