package kaveh.bazaarfoursquaresample.Model;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Price {
    private String tier;
    private String message;
    private String currency;

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
