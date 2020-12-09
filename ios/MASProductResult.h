//
//  MASProductResult.h
//  Adjust
//
//  Created by Jovan Stanimirovic on 16/12/2019.
//

#import <Foundation/Foundation.h>

@interface MASProductResult : NSObject
@property (nonatomic, strong) NSString *productId;
@property (nonatomic, strong) NSString *country;
@property (nonatomic, strong) NSString *localizedTitle;
@property (nonatomic, strong) NSString *localizedDescription;
@property (nonatomic, strong) NSString *price;
@property (nonatomic, strong) NSString *currency;
@property (nonatomic, strong) NSString *localizedPrice;
@property (nonatomic, strong) NSString *priceLocale;
@property (nonatomic, strong) NSString *subscriptionGroupIdentifier;

- (NSDictionary*)toDictionary;
- (NSString*)toJson;
@end
