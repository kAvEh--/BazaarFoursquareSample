package kaveh.bazaarfoursquaresample.Model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class VenueDetail extends Venue {

    private double rating;
    private String ratingColor;
    private Contact contact;
    private Price price;
    private VenuePhoto bestPhoto;

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getRatingColor() {
        return ratingColor;
    }

    public void setRatingColor(String ratingColor) {
        this.ratingColor = ratingColor;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public VenuePhoto getBestPhoto() {
        return bestPhoto;
    }

    public void setBestPhoto(VenuePhoto bestPhoto) {
        this.bestPhoto = bestPhoto;
    }
}
