/**
 * API endpoint constants for the Energo-Pro energy management system.
 * These are the relative paths used with the base URL for all API requests.
 */
export const Endpoints = {
  // Customer endpoints
  customer: 'customer',
  unwantedCustomer: 'unwanted-customer',
  groupsOfConnectedCustomers: 'connected-group',

  // POD and Meters
  pod: 'pod',
  meters: 'meters',

  // Communication
  sms: 'sms-communication',
  email: 'email-communication',

  // Contracts and Orders
  expressContract: 'express-contract',
  productContract: 'product-contract',
  serviceContract: 'service-contract',
  action: 'actions',
  serviceOrder: 'service-order',
  goodsOrder: 'goods-order',

  // Products, Services, and Goods
  product: 'products',
  service: 'services',
  goods: 'goods',
  priceParameters: 'prices',
  priceComponent: 'price-components',
  groupOfPriceComponents: 'price-component-groups',
  terms: 'terms',
  termsGroup: 'terms-group',
  interim: 'iap',
  groupOfInterims: 'advanced-payment-group',
  termination: 'terminations',
  groupOfTerminations: 'termination-groups',
  penalty: 'penalties',
  groupOfPenalties: 'penalty-groups',

  // Billing and Profiles
  profilesByDay: 'billing-by-profile',
  profilesByHour: 'billing-by-profile',
  profilesBy15Minutes: 'billing-by-profile',
  profilesByMonth: 'billing-by-profile',
  dataByScales: 'billing-by-scales',
  discount: 'discounts',
  compensation: 'government-compensations',

  // Billing Operations
  billingRun: 'billing-run',
  periodicBilling: 'periodic-billing',
  invoice: 'invoice',
  invoiceCancellation: 'invoice-cancellation',

  // Receivables Management
  customerLiability: 'customer-liability',
  customerReceivable: 'customer-receivable',
  deposit: 'deposit',
  manualLiabilityOffsetting: 'manual-liability-offsetting',
  payment: 'payment',
  paymentPackage: 'payment-package',
  collectionChannel: 'collection-channel',
  reminder: 'reminder',
  reminderForDisconnection: 'power-supply-disconnection-reminder',
  requestForDisconnection: 'disconnection-of-power-supply-requests',
  cancellationOfRequestOfDisconnection: 'cancellation-of-disconnection-of-the-power-supply',
  disconnectionOfPowerSupply: 'disconnection-of-power-supply',
  reconnectionOfPowerSupply: 'reconnection-of-the-power-supply',
  interestRate: 'interest-rate',
  latePaymentFine: 'latePaymentFine',
  defaultInterest: 'default-interest',
  massOperationForBlocking: 'mass-operation-for-blocking',
  customerAssessment: 'customer-assessment',
  rescheduling: 'rescheduling',
  calculateRescheduling: 'rescheduling/calculate-rescheduling',

  // Operations Management
  task: 'task',
  activity: 'activities',
  process: 'process',
  processPeriodicity: 'process-periodicity',
  template: 'template',
  billingGroup: 'billing-group',
  cbg: 'balancingGroupCoordinatorObjection',
  systemConfiguration: 'system-configurations',

  // Sales Portal
  salesPortalEndpoints: {
      podCustomerListByCoordinates: 'pod/customer-list-by-coordinates',
      getPodByIdentifier: 'pod/by-identifier',
      getProductListByPod: 'product/list-with-pod-and-customer',
      getProductList: 'product/list'
  }
  
} as const;

export type EndpointsType = typeof Endpoints;
