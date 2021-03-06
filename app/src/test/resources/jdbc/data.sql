Insert into WEBS.EP_SERVICE_STATE (SERVICE_STATE_ID,SERVICE_STATE_NAME,SERVICE_STATE_CODE,TRIAL,RENEWAL,OPT_IN,COLLECTION,CREATE_DATE,UPDATE_DATE) values (101,'Trial','TRIAL','Y','N','N','N','2015-04-16',null);
Insert into WEBS.subscription (
    SUBSCRIPTION_ID,
    CUSTOMER_ACCOUNT_ID,
    OFFER_ID,
    CREATE_DATE,
    UPDATE_DATE
) values (
    997121,
    9997121,
    20000009,
    '2015-05-04',
    null
);
Insert into WEBS.ENTITLED_PRODUCT(
    ENTITLED_PRODUCT_ID,
    SUBSCRIPTION_ID,
    PRODUCT_ID,
    ENTITLED_PRODUCT_STATE,
    SERVICE_STATE_ID,
    GRANT_ID,
    CREATE_DATE,
    UPDATE_DATE,
    SUBSCRIPTION_ADDON_ID,
    PRODUCT_CODE
) values (
    100120,
    997121,
    22000129,
    'ACTIVE',
    101,
    4541897816400737165,
    '2015-05-04',
    '2015-06-02',
    null,
    'QBDTCA'
);
Insert into WEBSMIG.CONVERT_ACCOUNT_V2
(
  CONVERSION_ID
, REALM_ID
, SOURCE_SYSTEM
, SOURCE_ACCOUNT_PRODUCT_TYPE
, CONVERT_ACCOUNT_TYPE_ID
, CUSTOMER_SEGMENT
, EXTENDED_CUSTOMER_SEGMENT
, CONVERT_TYPE_ID
, CONVERSION_CONTEXT_ID
, ACCOUNT_CONTEXT
, INTUIT_TID
, OBILL_CONV_TRY_COUNT
, OBILL_CONV_STATUS_ID
, OBILL_CONV_STATUS_DATE
, OBILL_CONV_NTFY_STATUS_ID
, OBILL_BILLSYS_STATUS_ID
, OFFER_MAPPING_RULE_ID
, OBILL_PRICE_AFTER_DISCOUNT
, OBILL_DISCOUNT
, OBILL_TOTAL_PRICE
, PARENT_REALM_ID
, LGCY_BDOM
, LGCY_PRICE_AFTER_DISCOUNT
, LGCY_CANCEL_TRY_COUNT
, LGCY_CANCEL_STATUS_ID
, LGCY_CANCEL_STATUS_DATE
, LGCY_CANCEL_ERROR_MSG
, LGCY_CURRENCY_ISO_CODE
, CONV_COMPANY_ADDRESS_TYPE
, CONV_PAYMENT_INFO_TYPE
, INTUIT_WEBS_TBT
, CLIENTS_COUNT
, WEBS_REQUEST_ID
, WEBS_CORRELATION_ID
, CONV_POST_SUBS_CONTEXT
, COMPANY_NAME
, OBILL_TEMP_GRANT_IDS
, IS_DELETE_OBILL_TEMP_GRANTS
, GRANT_CLEANUP_STATUS_ID
, GRANT_CLEANUP_TRY_COUNT
, GRANT_CLEANUP_STATUS_DATE
, CREATE_DATE
, UPDATE_DATE
, TAX_EXEMPT_NTFY_STATUS_ID
, IS_CLEANUP_OBILL_TEMP_GRANTS
, IS_CLIENT
, CLIENT_CONV_TRIG_TRY_COUNT
, CLIENT_CONV_TRIG_TRY_DATE
, CLIENT_CONV_TRIG_ERROR_MSG
, LGCY_OFFER_ID
, WEBS_TRANSACTION_DATE_TIME
, INTUIT_CHAOS_IDS
, INTUIT_CHAOS_POINTS
, RPS_NTFY_STATUS_ID
, DISCOUNT_END_DATE
, HAS_REMAINING_DISCOUNT
, IS_BUNDLE
, EXTERNAL_OFFER_ID
, IS_PENDING_CLIENT
, PERIOD_END_DATE
, LGCY_DISCOUNT_DURATION
, IS_ANNUAL_OFF_BDOY
, LEGACY_ACCOUNT_ID
, ENTITLEMENT_STATE
, CONVERT_OFFER_TYPE
, LGCY_COUNTRY
) values (
0,
9130352648956296,
'ICIS',
'TEST',
0,
'TEST',
'TEST',
0,
0,
'TEST',
'TEST',
0,
0,
CURRENT_DATE,
null,
0,
0,
null,
null,
null,
null,
null,
null,
null ,
null,
null,
null,
null,
null,
null,
null,
0,
null,
null,
null,
null,
null,
null,
null,
null,
null,
CURRENT_DATE,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
null,
100000,
null,
null,
null
);