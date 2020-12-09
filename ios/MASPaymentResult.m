//
//  MASPaymentResult.m
//  massive-media-payments
//
//  Created by Jovan Stanimirovic on 16/12/2019.
//

#import "MASPaymentResult.h"

@implementation MASPaymentResult

- (NSDictionary*)toDictionary
{
	return @{
			@"productId": self.productId ? self.productId : [NSNull null],
			@"orderId": self.orderId ? self.orderId : [NSNull null],
			@"purchaseTime": self.purchaseTime ? self.purchaseTime : [NSNull null],
			@"purchaseState": self.purchaseState ? self.purchaseState : [NSNull null],
			@"receiptData": self.receiptData ? self.receiptData : [NSNull null],
			@"developerPayload": self.developerPayload ? self.developerPayload : [NSNull null]
			};
}

- (NSString*)toJson
{
	NSError *error;
	NSData *jsonData = [NSJSONSerialization dataWithJSONObject:[self toDictionary] options:0 error:&error];
	NSString *json = jsonData ? [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding] : @"{}";
	return json;
}

@end
