import { Locator, Page } from '@playwright/test';
import { dropdownSelector } from '../../utils/dropdownSelector';

export type LegalCustomerCreateArguments = {
    identifier: string;
    name: string;
    nameTransliterated: string;
    legalForm: string;
    mainSubjectOfActivity: string;
    segment: string;
    populatedPlace: string;
    zipCode: string;
    oldCustomerNumber?: string;
    vatNumber?: string;
    publicProcurementLaw?: boolean;
    unrecognizedIdentifier?: boolean;
    marketingConsent?: boolean;
    preferredCommunicationInEnglish?: boolean;
};

export class CustomerCreateBasicPage {
    readonly page: Page;
    private readonly dropdown: dropdownSelector;

    readonly heading: Locator;
    readonly customerTab: Locator;
    readonly customerIndicatorsTab: Locator;
    readonly customerRelationshipTab: Locator;
    readonly contractsTab: Locator;
    readonly pointsOfDeliveryTab: Locator;
    readonly ordersTab: Locator;
    readonly invoicesTab: Locator;
    readonly receivablesAndLiabilitiesTab: Locator;
    readonly paymentsTab: Locator;
    readonly auditLogTab: Locator;
    readonly scrollTabsLeft: Locator;
    readonly scrollTabsRight: Locator;
    readonly breadcrumbHome: Locator;
    readonly breadcrumbCustomers: Locator;
    readonly breadcrumbCurrent: Locator;

    readonly legalEntityRadio: Locator;
    readonly privateCustomerRadio: Locator;

    readonly publicProcurementLaw: Locator;
    readonly businessActivity: Locator;
    readonly unrecognizedIdentifier: Locator;
    readonly gdprConsent: Locator;
    readonly marketingConsent: Locator;
    readonly preferredCommunicationInEnglish: Locator;

    readonly customerNumber: Locator;
    readonly privateCustomerNumber: Locator;
    readonly oldCustomerNumber: Locator;
    readonly privateOldCustomerNumber: Locator;
    readonly uic: Locator;
    readonly checkInApisButton: Locator;
    readonly vatNumber: Locator;
    readonly privateVatNumber: Locator;
    readonly copyFromUicButton: Locator;
    readonly privateCopyFromUicButton: Locator;
    readonly status: Locator;
    readonly privateStatus: Locator;
    readonly name: Locator;
    readonly nameTransliterated: Locator;
    readonly privateName: Locator;
    readonly privateNameTransliterated: Locator;
    readonly middleName: Locator;
    readonly middleNameTransliterated: Locator;
    readonly surname: Locator;
    readonly surnameTransliterated: Locator;
    readonly legalForm: Locator;
    readonly legalFormTransliterated: Locator;
    readonly legalFormDescription: Locator;
    readonly legalFormDescriptionTransliterated: Locator;
    readonly privateLegalForm: Locator;
    readonly privateLegalFormTransliterated: Locator;
    readonly privateLegalFormDescription: Locator;
    readonly privateLegalFormDescriptionTransliterated: Locator;
    readonly formOfOwnership: Locator;
    readonly privateFormOfOwnership: Locator;
    readonly economicBranchCommercial: Locator;
    readonly privateEconomicBranchCommercial: Locator;
    readonly economicBranchNational: Locator;
    readonly privateEconomicBranchNational: Locator;
    readonly mainSubjectOfActivity: Locator;
    readonly privateMainSubjectOfActivity: Locator;
    readonly nameOfBusinessActivity: Locator;
    readonly nameOfBusinessActivityTransliterated: Locator;
    readonly segment: Locator;
    readonly privateSegment: Locator;
    readonly removeSegmentValue: Locator;
    readonly personalNumber: Locator;
    readonly hasPassedKyc: Locator;
    readonly kycExpiration: Locator;
    readonly dateOfBirth: Locator;

    readonly unregisteredAddress: Locator;
    readonly privateUnregisteredAddress: Locator;
    readonly country: Locator;
    readonly region: Locator;
    readonly municipality: Locator;
    readonly populatedPlace: Locator;
    readonly zipCode: Locator;
    readonly district: Locator;
    readonly quarterResidentialArea: Locator;
    readonly quarterList: Locator;
    readonly streetBoulevard: Locator;
    readonly streetList: Locator;
    readonly streetNumber: Locator;
    readonly addressAdditionalInformation: Locator;
    readonly block: Locator;
    readonly entrance: Locator;
    readonly floor: Locator;
    readonly apartment: Locator;
    readonly mailbox: Locator;
    readonly toggleTranslatedAddressFieldsButton: Locator;
    readonly countryTransliterated: Locator;
    readonly regionTransliterated: Locator;
    readonly municipalityTransliterated: Locator;
    readonly populatedPlaceTransliterated: Locator;
    readonly zipCodeTransliterated: Locator;
    readonly districtTransliterated: Locator;
    readonly quarterTypeTransliterated: Locator;
    readonly quarterListTransliterated: Locator;
    readonly streetTypeTransliterated: Locator;
    readonly streetTransliterated: Locator;
    readonly streetNumberTransliterated: Locator;
    readonly addressAdditionalInformationTransliterated: Locator;
    readonly blockTransliterated: Locator;
    readonly entranceTransliterated: Locator;
    readonly floorTransliterated: Locator;
    readonly apartmentTransliterated: Locator;
    readonly mailboxTransliterated: Locator;

    readonly foreignCountry: Locator;
    readonly foreignRegion: Locator;
    readonly foreignMunicipality: Locator;
    readonly foreignPopulatedPlace: Locator;
    readonly foreignZipCode: Locator;
    readonly foreignDistrict: Locator;
    readonly foreignQuarterResidentialArea: Locator;
    readonly foreignStreet: Locator;
    readonly foreignStreetNumber: Locator;
    readonly foreignCountryTransliterated: Locator;
    readonly foreignRegionTransliterated: Locator;
    readonly foreignMunicipalityTransliterated: Locator;
    readonly foreignPopulatedPlaceTransliterated: Locator;
    readonly foreignZipCodeTransliterated: Locator;
    readonly foreignDistrictTransliterated: Locator;
    readonly foreignQuarterTypeTransliterated: Locator;
    readonly foreignQuarterTransliterated: Locator;
    readonly foreignStreetTypeTransliterated: Locator;
    readonly foreignStreetTransliterated: Locator;
    readonly foreignStreetNumberTransliterated: Locator;

    readonly directDebit: Locator;
    readonly bank: Locator;
    readonly bic: Locator;
    readonly iban: Locator;
    readonly declaredConsumption: Locator;
    readonly preference: Locator;
    readonly creditRating: Locator;
    readonly additionalInformation: Locator;

    readonly managersSection: Locator;
    readonly addManagerButton: Locator;
    readonly connectedCustomersSection: Locator;
    readonly ownersSection: Locator;
    readonly addOwnerButton: Locator;
    readonly communicationDataSection: Locator;
    readonly addCommunicationDataButton: Locator;
    readonly accountManagersSection: Locator;
    readonly accountManagerChip: Locator;
    readonly openAccountManagerChip: Locator;
    readonly removeAccountManagerButton: Locator;
    readonly addAccountManagerButton: Locator;
    readonly relatedIndividualsSection: Locator;
    readonly addRelatedIndividualButton: Locator;
    readonly tasksSection: Locator;
    readonly tasksAddButton: Locator;
    readonly tasksRefreshButton: Locator;

    readonly createCustomerButton: Locator;
    readonly cancelButton: Locator;

    readonly managersDialog: Locator;
    readonly ownersDialog: Locator;
    readonly communicationDataDialog: Locator;
    readonly accountManagersDialog: Locator;
    readonly relatedIndividualsDialog: Locator;
    readonly removeConfirmationDialog: Locator;

    constructor(page: Page, dropdown: dropdownSelector) {
        this.page = page;
        this.dropdown = dropdown;

        this.heading = page.getByRole('heading', { level: 1 });
        this.customerTab = page.getByRole('link', { name: 'Customer', exact: true });
        this.customerIndicatorsTab = page.getByRole('link', { name: 'Customer indicators', exact: true });
        this.customerRelationshipTab = page.getByRole('link', { name: 'Customer relationship', exact: true });
        this.contractsTab = page.getByRole('link', { name: 'Contracts', exact: true });
        this.pointsOfDeliveryTab = page.getByRole('link', { name: 'Points of Delivery', exact: true });
        this.ordersTab = page.getByRole('link', { name: 'Orders', exact: true });
        this.invoicesTab = page.getByRole('link', { name: 'Invoices', exact: true });
        this.receivablesAndLiabilitiesTab = page.getByRole('link', { name: 'Receivables and Liabilities', exact: true });
        this.paymentsTab = page.getByRole('link', { name: 'Payments', exact: true });
        this.auditLogTab = page.getByRole('link', { name: 'Audit Log', exact: true });
        this.scrollTabsLeft = page.locator('img[src$="left-arrow.svg"]').locator('xpath=..');
        this.scrollTabsRight = page.locator('img[src$="right-arrow.svg"]').locator('xpath=..');
        this.breadcrumbHome = page.getByText('Home >', { exact: true });
        this.breadcrumbCustomers = page.getByText('Customers >', { exact: true });
        this.breadcrumbCurrent = page.getByText('Customers Create Basic', { exact: true });

        this.legalEntityRadio = page.getByRole('radio', { name: 'Legal Entity' });
        this.privateCustomerRadio = page.getByRole('radio', { name: 'Private Customer' });

        this.publicProcurementLaw = page.locator('phx-checkbox#publicProcurementLawCheckBox');
        this.businessActivity = page.locator('phx-checkbox#businessActivityCheckBox');
        this.unrecognizedIdentifier = page.locator('phx-checkbox#foreignEntityCheckBox');
        this.gdprConsent = page.locator('phx-checkbox#gdprConsentCheckBox');
        this.marketingConsent = page.locator('phx-checkbox#consentToMarketingCommunicationCheckBox');
        this.preferredCommunicationInEnglish = page.locator('phx-checkbox#preferCommunicationInEnglish');

        this.customerNumber = page.locator('#legalEntityNumberInput');
        this.privateCustomerNumber = page.locator('#privateCustomerNumberInput');
        this.oldCustomerNumber = page.locator('#legalEntityOldCustomerNumber');
        this.privateOldCustomerNumber = page.locator('#privateCustomerOldCustomerNumber');
        this.uic = page.locator('#uicLegalEntity');
        this.checkInApisButton = page.locator('#checkInApisButton');
        this.vatNumber = page.locator('#legalEntityVatNumber');
        this.privateVatNumber = page.locator('#privateCustomerVatNumber');
        this.copyFromUicButton = page.locator('#legalEntityCopyFromUic');
        this.privateCopyFromUicButton = page.locator('#privateCustomerCopyFromUic');
        this.status = page.locator('#legalEntityCustomerDetailStatus');
        this.privateStatus = page.locator('#privateCustomerDetailStatus');
        this.name = page.locator('textarea#legalEntityNameTextArea');
        this.nameTransliterated = page.locator('textarea#legalEntityNameTransliteratedTextArea');
        this.privateName = page.locator('#privateCustomerNameInput');
        this.privateNameTransliterated = page.locator('#privateCustomerNameTransliteratedInput');
        this.middleName = page.locator('#middleNameInput');
        this.middleNameTransliterated = page.locator('#middleNameTransliteratedInput');
        this.surname = page.locator('#surnameInput');
        this.surnameTransliterated = page.locator('#surnameTransliteratedInput');
        this.legalForm = page.locator('#legalEntityLegalFormId');
        this.legalFormTransliterated = page.locator('#legalEntityLegalFormTransId');
        this.legalFormDescription = page.locator('#legalFormInputDisabled');
        this.legalFormDescriptionTransliterated = page.locator('#legalFormTransliteratedInputDisabled');
        this.privateLegalForm = page.locator('#privateCustomerLegalFormId');
        this.privateLegalFormTransliterated = page.locator('#privateCustomerLegalFormTransId');
        this.privateLegalFormDescription = page.locator('#legalFormDescriptionInput');
        this.privateLegalFormDescriptionTransliterated = page.locator('#legalFormDescriptionTransliteratedInput');
        this.formOfOwnership = page.locator('#legalEntityOwnershipForm');
        this.privateFormOfOwnership = page.locator('#privateCustomerOwnershipForm');
        this.economicBranchCommercial = page.locator('#legalEntityEconomicBranchCi');
        this.privateEconomicBranchCommercial = page.locator('#privateCustomerEconomicBranchCi');
        this.economicBranchNational = page.locator('#legalEntityEconomicBranchNcea');
        this.privateEconomicBranchNational = page.locator('#privateCustomerEconomicBranchNcea');
        this.mainSubjectOfActivity = page.locator('textarea#legalEntityMainSubjectOfActivityTextArea');
        this.privateMainSubjectOfActivity = page.locator('textarea#privateCustomerMainSubjectOfActivityTextArea');
        this.nameOfBusinessActivity = page.locator('textarea#nameOfBusinessActivityTextArea');
        this.nameOfBusinessActivityTransliterated = page.locator('textarea#nameOfBusinessActivityTransliteratedTextArea');
        this.segment = page.locator('#legalEntitySegmentDropdown');
        this.privateSegment = page.locator('#privateCustomerSegmentsDropdown');
        this.removeSegmentValue = page.locator('#legalEntitySegmentDropdown, #privateCustomerSegmentsDropdown').locator('img[alt="remove"]');
        this.personalNumber = page.locator('#personalNumber');
        this.hasPassedKyc = page.locator('#privateCustomerHasPassedKyc');
        this.kycExpiration = page.locator('#privateCustomerKycExpiration');
        this.dateOfBirth = page.locator('#privateCustomerDateOfBirth');

        const address = page.locator('phx-headquarters-address-management');
        this.unregisteredAddress = page.locator('phx-checkbox#legalEntityForeignersAddressCheckBox');
        this.privateUnregisteredAddress = page.locator('phx-checkbox#privateCustomerForeignAddressCheckbox');
        this.country = page.locator('#countryId');
        this.region = page.locator('#regionId');
        this.municipality = page.locator('#municipalityId');
        this.populatedPlace = page.locator('#populatedPlace');
        this.zipCode = page.locator('phx-with-request-dropdown').filter({ has: page.getByText('ZIP code', { exact: true }) }).locator('input');
        this.district = page.locator('#districtId');
        this.quarterResidentialArea = page.locator('#quarter-residential-area-types');
        this.quarterList = page.locator('#quarterListOfDropDown');
        this.streetBoulevard = page.locator('#street-boulevard-types');
        this.streetList = address.locator('label', { hasText: /^\s*List of Street\/ Boulevard\s*$/ }).locator('xpath=following-sibling::*[1]//input').first();
        this.streetNumber = page.locator('#headquartersAddressNumberOfStreet');
        this.addressAdditionalInformation = page.locator('#headquartersAddressAdditionalInfo');
        this.block = page.locator('#headquartersAddressBlock');
        this.entrance = page.locator('#headquartersAddressEntrance');
        this.floor = page.locator('#headquartersAddressFloor');
        this.apartment = page.locator('#headquartersAddressApartment');
        this.mailbox = page.locator('#headquartersAddressMailbox');
        this.toggleTranslatedAddressFieldsButton = page.locator('button').filter({
            has: page.locator('img[alt="Show fields"], img[alt="Hide fields"]'),
        });
        this.countryTransliterated = page.locator('#countryLocalTransl');
        this.regionTransliterated = page.locator('#regionLocalTransl');
        this.municipalityTransliterated = page.locator('#municipalityLocalTransl');
        this.populatedPlaceTransliterated = page.locator('#populatedPlaceLocalTransl');
        this.zipCodeTransliterated = page.locator('#zipCodeLocalTransl');
        this.districtTransliterated = page.locator('#districtLocalTransl');
        this.quarterTypeTransliterated = page.locator('#quarterResidentialAreaTypeLocalTransl');
        this.quarterListTransliterated = page.locator('#residentialAreaLocalTransl');
        this.streetTypeTransliterated = page.locator('#streetBoulevardTypeLocalTransl');
        this.streetTransliterated = page.locator('#streetLocalTransl');
        this.streetNumberTransliterated = page.locator('#streetNumberLocalTransl');
        this.addressAdditionalInformationTransliterated = page.locator('#addressAdditionalInfoTransl');
        this.blockTransliterated = page.locator('#blockTransl');
        this.entranceTransliterated = page.locator('#entranceTransl');
        this.floorTransliterated = page.locator('#floorTransl');
        this.apartmentTransliterated = page.locator('#apartmentTransl');
        this.mailboxTransliterated = page.locator('#mailboxTransl');

        this.foreignCountry = page.locator('#foreignCustomerCommonCountry');
        this.foreignRegion = page.locator('#foreignCustomerRegion');
        this.foreignMunicipality = page.locator('#foreignCustomerMunicipality');
        this.foreignPopulatedPlace = page.locator('#foreignCustomerPopulatedPlace');
        this.foreignZipCode = page.locator('#foreignCustomerZipCode');
        this.foreignDistrict = page.locator('#foreignCustomerDistrict');
        this.foreignQuarterResidentialArea = page.locator('#foreignCustomerQuarterResidentialArea');
        this.foreignStreet = page.locator('#foreignCustomerListOfStreet');
        this.foreignStreetNumber = page.locator('#foreignCustomerStreetNumber');
        this.foreignCountryTransliterated = page.locator('#countryForeignTransl');
        this.foreignRegionTransliterated = page.locator('#regionForeignTransl');
        this.foreignMunicipalityTransliterated = page.locator('#municipalityForeignTransl');
        this.foreignPopulatedPlaceTransliterated = page.locator('#populatedPlaceForeignTransl');
        this.foreignZipCodeTransliterated = page.locator('#zipCodeForeignTransl');
        this.foreignDistrictTransliterated = page.locator('#districtForeignTransl');
        this.foreignQuarterTypeTransliterated = page.locator('#quarterResidentialAreaTypeForeignTransl');
        this.foreignQuarterTransliterated = page.locator('#residentialAreaForeignTransl');
        this.foreignStreetTypeTransliterated = page.locator('#streetBoulevardTypeForeignTransl');
        this.foreignStreetTransliterated = page.locator('#streetForeignTransl');
        this.foreignStreetNumberTransliterated = page.locator('#streetNumberForeignTransl');

        this.directDebit = page.locator('phx-checkbox#headquartersAddressDirectDebitCheckBox');
        this.bank = page.locator('#bankId');
        this.bic = page.locator('#commonCustomerBic');
        this.iban = page.locator('#commonCustomerIban');
        this.declaredConsumption = page.locator('#declaredEstimatedConsumptionInMwh');
        this.preference = page.locator('#preferenceDropDown');
        this.creditRating = page.locator('#creditRatingDropDown');
        this.additionalInformation = page.getByRole('textbox', { name: 'Additional information' });

        this.managersSection = this.section('Managers');
        this.addManagerButton = this.managersSection.locator('button.circle.add');
        this.connectedCustomersSection = this.section('Group of connected customers');
        this.ownersSection = this.section('Owners(Partners)');
        this.addOwnerButton = this.ownersSection.locator('button.circle.add');
        this.communicationDataSection = this.section('Communication Data');
        this.addCommunicationDataButton = this.communicationDataSection.locator('button.circle.add');
        this.accountManagersSection = this.section('Account Managers');
        this.accountManagerChip = this.accountManagersSection.locator('p');
        this.openAccountManagerChip = this.accountManagersSection.locator('img[alt="Arrow Up Right"]');
        this.removeAccountManagerButton = this.accountManagersSection.locator('img[src$="circle-remove.svg"]').locator('xpath=ancestor::button[1]');
        this.addAccountManagerButton = this.accountManagersSection.locator('button.circle.add');
        this.relatedIndividualsSection = this.section('Related (Connected) individuals');
        this.addRelatedIndividualButton = this.relatedIndividualsSection.locator('button.circle.add');
        this.tasksSection = this.section('Tasks');
        this.tasksAddButton = this.tasksSection.locator('button').nth(0);
        this.tasksRefreshButton = this.tasksSection.locator('img[src$="circle-refresh-copy.svg"]').locator('xpath=ancestor::button[1]');

        this.createCustomerButton = page.locator('#createCustomerButton');
        this.cancelButton = page.locator('#cancelButtonCreateCustomerPage');

        this.managersDialog = this.dialog('Managers');
        this.ownersDialog = this.dialog('Owners(Partners)');
        this.communicationDataDialog = this.dialog('Communication Data');
        this.accountManagersDialog = this.dialog('Account Managers');
        this.relatedIndividualsDialog = this.dialog('Related (connected) individuals');
        this.removeConfirmationDialog = this.dialog('Attention!');
    }

    managerTitle(): Locator {
        return this.managersDialog.locator('#customersManagersModalTitleDropdown');
    }

    managerUnrecognizedIdentifier(): Locator {
        return this.managersDialog.locator('#selectAllLabel');
    }

    managerUnrecognizedYes(): Locator {
        return this.managersDialog.getByRole('radio', { name: 'Yes', exact: true });
    }

    managerUnrecognizedNo(): Locator {
        return this.managersDialog.getByRole('radio', { name: 'No', exact: true });
    }

    managerKycExpiration(): Locator {
        return this.labeledControl(this.managersDialog, 'Expiration of the KYC');
    }

    managerName(): Locator {
        return this.managersDialog.locator('#customersManagersModalName');
    }

    managerMiddleName(): Locator {
        return this.managersDialog.locator('#customersManagersModalMiddleName');
    }

    managerSurname(): Locator {
        return this.managersDialog.locator('#customersManagersModalSurname');
    }

    managerPersonalNumber(): Locator {
        return this.managersDialog.locator('#customersManagersPersonalNum');
    }

    managerJobPosition(): Locator {
        return this.managersDialog.locator('#customersManagersPosition');
    }

    managerPositionHeldFrom(): Locator {
        return this.labeledControl(this.managersDialog, 'Position held from');
    }

    managerPositionHeldTo(): Locator {
        return this.labeledControl(this.managersDialog, 'Position held to');
    }

    managerDateOfBirth(): Locator {
        return this.labeledControl(this.managersDialog, 'Date of birth');
    }

    managerRepresentationMethod(): Locator {
        return this.labeledControl(this.managersDialog, 'Method of representation');
    }

    managerAdditionalInformation(): Locator {
        return this.managersDialog.locator('#customersManagersAdditionalInformation');
    }

    managerMobileNumber(): Locator {
        return this.managersDialog.locator('#customersManagersMobileNumbers-0');
    }

    managerEmail(): Locator {
        return this.managersDialog.locator('#customersManagersEmails-0');
    }

    managerSaveButton(): Locator {
        return this.managersDialog.locator('#customersManagersSaveButton');
    }

    managerCancelButton(): Locator {
        return this.managersDialog.locator('#customersManagersCancelButton');
    }

    ownerIdentifier(): Locator {
        return this.ownersDialog.locator('#input');
    }

    ownerSearchButton(): Locator {
        return this.ownersDialog.getByRole('button', { name: 'Search' });
    }

    ownerName(): Locator {
        return this.ownersDialog.locator('#disabledInput');
    }

    ownerCapitalShare(): Locator {
        return this.ownersDialog.locator('#inputField');
    }

    ownerAdditionalInformation(): Locator {
        return this.labeledControl(this.ownersDialog, 'Additional information');
    }

    ownerSaveButton(): Locator {
        return this.ownersDialog.getByRole('button', { name: 'Save', exact: true });
    }

    ownerCancelButton(): Locator {
        return this.ownersDialog.getByRole('button', { name: 'Cancel', exact: true });
    }

    communicationContactType(): Locator {
        return this.communicationDataDialog.locator('#communicationDataContactTypeName');
    }

    communicationPurpose(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Purpose of contact');
    }

    communicationUnregisteredAddress(): Locator {
        return this.communicationDataDialog.locator('#communicationDataForeignAddressCheckBoxLabel');
    }

    communicationCountry(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Country');
    }

    communicationRegion(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Region');
    }

    communicationMunicipality(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Municipality');
    }

    communicationPopulatedPlace(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Populated place (City, Town, Village, Place)');
    }

    communicationZipCode(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'ZIP code');
    }

    communicationDistrict(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'District');
    }

    communicationQuarter(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Quarter / Residential Area');
    }

    communicationQuarterList(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'List of Quarter/Residential Area');
    }

    communicationStreet(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Street / boulevard');
    }

    communicationStreetList(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'List of Street/ Boulevard');
    }

    communicationStreetNumber(): Locator {
        return this.communicationDataDialog.locator('#communicationDataNumber');
    }

    communicationAdditionalInformation(): Locator {
        return this.communicationDataDialog.locator('#communicationDataAdditionalInformation');
    }

    communicationBlock(): Locator {
        return this.communicationDataDialog.locator('#communicationDataBlock');
    }

    communicationEntrance(): Locator {
        return this.communicationDataDialog.locator('#communicationDataEntrance');
    }

    communicationFloor(): Locator {
        return this.communicationDataDialog.locator('#communicationDataFloor');
    }

    communicationApartment(): Locator {
        return this.communicationDataDialog.locator('#communicationDataApartment');
    }

    communicationMailbox(): Locator {
        return this.communicationDataDialog.locator('#communicationDataMailbox');
    }

    communicationLatitude(): Locator {
        return this.communicationDataDialog.locator('#communicationDataLatitude');
    }

    communicationLongitude(): Locator {
        return this.communicationDataDialog.locator('#communicationDataLongitude');
    }

    communicationPrecision(): Locator {
        return this.labeledControl(this.communicationDataDialog, 'Precision');
    }

    communicationMobileNumber(): Locator {
        return this.communicationDataDialog.locator('#communicationDataMobileNumber-0');
    }

    communicationSms(): Locator {
        return this.communicationDataDialog.locator('phx-checkbox').filter({ hasText: 'SMS' });
    }

    communicationLandlinePhone(): Locator {
        return this.communicationDataDialog.locator('#communicationDataLandLinePhone-0');
    }

    communicationCallCenter(): Locator {
        return this.communicationDataDialog.locator('#communicationDataCallCenter-0');
    }

    communicationFax(): Locator {
        return this.communicationDataDialog.locator('#communicationDataFax-0');
    }

    communicationEmail(): Locator {
        return this.communicationDataDialog.locator('#communicationDataEmail-0');
    }

    communicationWebsite(): Locator {
        return this.communicationDataDialog.locator('#communicationDataWeb-0');
    }

    communicationOtherPlatforms(): Locator {
        return this.communicationDataDialog.locator('#communicationDataOtherPlatforms-0');
    }

    addCommunicationContactPersonButton(): Locator {
        return this.communicationDataDialog.locator('#communicationDataContactPersonAddButton');
    }

    communicationSaveButton(): Locator {
        return this.communicationDataDialog.locator('#communicationDataSaveButton');
    }

    communicationCancelButton(): Locator {
        return this.communicationDataDialog.locator('#communicationDataCancelButton');
    }

    accountManagerName(): Locator {
        return this.accountManagersDialog.locator('#accountManagersModalAccountManagerId');
    }

    accountManagerBusinessUnit(): Locator {
        return this.accountManagersDialog.locator('#accountManagersBusinessUnit');
    }

    accountManagerOrganizationalUnit(): Locator {
        return this.accountManagersDialog.locator('#accountManagersOrganizationUnit');
    }

    accountManagerType(): Locator {
        return this.accountManagersDialog.locator('#accountManagersType');
    }

    accountManagerSaveButton(): Locator {
        return this.accountManagersDialog.locator('#accountManagersSaveButton');
    }

    accountManagerCancelButton(): Locator {
        return this.accountManagersDialog.locator('#accountManagersCancelButton');
    }

    relatedIndividualIdentifier(): Locator {
        return this.relatedIndividualsDialog.locator('#input');
    }

    relatedIndividualName(): Locator {
        return this.relatedIndividualsDialog.locator('#disabledInput');
    }

    relatedIndividualConnectionType(): Locator {
        return this.relatedIndividualsDialog.locator('#ciConnectionTypeIdDiv');
    }

    removeConfirmationYesButton(): Locator {
        return this.removeConfirmationDialog.getByRole('button', { name: 'Yes', exact: true });
    }

    removeConfirmationNoButton(): Locator {
        return this.removeConfirmationDialog.getByRole('button', { name: 'No', exact: true });
    }

    async selectLegalEntity(): Promise<void> {
        await this.selectCustomerType('Legal Entity');
    }

    async selectPrivateCustomer(): Promise<void> {
        await this.selectCustomerType('Private Customer');
    }

    async toggleTranslatedAddressFields(): Promise<void> {
        await this.toggleTranslatedAddressFieldsButton.click();
    }

    async toggleBusinessActivity(): Promise<void> {
        await this.businessActivity.click();
    }

    async toggleUnregisteredAddress(): Promise<void> {
        if (await this.unregisteredAddress.isVisible()) {
            await this.unregisteredAddress.click();
            return;
        }
        await this.privateUnregisteredAddress.click();
    }

    async openManagersDialog(): Promise<void> {
        await this.addManagerButton.click();
        await this.managersDialog.waitFor({ state: 'visible' });
    }

    async openOwnersDialog(): Promise<void> {
        await this.addOwnerButton.click();
        await this.ownersDialog.waitFor({ state: 'visible' });
    }

    async openCommunicationDataDialog(): Promise<void> {
        await this.addCommunicationDataButton.click();
        await this.communicationDataDialog.waitFor({ state: 'visible' });
    }

    async openAccountManagersDialog(): Promise<void> {
        await this.addAccountManagerButton.click();
        await this.accountManagersDialog.waitFor({ state: 'visible' });
    }

    async openRelatedIndividualsDialog(): Promise<void> {
        await this.selectPrivateCustomer();
        await this.addRelatedIndividualButton.click();
        await this.relatedIndividualsDialog.waitFor({ state: 'visible' });
    }

    async dismissDialog(): Promise<void> {
        const dialog = this.page.locator('mat-dialog-container');
        const cancel = dialog.getByRole('button', { name: 'Cancel', exact: true });
        if (await cancel.count()) {
            await cancel.click();
            return;
        }
        const no = dialog.getByRole('button', { name: 'No', exact: true });
        if (await no.count()) {
            await no.click();
            return;
        }
        await this.page.keyboard.press('Escape');
    }

    private section(heading: string): Locator {
        return this.page.getByRole('heading', { name: heading, exact: true }).locator('xpath=ancestor::article[1]');
    }

    private dialog(title: string): Locator {
        return this.page.locator('mat-dialog-container').filter({
            has: this.page.getByRole('heading', { name: title, exact: true }),
        });
    }

    private labeledControl(root: Locator, label: string): Locator {
        const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return root.locator('label', { hasText: new RegExp(`^\\s*${escaped}\\s*$`) }).locator('xpath=..');
    }

    private async selectCustomerType(name: 'Legal Entity' | 'Private Customer'): Promise<void> {
        const group = this.page.locator('#createCustomerRadioButtons');
        const selected = group.locator('.radio-checked');
        const selectedText = (await selected.locator('xpath=..').innerText()).replace(/\s+/g, ' ').trim();
        if (selectedText.includes(name)) {
            return;
        }
        await group.getByText(name, { exact: true }).click();
    }

    private async fillCustomerIdentifier(identifier: string): Promise<void> {
        await this.uic.fill(identifier);
    }

    private async fillLegalCustomerName(name: string): Promise<void> {
        await this.name.fill(name);
    }

    private async fillLegalCustomerNameTransliterated(name: string): Promise<void> {
        await this.nameTransliterated.fill(name);
    }

    private async fillLegalCustomerMainSubject(value: string): Promise<void> {
        await this.mainSubjectOfActivity.fill(value);
    }

    private async selectLegalForm(value: string): Promise<void> {
        await this.legalForm.click();
        const option = this.page.locator('ul.absolute:visible li').filter({ hasText: new RegExp(`^\\s*${value}\\s*$`) });
        await option.waitFor({ state: 'visible' });
        await option.click();
    }

    private async selectLegalCustomerSegment(value: string): Promise<void> {
        const selectedValue = this.segment.locator('.ng-value-label').filter({ hasText: value });
        if (await selectedValue.count()) {
            return;
        }

        await this.segment.locator('.ng-select').click();
        const option = this.page.locator('.ng-dropdown-panel .ng-option').filter({ hasText: value }).first();
        await option.waitFor({ state: 'visible' });
        await option.click();
        await this.page.keyboard.press('Tab');
        await selectedValue.waitFor({ state: 'visible' });
    }

    private async selectAddressValue(field: Locator, value: string): Promise<void> {
        await field.click();
        await field.fill(value);
        const option = this.page.locator('ul.absolute:visible li').filter({
            hasText: new RegExp(`^\\s*${value}\\s*$`),
        });
        await option.waitFor({ state: 'visible' });
        await option.click();
    }

    private async selectHeadquartersPopulatedPlace(value: string): Promise<void> {
        await this.selectAddressValue(this.populatedPlace, value);
    }

    private async selectHeadquartersZipCode(value: string): Promise<void> {
        await this.zipCode.click();
        await this.zipCode.pressSequentially(value, { delay: 20 });
        const list = this.page.locator('ul.absolute').last();
        await list.waitFor({ state: 'visible' });
        const matchingOption = list.locator('li').filter({ hasText: value });
        const option = (await matchingOption.count()) ? matchingOption.first() : list.locator('li').first();
        await option.click();
    }

    private async enableCheckbox(checkbox: Locator, enabled: boolean | undefined): Promise<void> {
        if (enabled) {
            await checkbox.click();
        }
    }

    private async fillOptionalField(field: Locator, value: string | undefined): Promise<void> {
        if (value) {
            await field.fill(value);
        }
    }

    async createLegalCustomer(args: LegalCustomerCreateArguments): Promise<string> {
        await this.selectLegalEntity();
        await this.enableCheckbox(this.publicProcurementLaw, args.publicProcurementLaw);
        await this.enableCheckbox(this.unrecognizedIdentifier, args.unrecognizedIdentifier);
        await this.enableCheckbox(this.marketingConsent, args.marketingConsent);
        await this.enableCheckbox(
            this.preferredCommunicationInEnglish,
            args.preferredCommunicationInEnglish,
        );

        await this.fillCustomerIdentifier(args.identifier);
        await this.fillOptionalField(this.oldCustomerNumber, args.oldCustomerNumber);
        await this.fillOptionalField(this.vatNumber, args.vatNumber);
        await this.fillLegalCustomerName(args.name);
        await this.fillLegalCustomerNameTransliterated(args.nameTransliterated);
        await this.selectLegalForm(args.legalForm);
        await this.fillLegalCustomerMainSubject(args.mainSubjectOfActivity);
        await this.selectLegalCustomerSegment(args.segment);
        await this.selectHeadquartersPopulatedPlace(args.populatedPlace);
        await this.selectHeadquartersZipCode(args.zipCode);
        await this.createCustomerButton.click();

        return args.identifier;
    }
}
