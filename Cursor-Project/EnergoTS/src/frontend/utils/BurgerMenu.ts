import { Page } from "@playwright/test";

type menuItems = 'Shortcuts' | 'Customers' | 'Points of Delivery' | 'Customer Communication' | 'Contracts and Orders' | 'Product and Services' | 'Energy Data' | 'Billing' | 'Receivables Management' | 'Operations Management' | 'Master Data' | 'System Settings';
type subMenuItems = 'Terms' | 'Energy Products' | 'Price Components' | 'Energy Product Contracts' | 'Billing Run' | 'Tasks' | 'Nomenclature' | 'Points of Delivery' | 'Data by Profiles' | 'Data by Scales' | 'Discount from Duty to Society' | 'Rescheduling' | 'Customer Assessment' | 'Customer liabilities';

export class BurgerMenu {
    private page: Page;
    constructor(page: Page){
        this.page = page;
    }

    private async bgSec(){
        await this.page.locator('.burger').click();
    }

    private async subMenu(innerSec: string){
        await this.page.locator('a').filter({ hasText: new RegExp(`^\\s*${innerSec}\\s*$`) }).click();
    }

    private async menu(sec: string){
        const isMenuOpen = await this.page.locator('div').filter({ hasText: new RegExp(`^\\s*${sec}\\s*$`) }).first().getAttribute('class');
        if(isMenuOpen === 'label active'){
            return;
        }
        await this.page.locator('div').filter({ hasText: new RegExp(`^\\s*${sec}\\s*$`) }).first().click();
    }

    public async Navigate(menu: menuItems, submenu: subMenuItems){
        await this.bgSec();
        await this.menu(menu);
        await this.subMenu(submenu);
        await this.bgSec();
    }
}