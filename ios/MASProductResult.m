//
//  MASProductResult.m
//  Adjust
//
//  Created by Jovan Stanimirovic on 16/12/2019.
//

#import "MASProductResult.h"

@implementation MASProductResult

- (NSDictionary*)toDictionary
{
	return @{@"productId": self.productId ? self.productId : [NSNull null],
			 @"country": self.country ? self.country : [NSNull null],
			 @"localizedTitle": self.localizedTitle ? self.localizedTitle : [NSNull null],
			 @"localizedDescription": self.localizedDescription ? self.localizedDescription : [NSNull null],
			 @"price": self.price ? self.price : [NSNull null],
             @"currency": self.currency ? self.currency : [NSNull null],
             @"localizedPrice": self.localizedPrice ? self.localizedPrice : [NSNull null],
			 @"priceLocale": self.priceLocale ? self.priceLocale : [NSNull null],
			 @"subscriptionGroupIdentifier": self.subscriptionGroupIdentifier ? self.subscriptionGroupIdentifier : [NSNull null]
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
