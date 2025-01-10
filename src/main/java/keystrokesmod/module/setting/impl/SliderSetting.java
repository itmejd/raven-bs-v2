package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.module.setting.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderSetting extends Setting {
    private String settingName;
    private String[] options = null;
    private double defaultValue;
    private double max;
    private double min;
    private double intervals;
    public boolean isString;
    private String suffix = "";
    public boolean canBeDisabled;

    public SliderSetting(String settingName, double defaultValue, double min, double max, double intervals) {
        super(settingName);
        this.settingName = settingName;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isString = false;
    }

    public SliderSetting(String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, String suffix, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, int defaultValue, String[] options) {
        super(settingName);
        this.settingName = settingName;
        this.options = options;
        this.defaultValue = defaultValue;
        this.min = 0;
        this.max = options.length - 1;
        this.intervals = 1;
        this.isString = true;
    }

    public SliderSetting(String settingName, String suffix, int defaultValue, String[] options) {
        this(settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String[] getOptions() {
        return options;
    }

    public String getName() {
        return this.settingName;
    }

    public double getInput() {
        return roundToInterval(this.defaultValue, 4);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setValue(double n) {
        n = correctValue(n, this.min, this.max);
        n = (double) Math.round(n * (1.0D / this.intervals)) / (1.0D / this.intervals);
        this.defaultValue = n;
    }

    public void setValueRaw(double n) {
        this.defaultValue = n;
    }

    public static double correctValue(double v, double i, double a) {
        v = Math.max(i, v);
        v = Math.min(a, v);
        return v;
    }

    public static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
        if (data.has(getName()) && data.get(getName()).isJsonPrimitive()) {
            double newValue = defaultValue;
            try {
                newValue = data.getAsJsonPrimitive(getName()).getAsDouble();
            }
            catch (Exception e) {

            }
            if (newValue == -1) {
                setValueRaw(newValue);
                return;
            }
            setValue(newValue);
        }
    }
}
