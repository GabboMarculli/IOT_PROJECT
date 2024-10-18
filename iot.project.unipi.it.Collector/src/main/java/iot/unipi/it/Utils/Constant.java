package iot.unipi.it.Utils;

import iot.unipi.it.DTO.MeasurementsDTO;

public class Constant {
    private static double TARGET_TEMPERATURE;
    private static MeasurementsDTO LAST_MEASUREMENT;

    public Constant()
    {
    	Constant.LAST_MEASUREMENT   = new MeasurementsDTO(0,null,0,0);
    }

    public static double getTARGET_TEMPERATURE() {
        return TARGET_TEMPERATURE;
    }

    public static MeasurementsDTO getLAST_MEASUREMENT() {
        return LAST_MEASUREMENT;
    }

    public static void setLAST_MEASUREMENT(MeasurementsDTO LAST_MEASUREMENT) {
        Constant.LAST_MEASUREMENT = LAST_MEASUREMENT;
    }

    public static void setTARGET_TEMPERATURE(double TARGET_TEMPERATURE) {
        Constant.TARGET_TEMPERATURE = TARGET_TEMPERATURE;
    }
}

