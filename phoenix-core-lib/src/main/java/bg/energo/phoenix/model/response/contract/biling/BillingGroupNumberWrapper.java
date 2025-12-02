package bg.energo.phoenix.model.response.contract.biling;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor

public class BillingGroupNumberWrapper implements Comparable<BillingGroupNumberWrapper> {

    private Integer number;

    public BillingGroupNumberWrapper(String number) {
        this.number = Integer.parseInt(number);
    }

    @Override
    public int compareTo(BillingGroupNumberWrapper o) {
        return this.getNumber().compareTo(o.getNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillingGroupNumberWrapper that)) return false;

        return getNumber().equals(that.getNumber());
    }

    @Override
    public int hashCode() {
        return getNumber().hashCode();
    }
}
