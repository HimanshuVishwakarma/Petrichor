package com.himanshu.petrichor;

/**
 * Created by Himanshu on 9/13/2017.
 */

public class Users {

    public String name, status, image, thumb_image;

    public Users() {

    }

    public Users(String name, String status, String image, String thumb_image) {
        this.name = name;
        this.status = status;
        this.image = image;
        this.thumb_image = thumb_image;
    }

    public String getThumb_image() {
        return thumb_image;
    }

    public void setThumb_image(String thumb_image) {
        this.thumb_image = thumb_image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
