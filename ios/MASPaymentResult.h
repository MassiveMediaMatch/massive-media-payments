//
//  MASPaymentResult.h
//  massive-media-payments
//
//  Created by Jovan Stanimirovic on 16/12/2019.
//

#import <Foundation/Foundation.h>

@interface MASPaymentResult : NSObject
@property (nonatomic, strong) NSString *productId;
@property (nonatomic, strong) NSString *orderId;
@property (nonatomic, strong) NSString *purchaseTime;
@property (nonatomic, strong) NSString *purchaseState;
@property (nonatomic, strong) NSString *receiptData;
@property (nonatomic, strong) NSString *developerPayload;

- (NSDictionary*)toDictionary;
- (NSString*)toJson;
@end
