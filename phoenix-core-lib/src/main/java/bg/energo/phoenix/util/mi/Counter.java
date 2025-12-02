package bg.energo.phoenix.util.mi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Counter {
    private int count;

    public Counter(int count) {
        this.count = count;
    }
}

