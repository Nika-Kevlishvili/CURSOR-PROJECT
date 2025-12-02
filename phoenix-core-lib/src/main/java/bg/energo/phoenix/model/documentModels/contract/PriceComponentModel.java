package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.PriceComponentResponse;

public class PriceComponentModel {
    public String TAG;
    public String CurrencyPrintName;
    public String CurrencyAbr;
    public String CurrencyFullName;
    public String Price;
    public String PriceWithWords;
    public String PriceComponentName;
    public String PriceComponentNameTemplates;
    public String X1Desc;
    public String X1Value;
    public String X2Desc;
    public String X2Value;
    public String X3Desc;
    public String X3Value;
    public String X4Desc;
    public String X4Value;
    public String X5Desc;
    public String X5Value;
    public String X6Desc;
    public String X6Value;
    public String X7Desc;
    public String X7Value;
    public String X8Desc;
    public String X8Value;

    public PriceComponentModel from(PriceComponentResponse response) {
        this.TAG = response.getTAG();
        this.Price = response.getPrice();
        this.CurrencyAbr = response.getCurrencyAbr();
        this.CurrencyFullName = response.getCurrencyFullName();
        this.CurrencyPrintName = response.getCurrencyPrintName();
        this.PriceComponentName = response.getPriceComponentName();
        this.PriceWithWords = response.getPriceWithWords();
        this.PriceComponentNameTemplates = response.getPriceComponentNameTemplates();
        this.X1Desc = response.getX1Desc();
        this.X1Value = response.getX1Value();
        this.X2Desc = response.getX2Desc();
        this.X2Value = response.getX2Value();
        this.X3Desc = response.getX3Desc();
        this.X3Value = response.getX3Value();
        this.X4Desc = response.getX4Desc();
        this.X4Value = response.getX4Value();
        this.X5Desc = response.getX5Desc();
        this.X5Value = response.getX5Value();
        this.X6Desc = response.getX6Desc();
        this.X6Value = response.getX6Value();
        this.X7Desc = response.getX7Desc();
        this.X7Value = response.getX7Value();
        this.X8Desc = response.getX8Desc();
        this.X8Value = response.getX8Value();
        return this;
    }
}
