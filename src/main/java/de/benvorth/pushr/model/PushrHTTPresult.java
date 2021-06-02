package de.benvorth.pushr.model;

public class PushrHTTPresult {

    public static String STATUS_SUCCESS = "success";
    public static String STATUS_ERROR = "error";

    private String status;
    private String msg;

    public PushrHTTPresult (String status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public String getJSON(){
        return "{" +
            "\"status\":\"" + this.status + "\"," +
            "\"msg\":\"" + this.msg + "\"" +
            "}";
    }

}
