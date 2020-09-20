package ru.pekcherkin.androiduberremake;

import ru.pekcherkin.androiduberremake.Model.DriverInfoModel;

public class Common {
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static final String DRIVERS_LOCATION_REFERENCE = "DriversLocation";

    public static DriverInfoModel currentUser;

    public static String builderWelcomeMessage() {
        if(Common.currentUser != null){
            return new StringBuilder("Добро пожаловать")
                    .append(Common.currentUser.getFirstName())
                    .append(" ")
                    .append(Common.currentUser.getLastName()).toString();
        } else return "";
    }
}
