# 🎓 Backend API ტესტების წერის სახელმძღვანელო

## სარჩევი

1. [შესავალი](#შესავალი)
2. [პროექტის არქიტექტურა](#პროექტის-არქიტექტურა)
3. [გარემოს მომზადება](#გარემოს-მომზადება)
4. [პირველი ტესტის დაწერა — ნაბიჯ-ნაბიჯ](#პირველი-ტესტის-დაწერა--ნაბიჯ-ნაბიჯ)
5. [Fixture-ების გაცნობა](#fixture-ების-გაცნობა)
6. [Payload Generator-ების გამოყენება](#payload-generator-ების-გამოყენება)
7. [Responses — მდგომარეობის მართვა ტესტებს შორის](#responses--მდგომარეობის-მართვა-ტესტებს-შორის)
8. [Endpoints — API ბილიკები](#endpoints--api-ბილიკები)
9. [CheckResponse — API პასუხის შემოწმება](#checkresponse--api-პასუხის-შემოწმება)
10. [ტესტის სახელდება და თეგები](#ტესტის-სახელდება-და-თეგები)
11. [test.step() — ნაბიჯები ტესტში](#teststep--ნაბიჯები-ტესტში)
12. [ენთითის დამოკიდებულებები](#ენთითის-დამოკიდებულებები)
13. [ნომენკლატურები](#ნომენკლატურები)
14. [რეალური ტესტის ანალიზი](#რეალური-ტესტის-ანალიზი)
15. [Mass Import ტესტები](#mass-import-ტესტები)
16. [ტესტების გაშვება](#ტესტების-გაშვება)
17. [ხშირი შეცდომები და გადაწყვეტები](#ხშირი-შეცდომები-და-გადაწყვეტები)
18. [საუკეთესო პრაქტიკები](#საუკეთესო-პრაქტიკები)
19. [ფაილური სტრუქტურის მითითება](#ფაილური-სტრუქტურის-მითითება)

---

## შესავალი

გამარჯობა! ეს დოკუმენტაცია შექმნილია იმისთვის, რომ დაგეხმაროს ჩვენს API ტესტირების ფრეიმვორკში ჩართვაში. ჩვენ ვიყენებთ **Playwright**-ის API ტესტირების შესაძლებლობებს ენერგო-პროს ბექენდ სისტემის ტესტირებისთვის.

**რა უნდა იცოდე ტესტების წერის დაწყებამდე:**
- javascript/typescript ის ბაზისური ცოდნა
- REST API-ის კონცეფციები (POST, GET, PATCH, DELETE)
- async/await-ის გაგება
- Playwright-ის ძირითადი ცნებები (არ არის აუცილებელი ღრმა ცოდნა)

**რა არ არის ეს პროექტი:**
- ეს არ არის UI ტესტირების ფრეიმვორკი
- ბრაუზერს არ ვიყენებთ — მხოლოდ HTTP მოთხოვნებს ვაგზავნით API-ზე

---

## პროექტის არქიტექტურა

პროექტი ორგანიზებულია **ბიზნეს დომეინების** მიხედვით. ყველა ტესტი, payload-ი და გენერატორი თავის დომეინშია განთავსებული.

### მთავარი საქაღალდეები:

```
📦 პროექტი
├── fixtures/           ← ფიქსჩერები (ტესტების საბაზისო კონფიგურაცია)
│   ├── baseFixture.ts  ← მთავარი ფიქსჩერი — აქედან იმპორტირდება test და expect
│   ├── constants/      ← ენდპოინტები
│   ├── matchers/       ← CheckResponse მატჩერი
│   └── types/          ← TypeScript ტიპები (ResponsesContainer)
│
├── jsons/
│   ├── payloadGenerators/
│   │   ├── PayloadGenerator.ts    ← მთავარი კლასი — ყველა დომეინს აერთიანებს
│   │   └── domains/               ← დომეინ-სპეციფიური გენერატორები
│   │       ├── CustomerPayloads.ts
│   │       ├── BillingPayloads.ts
│   │       ├── ContractsAndOrdersPayloads.ts
│   │       └── ...
│   └── payloads/
│       └── create/               ← ნედლი payload შაბლონები
│
├── tests/                        ← ტესტ ფაილები
│   ├── billing/
│   ├── customers/
│   ├── contractsAndOrders/
│   ├── receivableManagement/
│   └── setup/                    ← გლობალური სეტაპი
│
├── mass-imports/                  ← Excel-ზე დაფუძნებული მასს იმპორტი
└── utils/                        ← დამხმარე ფუნქციები
```

### სამფაზიანი სასიცოცხლო ციკლი:

1. **Setup პროექტი** (`global-setup.ts`) — ავთენტიფიკაცია, ნომენკლატურების გენერაცია
2. **Main პროექტი** — ტესტების შესრულება (პარალელურად)
3. **Send report** (`global-teardown.ts`) — შედეგების გაგზავნა Slack/Jira-ზე

---

## გარემოს მომზადება

### ბიბლიოთეკების დაყენება

```bash
npm install
```

### სეტაპის გაშვება (პირველად აუცილებლად!)

სეტაპი ქმნის ავთენტიფიკაციის ტოკენს და ნომენკლატურებს:

```bash
npx playwright test --project=setup --workers=1
```

> ⚠️ **მნიშვნელოვანი:** სეტაპი აუცილებლად `--workers=1`-ით უნდა გაეშვას! პარალელური შესრულება race condition-ებს იწვევს ტოკენისა და ნომენკლატურების ფაილებში ჩაწერისას.

ამის შემდეგ გაჩნდება:
- `fixtures/token.json` — JWT ტოკენი
- `fixtures/env_variables.json` — ნომენკლატურები (თუ არსებობს წამოიღებს არსებულს, თუ არადა შექმნის ახლებს.
იმავე ლოგიკა ვრცელდება თემფლეითებზე, თუ არსებობს წამოიღებს არსებულ თემფლეითებს თუ არადა შექმნის ახალს და ჩაწერს ფაილში)

---

## პირველი ტესტის დაწერა — ნაბიჯ-ნაბიჯ

დავწეროთ მარტივი ტესტი რომელიც ქმნის მომხმარებელს (customer). ეს არის ყველაზე მარტივი ტიპის ტესტი.

### ნაბიჯი 1: ფაილის შექმნა

შექმენი ფაილი შესაბამის დომეინის საქაღალდეში, მაგალითად:
`tests/customers/myFirstTest.spec.ts`

### ნაბიჯი 2: იმპორტები

```typescript
import { test, expect } from '../../fixtures/baseFixture';
```

> 🔴 **არასოდეს დააიმპორტო `test` და `expect` `@playwright/test`-იდან!** ყოველთვის `baseFixture`-იდან. ჩვენს `baseFixture`-ს აქვს ჩაშენებული ფიქსჩერები და ქასთომ მატჩერი (`CheckResponse`).

### ნაბიჯი 3: ტესტის სტრუქტურა

```typescript
import { test, expect } from '../../fixtures/baseFixture';

test.describe('[REG-1]: Customer', { tag: '@customer' }, () => {
    test('[REG-XXX]: მომხმარებლის შექმნა | Happy path', async ({ Request, GeneratePayload, Responses, Endpoints }) => {

        await test.step('მომხმარებლის შექმნა', async () => {
            // 1. payload-ის გენერაცია
            const payload = GeneratePayload.customers.customer_legal();

            // 2. API მოთხოვნის გაგზავნა
            const response = await Request.post(Endpoints.customer, { data: payload });

            // 3. პასუხის შემოწმება
            await expect(response).CheckResponse();

            // 4. პასუხის შენახვა Responses-ში
            Responses.customer.push(await response.json());
        });

    });
});
```

**ნაბიჯების განმარტება:**

| # | რას ვაკეთებთ | რატომ |
|---|-------------|-------|
| 1 | `GeneratePayload.customers.customer_legal()` | აგენერირებს ვალიდურ payload-ს რანდომული მონაცემებით |
| 2 | `Request.post(Endpoints.customer, { data: payload })` | აგზავნის POST მოთხოვნას. `Request` უკვე შეიცავს Bearer ტოკენს |
| 3 | `await expect(response).CheckResponse()` | ამოწმებს რომ HTTP სტატუსი 2xx-ია. თუ შეცდომაა — დეტალურ ინფორმაციას აბრუნებს |
| 4 | `Responses.customer.push(await response.json())` | ინახავს ობიექტის შექმნიდან დაბრუნებულ რესფონსს — შეიძლება სხვა ნაბიჯებში დაგვჭირდეს |

---

## Fixture-ების გაცნობა

Fixture-ები არის Playwright-ის მექანიზმი რომელიც ავტომატურად ამზადებს ტესტისთვის საჭირო რესურსებს. ჩვენ გვაქვს შემდეგი ფიქსჩერები:

### `Request`
ავთენტიფიცირებული API კონტექსტი. ავტომატურად ამატებს `Authorization: Bearer <token>` ჰედერს ყველა მოთხოვნას.

```typescript
// GET მოთხოვნა
const response = await Request.get(Endpoints.customer);

// POST მოთხოვნა payload-ით
const response = await Request.post(Endpoints.customer, { data: payload });

// PATCH მოთხოვნა
const response = await Request.patch(`${Endpoints.customer}/${id}`, { data: payload });
```

### `FileUploadRequest`
იგივეა რაც `Request`, მაგრამ **multipart/form-data** ფაილის ატვირთვისთვის. გამოიყენება მასს იმპორტის ტესტებში.

> 🔴 **არასოდეს გამოიყენო ჩვეულებრივი `Request` ფაილის ატვირთვისთვის** — მას არ აქვს multipart content-type.

### `GeneratePayload`
მთავარი payload გენერატორი. აქვს ყველა დომეინის ქვეკლასი:

```typescript
GeneratePayload.customers          // მომხმარებლები
GeneratePayload.billing            // ბილინგი
GeneratePayload.contractsAndOrders // კონტრაქტები და შეკვეთები
GeneratePayload.productAndServices // პროდუქტები და სერვისები
GeneratePayload.pointsOfDelivery   // მომარაგების წერტილები (POD)
GeneratePayload.receivablesManagement // მოთხოვნების მართვა
GeneratePayload.energyData         // ენერგეტიკული მონაცემები
GeneratePayload.operationsManagement  // ოპერაციების მართვა
GeneratePayload.masterData         // მასტერ მონაცემები
GeneratePayload.customerCommunication // მომხმარებლის კომუნიკაცია
```

### `Responses`
ტესტის სტეფებს შორის გაზიარებული სტეიტის კონტეინერი. ყოველი დომეინისთვის ცარიელი მასივია. შექმნილ ენტითებს ვუშვებთ შესაბამის მასივში:

```typescript
Responses.customer          // მომხმარებლის პასუხები
Responses.productContract   // პროდუქტ-კონტრაქტის პასუხები
Responses.billingRun        // ბილინგ რანის პასუხები
Responses.invoice           // ინვოისების პასუხები
// ... და ა.შ.
```

### `Endpoints`
API ენდპოინტების სტრინგ-კონსტანტები:

```typescript
Endpoints.customer            // 'customer'
Endpoints.productContract     // 'product-contract'
Endpoints.billingRun          // 'billing-run'
Endpoints.invoice             // 'invoice'
Endpoints.priceComponent      // 'price-components'
Endpoints.terms               // 'terms'
Endpoints.pod                 // 'pod'
// ... და ა.შ.
```

### `Nomenclatures`
სისტემური საცნობარო მონაცემების (ნონემკლატურები და თემფლეითები) მიღება/შექმნა:

```typescript
const profileId = await Nomenclatures.profiles('playwright');
```

---

## Payload Generator-ების გამოყენება

### როგორ მუშაობს

Payload გენერატორი ორი ფენისგან შედგება:

1. **ნედლი შაბლონი** (`jsons/payloads/create/`) — დაბრუნებულია ფუნქციიდან, შეიცავს ბაზისურ სტრუქტურას
2. **დომეინის გენერატორი** (`jsons/payloadGenerators/domains/`) — იძახებს შაბლონს და ამატებს ობიექტის შექმნისთვის საჭირო წინარე შექმნილ ობიექტებს/ცვლადებს შესაბამის ველებში დინამიურად

### მარტივი მაგალითი — მომხმარებლის payload

```typescript
// CustomerPayloads.ts-ში:
public customer_legal() {
    let payload = customerLegal(); // ნედლი შაბლონი
    return payload;
}
```

ეს მარტივი შემთხვევაა — მომხმარებლის შესაქმნელად სხვა ენტითი არ გვჭირდება.

### რთული მაგალითი — ენტითების დაკავშირება

```typescript
// BillingPayloads.ts-ში:
public async billingRun(level: string, types: string[]) {
    const payload = forVolumes(); // ნედლი შაბლონი

    // ავტომატურად ვეძებთ Responses-ში ადრე შექმნილ ენტითებს:
    payload.basicParameters.listOfCustomersContractsOrPOD = [
        this.responses.productContract[0].id  // ან serviceContract
    ];

    return payload;
}
```

> 💡 **გენერატორმა თავად იცის რა ენტითები სჭირდება `this.responses` მასივიდან.** მაგრამ შენ, როგორც ტესტის ავტორმა, აუცილებლად უნდა შექმნა ეს ენტითები წინა ნაბიჯებში!

---

## Responses — მდგომარეობის მართვა ტესტებს შორის

`Responses` არის ერთ-ერთი ყველაზე მნიშვნელოვანი კონცეფცია ჩვენს ფრეიმვორკში.

### რატომ გვჭირდება?

ბიზნეს ენტითები ერთმანეთზეა დამოკიდებული. მაგალითად:
- **კონტრაქტისთვის** გჭირდება მომხმარებელი, პროდუქტი და პირობები (terms)
- **ბილინგ რანისთვის** გჭირდება კონტრაქტი
- **ინვოისის რევერსალისთვის** გჭირდება ინვოისი

### როგორ ვიყენებთ?

```typescript
// ნაბიჯი 1: ვქმნით მომხმარებელს
const customerResponse = await Request.post(Endpoints.customer, { data: payload });
await expect(customerResponse).CheckResponse();
Responses.customer.push(await customerResponse.json());  // ← ვინახავთ!

// ნაბიჯი 2: ვქმნით კონტრაქტს (რომელიც ავტომატურად იყენებს Responses.customer[0].id-ს)
const contractPayload = await GeneratePayload.contractsAndOrders.productContract();
const contractResponse = await Request.post(Endpoints.productContract, { data: contractPayload });
await expect(contractResponse).CheckResponse();
Responses.productContract.push(await contractResponse.json());  // ← ვინახავთ!
```

### ResponsesContainer-ის სრული სტრუქტურა

ყველა დომეინისთვის არის ცარიელი მასივი:

```typescript
{
    customer: [],
    pod: [],
    meters: [],
    productContract: [],
    serviceContract: [],
    product: [],
    service: [],
    priceComponent: [],
    terms: [],
    billingRun: [],
    invoice: [],
    // ... და კიდევ 40+ დომეინი
}
```

> 🔴 **ყურადღება:** `Responses`-ში push-ის თანმიმდევრობა მნიშვნელოვანია! payload გენერატორები `[0]` ინდექსით ეძებენ პირველ ელემენტს.

---

## Endpoints — API ბილიკები

ენდპოინტები არის სტრინგ-კონსტანტები. არ დაწერო ენდპოინტის URL ხელით — გამოიყენე `Endpoints` ობიექტი:

```typescript
// ✅ სწორი
await Request.post(Endpoints.customer, { data: payload });

// ❌ არასწორი — არ ჩაწერო ხელით!
await Request.post('customer', { data: payload });
```

### რატომ?
- თუ ენდპოინტი შეიცვალა, ერთ ადგილას გასწორდება
- TypeScript-ის ავტოკომპლიტი გეხმარება სწორი ენდპოინტის პოვნაში
- ბეჭდვის შეცდომების თავიდან აცილება

---

## CheckResponse — API პასუხის შემოწმება

### ყოველთვის გამოიყენე `CheckResponse()`!

```typescript
// ✅ სწორი — დეტალური ინფორმაცია შეცდომის დროს
await expect(response).CheckResponse();

// ❌ არასწორი — შეცდომის დროს მხოლოდ "false" დაბრუნდება
expect(response.ok()).toBeTruthy();
```

### რას აკეთებს CheckResponse?

1. ამოწმებს HTTP სტატუსს (2xx = წარმატება)
2. თუ შეცდომაა, ფორმატირებულ შეტყობინებას დაბეჭდავს:
   - ენდპოინტი და HTTP მეთოდი
   - გაგზავნილი payload (JSON ფორმატში)
   - რესპონსის სტატუს კოდი
   - რესპონსის ბოდი (JSON ფორმატში)
3. ავტომატურად ამოიღებს `[REG-XXX]` ID-ს ტესტის სახელიდან ანგარიშგებისთვის

### შეცდომის მაგალითი:

```
REG-706 ❌ Failed 
Endpoint: https://devapps.energo-pro.bg/backend/phoenix1-dev/customer
Method: POST
Payload: {
  "customerType": "LEGAL_ENTITY",
  "customerIdentifier": "20250310143022",
  ...
}
Status: 400
Response: {
  "message": "Customer identifier already exists",
  ...
}
```

> 🔴 **კრიტიკული წესი:** რესპონსის ბოდი (body) მხოლოდ ერთხელ იკითხება! არასოდეს გამოიძახო `response.json()` `CheckResponse()`-ის **წინ**. მატჩერი თავად ამუშავებს ბოდის კითხვას.

```typescript
// ✅ სწორი
const response = await Request.post(Endpoints.customer, { data: payload });
await expect(response).CheckResponse();
Responses.customer.push(await response.json()); // ← json() ამ ეტაპზე უკვე safe-ია

// ❌ არასწორი — ბოდი უკვე წაკითხულია, CheckResponse ვეღარ მიიღებს
const response = await Request.post(Endpoints.customer, { data: payload });
const body = await response.json(); // ← ადრე წაკითხვა!
await expect(response).CheckResponse(); // ← არ იმუშავებს სრულად
```

---

## ტესტის სახელდება და თეგები

### Jira ID აუცილებელია!

ყველა ტესტს უნდა ჰქონდეს Jira ტიკეტის ID კვადრატულ ფრჩხილებში:

```typescript
test.describe('[REG-55]: Billing', { tag: '@billing' }, () => {
    test('[REG-706]: invoice reversal | happy pass', async ({...}) => {
```

### თეგების გამოყენება

თეგები CI/CD-ში ფილტრაციისთვის გამოიყენება:

| თეგი | დომეინი |
|------|---------|
| `@billing` | ბილინგი |
| `@customer` | მომხმარებლები |
| `@contracts` | კონტრაქტები |
| `@receivable` | მოთხოვნების მართვა |
| `@massImport` | მასს იმპორტი |

### describe-ების იერარქია

```typescript
test.describe('[REG-55]: Billing', { tag: '@billing' }, () => {        // დომეინი
    test.describe('[REG-106]: Billing run', () => {                     // ქვედომეინი
        test.describe('[REG-561]: Per piece', () => {                   // სცენარის ტიპი
            test('[REG-973]: Per Piece - happy pass', async ({...}) => { // კონკრეტული ტესტი
```

---

## test.step() — ნაბიჯები ტესტში

ყოველთვის გამოიყენე `test.step()` ლოგიკური ნაბიჯების გამოსაყოფად:

```typescript
test('[REG-973]: Per Piece', async ({ Request, GeneratePayload, Responses, Endpoints }) => {

    await test.step('მომხმარებლის შექმნა', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal()
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('ფასის კომპონენტის შექმნა', async () => {
        const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.perPiece()
        });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    // ... დანარჩენი ნაბიჯები
});
```

### რატომ არის step() მნიშვნელოვანი?

1. **HTML ანგარიშში ჩანს** — თუ ტესტი ჩავარდა, ზუსტად ნახავ რომელ ნაბიჯზე
2. **გრანულარული debug** — არ გჭირდება მთელი ტესტის გადახედვა
3. **დოკუმენტაცია** — step-ის სახელი აღწერს რა ხდება

---

## ენთითის დამოკიდებულებები

ეს არის ყველაზე მნიშვნელოვანი კონცეფცია რომელიც უნდა გაიგო. ბიზნეს ენტითები იერარქიულ დამოკიდებულებაში არიან.

### ბილინგის მაგალითი (For Volumes)

ბილინგ რანის (billing run) შესაქმნელად, ჯერ უნდა შეიქმნას ყველა წინაპირობა:

```
მომხმარებელი (customer)
    ↓
ფასის კომპონენტი (priceComponent)
    ↓
პირობები (terms)
    ↓
მიწოდების წერტილი (POD)
    ↓
პროდუქტი/სერვისი (product/service)
    ↓
კონტრაქტი (productContract/serviceContract) ← იყენებს ზემოთ შექმნილ ყველაფერს
    ↓
ბილინგ რანი (billingRun) ← იყენებს კონტრაქტს
    ↓
ინვოისის გენერაცია ← ელოდება ბილინგ რანის დასრულებას
```

### პრაქტიკული მაგალითი

```typescript
test('[REG-973]: Billing - For Volumes', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    test.setTimeout(6 * 60 * 1000); // ბილინგს დრო სჭირდება!

    // 1. მომხმარებელი
    await test.step('მომხმარებლის შექმნა', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal()
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    // 2. ფასის კომპონენტი
    await test.step('ფასის კომპონენტის შექმნა', async () => {
        const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.perPiece()
        });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    // 3. პირობები
    await test.step('პირობების შექმნა', async () => {
        const term = await Request.post(Endpoints.terms, {
            data: GeneratePayload.productAndServices.term()
        });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    // 4. POD
    await test.step('POD-ის შექმნა', async () => {
        const pod = await Request.post(Endpoints.pod, {
            data: GeneratePayload.pointsOfDelivery.pod_settlement()
        });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
    });

    // 5. სერვისი
    await test.step('სერვისის შექმნა', async () => {
        const service = await Request.post(Endpoints.service, {
            data: GeneratePayload.productAndServices.service()
        });
        await expect(service).CheckResponse();
        Responses.service.push(await service.json());
    });

    // 6. კონტრაქტი (იყენებს customer, priceComponent, terms, pod, service)
    await test.step('კონტრაქტის შექმნა', async () => {
        const contract = await Request.post(Endpoints.serviceContract, {
            data: await GeneratePayload.contractsAndOrders.serviceContract()
        });
        await expect(contract).CheckResponse();
        Responses.serviceContract.push(await contract.json());
    });

    // 7. ბილინგ რანი (იყენებს კონტრაქტს)
    await test.step('ბილინგ რანის შექმნა', async () => {
        const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['PER_PIECE']);
        const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
        await expect(billingRun).CheckResponse();
        Responses.billingRun.push(await billingRun.json());
    });

    // 8. ინვოისის გენერაცია
    await test.step('ინვოისის გენერაცია', async () => {
        await GeneratePayload.billing.waitForInvoiceGeneration();
    });
});
```

> 💡 **თანმიმდევრობა კრიტიკულია!** თუ ნაბიჯი 6 (კონტრაქტი) ნაბიჯ 1-ის (მომხმარებლის) წინ გაეშვება — ტესტი ჩავარდება, რადგან `Responses.customer[0]` ცარიელი იქნება.

---

## ნომენკლატურები

ნომენკლატურები არის სისტემური საცნობარო მონაცემები: ვალუტები, სტატუსები, პროფილები, საზომი ერთეულები და ა.შ.

### როგორ ვიყენებთ?

```typescript
test('...', async ({ Nomenclatures }) => {
    const profileId = await Nomenclatures.profiles('luka');
    // profileId-ს იყენებ payload-ში
});
```

### რა ხდება კულისებში?

1. სეტაპის დროს ნომენკლატურები ინახება `fixtures/env_variables.json`-ში
2. `envCashed.ts` ექსპორტს უკეთებს ამ მონაცემებს `envVariables` ობიექტად
3. payload შაბლონები ავტომატურად იყენებენ `envVariables`-ს

> 🔴 **არასოდეს გამოიყენო hardcoded ID-ები payload-ებში!** ყოველთვის `Responses` მასივებს ან `Nomenclatures`-ს მიმართე.

---

## რეალური ტესტის ანალიზი

მოდი, გავაანალიზოთ რეალური ტესტი `tests/customers/customer.spec.ts`-იდან:

```typescript
import { test, expect } from "../../fixtures/baseFixture";
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-1]: Customer', { tag: '@customer' }, () => {
    let numberUIC: string | undefined;

    // beforeEach - ტესტებს შორის გაზიარებული ცვლადი
    test.beforeEach(async ({ GeneratePayload }) => {
        const payload = GeneratePayload.customers.customer_private_business();
        numberUIC = payload.customerIdentifier;
    });

    test.describe('[REG-2]: Customer', () => {
        test.describe('[REG-3]: Create - Customer', () => {
            test('[REG-157]: მომხმარებლის შექმნა | მხოლოდ სავალდებულო ველები',
                async ({ Request, GeneratePayload, Endpoints, Responses }) => {
                    // 1. payload-ის გენერაცია
                    const customerData = GeneratePayload.customers.customer_private_business();
                    customerData.customerIdentifier = numberUIC!;

                    // 2. არასავალდებულო ველების null-ად დაყენება
                    customerData.foreign = false;
                    customerData.address.foreignAddressData = null;
                    customerData.bankingDetails = null;
                    customerData.relatedCustomers = null;
                    customerData.communicationData = null;
                    // ... და ა.შ.

                    // 3. API გამოძახება
                    const customer = await Request.post(Endpoints.customer, { data: customerData });
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());

                    // 4. რეპორტში დამატება
                    test.info().attach('[REG-157] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })
        })
    });
});
```

### რას ვსწავლობთ ამ ტესტიდან?

1. **`beforeEach`** — შეგიძლია საერთო ლოგიკა ტესტებს შორის გაიზიარო
2. **payload-ის მოდიფიკაცია** — გენერირებულ payload-ს ხელით ვცვლით ტესტ-სცენარის მიხედვით (null-ები, სხვა მნიშვნელობები)
3. **`test.info().attach()`** — ანგარიშში რესპონსის მიმაგრება debug-ისთვის
4. **`reportGenerator.setLinksToResponses()`** — ენტითის ID-ების ლინკებად გადაქცევა

---

## Mass Import ტესტები

მასს იმპორტი Excel ფაილების ატვირთვაზეა დაფუძნებული:

```typescript
test('[REG-XXX]: მასიური იმპორტი', async ({ MassImportGenerator }) => {
    await test.step('მომხმარებლების მასს იმპორტი', async () => {
        // 1. payload-ის და შაბლონის სახელის მიღება
        const { payload, templateName } = MassImportGenerator
            .CustomerMassImportGenerator.privateCustomerMP();

        // 2. ატვირთვა და შესრულება
        const filePath = await MassImportGenerator
            .uploadAndExecuteMassImport(payload, templateName);
    });
});
```

> 🔴 **Mass Import-ისთვის `FileUploadRequest` გამოიყენება** — არა ჩვეულებრივი `Request`!

---

## ტესტების გაშვება

### სრული pipeline (სეტაპი → ტესტები → ანგარიში)

```bash
npx playwright test
```

### კონკრეტული დომეინი თეგით

```bash
npx playwright test --grep "@billing"
npx playwright test --grep "@customer"
```

### კონკრეტული ტესტი Jira ID-თ

```bash
npx playwright test --grep "\bREG-706\b"
```

### მხოლოდ სეტაპი (ტოკენის და ნომენკლატურების განახლება)

```bash
npx playwright test --project=setup --workers=1
```

### ერთი ფაილის გაშვება

```bash
npx playwright test tests/customers/customer.spec.ts
```

### ვიზუალური ანგარიშის ნახვა

```bash
npx playwright show-report
```

---

## ხშირი შეცდომები და გადაწყვეტები

### 1. ❌ `response.json()` ორჯერ გამოძახება

```typescript
// ❌ არასწორი
const body = await response.json();           // პირველი წაკითხვა
await expect(response).CheckResponse();        // მეორე წაკითხვა — ვეღარ მუშაობს!

// ✅ სწორი
await expect(response).CheckResponse();        // ჯერ შემოწმება
Responses.customer.push(await response.json()); // მერე წაკითხვა
```

**მიზეზი:** HTTP რესპონსის ბოდი stream-ია — მხოლოდ ერთხელ იკითხება.

### 2. ❌ Hardcoded ID-ების გამოყენება

```typescript
// ❌ არასწორი
payload.customerId = 12345; // ეს ID სხვა გარემოში არ იარსებებს!

// ✅ სწორი
payload.customerId = Responses.customer[0].id;
```

### 3. ❌ ენტითების არასწორი თანმიმდევრობა

```typescript
// ❌ არასწორი — კონტრაქტი მომხმარებლის წინ
await test.step('კონტრაქტი', async () => {
    const payload = await GeneratePayload.contractsAndOrders.productContract();
    // 💥 Responses.customer[0] ცარიელია!
});

await test.step('მომხმარებელი', async () => {
    // ეს ნაბიჯი გვიან შესრულდა
});
```

### 4. ❌ სეტაპის პარალელურად გაშვება

```bash
# ❌ არასწორი
npx playwright test --project=setup --workers=4

# ✅ სწორი
npx playwright test --project=setup --workers=1
```

### 5. ❌ `@playwright/test`-იდან იმპორტი

```typescript
// ❌ არასწორი — არ ექნება CheckResponse და ფიქსჩერები
import { test, expect } from '@playwright/test';

// ✅ სწორი
import { test, expect } from '../../fixtures/baseFixture';
```

### 6. ❌ ფაილის ატვირთვა ჩვეულებრივი Request-ით

```typescript
// ❌ არასწორი
const response = await Request.post(endpoint, { data: file });

// ✅ სწორი
const response = await FileUploadRequest.post(endpoint, { multipart: { file } });
```

### 7. ❌ Jira ID-ს არ დაამატებ ტესტის სახელში

```typescript
// ❌ არასწორი — CI/CD ვერ დაფილტრავს
test('Customer creation', async ({...}) => {

// ✅ სწორი
test('[REG-157]: Customer creation | happy pass', async ({...}) => {
```

---

## საუკეთესო პრაქტიკები

### 1. ყოველთვის გამოიყენე `test.step()`
ყოველი ლოგიკური ოპერაცია ცალკე step-ში მოქცეული უნდა იყოს.

### 2. ყურადღებით მიჰყევი ენტითების თანმიმდევრობას
payload გენერატორის კოდი წაიკითხე იმისთვის, რომ გაიგო რა წინაპირობები აქვს.

### 3. ყოველთვის `CheckResponse()`
არასოდეს `.ok()` ან `.toBeTruthy()`.

### 4. ანგარიშის მიმაგრება
`test.info().attach()` გამოიყენე debug-ისთვის.

### 5. Timeout-ის დაყენება
ბილინგი და ხანგრძლივი ტესტები timeout-ს საჭიროებენ:

```typescript
test.setTimeout(6 * 60 * 1000); // 6 წუთი
```

### 6. `response.json()`-ის ქეშირება
თუ ერთი რესპონსი რამდენჯერმე გჭირდება:

```typescript
const body = await response.json();
Responses.customer.push(body);
// body-ს იყენებ რამდენჯერაც გინდა
```

### 7. payload-ის მოდიფიცირება ტესტის მიხედვით
გენერატორი ბაზისურ payload-ს აბრუნებს. შეცვალე ის ტესტის საჭიროების მიხედვით:

```typescript
const payload = GeneratePayload.customers.customer_legal();
payload.foreign = false;          // ტესტ-სცენარის მიხედვით
payload.marketingConsent = true;  // ტესტ-სცენარის მიხედვით
```

---

## ფაილური სტრუქტურის მითითება

| ფაილი/საქაღალდე | აღწერა |
|-----------------|--------|
| `fixtures/baseFixture.ts` | მთავარი ფიქსჩერების განსაზღვრა, import-ის წყარო |
| `fixtures/constants/endpoints.ts` | ყველა API ენდპოინტის კონსტანტა |
| `fixtures/matchers/checkResponse.ts` | CheckResponse კასტომ მატჩერი |
| `fixtures/types/responses.ts` | ResponsesContainer ტიპი და factory |
| `fixtures/token.json` | JWT ავთენტიფიკაციის ტოკენი (სეტაპით იქმნება) |
| `fixtures/env_variables.json` | ნომენკლატურები (სეტაპით იქმნება) |
| `jsons/payloadGenerators/PayloadGenerator.ts` | მთავარი GeneratePayload კლასი |
| `jsons/payloadGenerators/domains/` | დომეინ-სპეციფიური payload გენერატორები |
| `jsons/payloads/create/` | ნედლი payload შაბლონები |
| `mass-imports/generators/` | Excel-ზე დაფუძნებული მასს იმპორტის ლოგიკა |
| `tests/setup/` | გლობალური სეტაპი/ტეარდაუნი |
| `tests/billing/` | ბილინგის ტესტები |
| `tests/customers/` | მომხმარებლების ტესტები |
| `tests/contractsAndOrders/` | კონტრაქტების ტესტები |
| `utils/generateReport.ts` | ანგარიშის პარსინგი, Slack/Jira ინტეგრაცია |
| `utils/randomGens.ts` | რანდომული მონაცემების გენერატორი |
| `utils/RequestWrapper.ts` | Request-ის wrapper მეტადატით |

---

## დასკვნა

ამ დოკუმენტშია ყველაფერი რაც საჭიროა API ტესტების დასაწერად ჩვენს ფრეიმვორკში. მთავარი წესები:

1. **იმპორტი ყოველთვის `baseFixture`-იდან**
2. **`CheckResponse()` ყოველთვის**
3. **`test.step()` ყოველთვის**
4. **Jira ID ყოველთვის ტესტის სახელში**
5. **ენტითების თანმიმდევრობის დაცვა**
6. **`response.json()` მხოლოდ ერთხელ**
7. **Hardcoded ID-ებს კი არა — `Responses` და `Nomenclatures`**

თუ რაიმე არ არის გასაგები — payload გენერატორის კოდი წაიკითხე, იქ ხშირად ყველაფერი ნათლად ჩანს თუ რა ენტითებია საჭირო და როგორ არის ერთმანეთთან დაკავშირებული.
