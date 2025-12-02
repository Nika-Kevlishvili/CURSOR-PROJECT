package bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel;

import lombok.Getter;
@Getter
public enum SettlementPeriodHours {
    ALL_HOURS(-1),
    ONE(1),
    TWO(2),
    THREE(3),
    THREE_ADDITIONAL(3),
    FOUR(4),
    FOUR_ADDITIONAL(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    ELEVEN(11),
    TWELVE(12),
    THIRTEEN(13),
    FOURTEEN(14),

    FIFTEEN(15),
    SIXTEEN(16),
    SEVENTEEN(17),
    EIGHTEEN(18),
    NINETEEN(19),
    TWENTY(20),
    TWENTYONE(21),
    TWENTYTWO(22),
    TWENTYTHREE(23),
    TWENTYFOUR(0);


    public final Integer hour;

    SettlementPeriodHours(Integer hour) {
        this.hour=hour;
    }

    public SettlementPeriodHours upShift(){
        if(this.equals(FOUR_ADDITIONAL)|| this.equals(ALL_HOURS)){
            return this;
        }
        int ordinal = this.ordinal();
        if(ordinal>2&& ordinal<6) {

            return SettlementPeriodHours.values()[ordinal +2];
        }
        return SettlementPeriodHours.values()[ordinal +1];

    }

    public SettlementPeriodHours downShift(){
        if(this.equals(THREE_ADDITIONAL)|| this.equals(ALL_HOURS)){
            return this;
        }
        int ordinal = this.ordinal();
        if(ordinal<8&& ordinal>4) {

            return SettlementPeriodHours.values()[ordinal -2];
        }
        return SettlementPeriodHours.values()[ordinal -1];
    }

}
