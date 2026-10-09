/**
 * Container for storing created entity responses during test execution.
 * Each domain has its own array to store entities created in test steps.
 * This enables cross-domain dependencies where entities can reference
 * previously created entities from other domains.
 */
export interface ResponsesContainer {
  // Customer entities
  customer: any[];
  unwantedCustomer: any[];
  groupsOfConnectedCustomers: any[];

  // POD and Meters
  pod: any[];
  meters: any[];

  // Communication
  sms: any[];
  email: any[];

  // Contracts and Orders
  expressContract: any[];
  productContract: any[];
  serviceContract: any[];
  action: any[];
  serviceOrder: any[];
  goodsOrder: any[];
  claimedPenalty: any[];

  // Products, Services, and Goods
  product: any[];
  service: any[];
  goods: any[];
  priceParameters: any[];
  priceComponent: any[];
  groupOfPriceComponents: any[];
  terms: any[];
  termsGroup: any[];
  interim: any[];
  groupOfInterims: any[];
  termination: any[];
  groupOfTerminations: any[];
  penalty: any[];
  groupOfPenalties: any[];

  // Billing data
  dataByProfiles: any[];
  dataByScales: any[];
  discount: any[];
  compensation: any[];

  // Billing operations
  billingRun: any[];
  periodicBilling: any[];
  invoice: any[];
  invoiceCancellation: any[];

  // Receivables management
  customerLiability: any[];
  customerReceivable: any[];
  deposit: any[];
  manualLiabilityOffsetting: any[];
  payment: any[];
  paymentPackage: any[];
  collectionChannel: any[];
  reminder: any[];
  reminderForDisconnection: any[];
  requestForDisconnection: any[];
  cancellationOfRequestOfDisconnection: any[];
  disconnectionOfPowerSupply: any[];
  reconnectionOfPowerSupply: any[];
  interestRate: any[];
  latePaymentFine: any[];
  defaultInterest: any[];
  massOperationForBlocking: any[];
  customerAssessment: any[];
  rescheduling: any[];

  // Operations management
  task: any[];
  activity: any[];
  processPeriodicity: any[];
  template: any[];
  cbg: any[];
}

/**
 * Factory function to create a new ResponsesContainer with all arrays initialized.
 * Used by the Responses fixture to provide a fresh container for each test.
 */
export function createResponsesContainer(): ResponsesContainer {
  return {
    customer: [],
    unwantedCustomer: [],
    groupsOfConnectedCustomers: [],
    pod: [],
    meters: [],
    sms: [],
    email: [],
    expressContract: [],
    productContract: [],
    serviceContract: [],
    action: [],
    serviceOrder: [],
    goodsOrder: [],
    claimedPenalty: [],
    product: [],
    service: [],
    goods: [],
    priceParameters: [],
    priceComponent: [],
    groupOfPriceComponents: [],
    terms: [],
    termsGroup: [],
    interim: [],
    groupOfInterims: [],
    termination: [],
    groupOfTerminations: [],
    penalty: [],
    groupOfPenalties: [],
    dataByProfiles: [],
    dataByScales: [],
    discount: [],
    compensation: [],
    billingRun: [],
    periodicBilling: [],
    invoice: [],
    invoiceCancellation: [],
    customerLiability: [],
    customerReceivable: [],
    deposit: [],
    manualLiabilityOffsetting: [],
    payment: [],
    paymentPackage: [],
    collectionChannel: [],
    reminder: [],
    reminderForDisconnection: [],
    requestForDisconnection: [],
    cancellationOfRequestOfDisconnection: [],
    disconnectionOfPowerSupply: [],
    reconnectionOfPowerSupply: [],
    interestRate: [],
    latePaymentFine: [],
    defaultInterest: [],
    massOperationForBlocking: [],
    customerAssessment: [],
    rescheduling: [],
    task: [],
    activity: [],
    processPeriodicity: [],
    template: [],
    cbg: []
  };
}
