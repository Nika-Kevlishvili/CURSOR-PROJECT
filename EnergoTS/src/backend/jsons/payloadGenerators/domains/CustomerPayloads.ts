import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { customerLegal } from '../../payloads/customer/customerLegal';
import { customerPrivate } from '../../payloads/customer/customerPrivate';
import { customerPrivateBusiness } from '../../payloads/customer/customerPrivateBusiness';
import { unwanted_customer } from '../../payloads/customer/unwantedCustomers';
import { groups_of_connected_customers } from "../../payloads/customer/groupsOfConnectedCustomers";

export class CustomerPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    /**
    * Generates a valid Bulgarian EGN (Unified Civil Number) using the official algorithm
    * EGN structure: YYMMDDRRRSС
    * - YY: Year (last 2 digits)
    * - MM: Month (add 40 for years after 2000, add 20 for years 1800-1899)
    * - DD: Day
    * - RRR: Region code (first 1-2 digits) + sequence number + gender (odd=male, even=female)
    * - C: Checksum digit
    */
    private generatePrivateCustomerUIC(): string {
    // Generate random birth date between 1950-2000
    const year = Math.floor(Math.random() * 51) + 1950; // 1950-2000
    const month = Math.floor(Math.random() * 12) + 1; // 1-12
    const day = Math.floor(Math.random() * 28) + 1; // 1-28 (safe for all months)
    
    // Format date parts
    const yy = year % 100;
    let mm = month;
    
    // Adjust month for century (add 40 for 2000+, add 20 for 1800-1899)
    if (year >= 2000) {
        mm += 40;
    } else if (year < 1900) {
        mm += 20;
    }
    
    const dd = day;
    
    // Generate region code and sequence number (digits 7-9)
    // Region codes: 0-43=Sofia, 44-93=Plovdiv, etc.
    // Sequence number determines birth order in that region/day
    // Last digit: odd=male, even=female
    const gender = Math.random() > 0.5 ? 1 : 0; // 1=male, 0=female
    const regionAndSequence = Math.floor(Math.random() * 499) * 2 + gender; // 0-998, ensures correct gender parity
    
    // Build first 9 digits
    const yyStr = yy.toString().padStart(2, '0');
    const mmStr = mm.toString().padStart(2, '0');
    const ddStr = dd.toString().padStart(2, '0');
    const rrrStr = regionAndSequence.toString().padStart(3, '0');
    
    const first9 = yyStr + mmStr + ddStr + rrrStr;
    
    // Calculate checksum using official algorithm
    // Weights: 2,4,8,5,10,9,7,3,6
    const weights = [2, 4, 8, 5, 10, 9, 7, 3, 6];
    let sum = 0;
    
    for (let i = 0; i < 9; i++) {
        sum += parseInt(first9[i]) * weights[i];
    }
    
    let checksum = sum % 11;
    if (checksum === 10) {
        checksum = 0;
    }
    
    return first9 + checksum.toString();
    }

    public customer_legal() {
        let payload = customerLegal();
        return payload;
    }

    public customer_private() {
        let payload = customerPrivate();
        payload.customerIdentifier = this.generatePrivateCustomerUIC();
        return payload;
    }

    public customer_private_business() {
        let payload = customerPrivateBusiness();
        payload.customerIdentifier = this.generatePrivateCustomerUIC();
        return payload;
    }

    public unwanted_customer() {
        let payload = unwanted_customer();
        payload.identificationNumber = this.responses.customer[0].identifier;
        
        return payload;
    }

    public groupsOfConnectedCustomers() {
        const customerIds: number[] = [];
        const payload = groups_of_connected_customers();

        for (const i of this.responses.customer) {
            customerIds.push(i.id);
        }
        payload.customerIds = customerIds;
        return payload;
    }
}