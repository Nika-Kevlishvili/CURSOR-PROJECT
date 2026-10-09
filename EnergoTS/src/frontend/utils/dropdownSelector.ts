import { expect, Locator, Page } from "@playwright/test";

type dropdownOption = 'option1' | string;
type multiselectOptions = 'option1' | 'Select All' | string;

export class dropdownSelector {
    private page: Page;
    
    private phxWithRequestDropdown: Locator;

    private phxDropdown: Locator;
    private phxDropdownOptions: Locator;

    private phxBaseDropdown: Locator;

    private phxMultiselectDropdown: Locator;
    private phxmultiselectDropdownOptions: Locator;

    private dropdownOption: Locator;


    constructor(page: Page) {
        this.page = page;

        this.phxDropdown = this.page.locator('phx-dropdown');
        this.phxDropdownOptions = this.page.locator('phx-dropdown-options > ul');

        this.phxBaseDropdown = this.page.locator('phx-base-dropdown');
        this.phxWithRequestDropdown = this.page.locator('phx-with-request-dropdown > section');

        this.phxMultiselectDropdown = this.page.locator('phx-multiselect-dropdown');
        this.phxmultiselectDropdownOptions = this.page.locator('phx-multiselect-dropdown-options > ul');

        this.dropdownOption = this.page.locator('phx-base-dropdown-options > ul > li > div > p ');
    }

    public async Select_PhxMultiselectDropdown(dropdownName: string, optionName: multiselectOptions) {
        const nameofdropdown = this.phxMultiselectDropdown.locator(`//label[text()="${dropdownName}"]`)
        await nameofdropdown.locator('+ div').click();

        await expect(this.phxmultiselectDropdownOptions).toBeVisible({timeout: 2000});
        try {
            await this.phxmultiselectDropdownOptions.locator('label').filter({ hasText: optionName }).click();
            await this.page.keyboard.press('Tab');
        }catch {
            throw new Error(`Option "${optionName}" not found in dropdown "${nameofdropdown}"`);
        }

    }

    public async SelectPhxDropdown(dropdownName: string, optionName: dropdownOption) {
        const nameofdropdown = this.phxDropdown.locator('//label[text()="' + dropdownName + '"]');
        await nameofdropdown.locator('../div').click();

        await expect(this.phxDropdownOptions).toBeVisible({timeout: 2000});

        try {
            await nameofdropdown.locator('../div/input').fill(optionName, { timeout: 500 });
            await expect(this.phxDropdownOptions.locator('li')).toHaveCount(1, { timeout: 1000 });
            await this.page.keyboard.press('Enter');
        } catch {
            const option = this.phxDropdownOptions.locator('li').filter({ hasText: optionName });
            await expect(option).toBeVisible({ timeout: 1000 });
            await option.click();

        }
    }

    async selectphxWithRequestDropdown(dropdownName: string, optionName: dropdownOption, typeIn?: boolean, ) {
        const nameOfDropdown = this.phxWithRequestDropdown.filter({hasText: `${dropdownName}`});
        await nameOfDropdown.locator('div').nth(0).click();
        await expect(this.page.locator('ul.absolute.w-full')).toBeVisible({timeout: 4000});

        try{
            const dropdownInput = nameOfDropdown.locator('//div/div/input')
            await dropdownInput.click({timeout: 1000});
            typeIn ? await dropdownInput.pressSequentially(optionName) : await dropdownInput.fill(optionName)

            await expect(this.page.locator('phx-base-dropdown-options > ul > li')).toHaveCount(1);
            await this.page.keyboard.press('Enter');
        }catch {
            await expect(this.dropdownOption.filter({hasText: optionName})).toBeVisible()
            await this.dropdownOption.filter({hasText: optionName}).click()
        }
    }

    async select_phxBaseDropdown(dropdownName: string, optionName: string) {
        await this.phxBaseDropdown.locator(`/section[label[text()='${dropdownName}']]//div`).nth(0).click();
        await this.dropdownOption.filter({ has: this.page.locator(`text="${optionName}"`)}).click();
    }

    async selectMultiSelectDropdown(dropdownName: string, optionName: multiselectOptions) {
        await this.page.locator(`//phx-multiselect-dropdown/div/label[text()='${dropdownName}']/following-sibling::div`).click();

        try{
            await this.page.locator('//phx-multiselect-dropdown/div/div//input').click({timeout: 1000});
            await this.page.locator('//phx-multiselect-dropdown/div/div//input').fill(optionName);
            await this.page.locator('phx-multiselect-dropdown-options > ul > li > phx-checkbox').filter({hasText: `${optionName}`}).click();
            await this.page.locator('phx-multiselect-dropdown-options').press('Tab')
        }catch {
            await this.page.locator('phx-multiselect-dropdown-options > ul > li').filter({hasText: `${optionName}`}).click({timeout: 1000});
            await this.page.locator('phx-multiselect-dropdown-options').press('Tab')
        }
    }

}