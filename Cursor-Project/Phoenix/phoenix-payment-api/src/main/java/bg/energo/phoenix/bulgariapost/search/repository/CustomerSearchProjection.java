package bg.energo.phoenix.bulgariapost.search.repository;

public interface CustomerSearchProjection {
    String getCustomerNumber();

    String getCustomerName();

    String getCustomerAddress();

    String getCustomerPIN();

    String getCustomerPhone();
}
