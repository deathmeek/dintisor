package upb.com.smarttooth;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

class XNumberFormat extends NumberFormat {


    private final DataFrame frame;

    XNumberFormat(DataFrame f) {
        this.frame = f;
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
        return format(((long) value), buffer, field);
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition field) {
        if (value < frame.valuesDate.length)
            return new StringBuffer(Config.dateformatOut.format(frame.valuesDate[((int) value)]));
        return new StringBuffer("" + value);
    }

    @Override
    public Number parse(String string, ParsePosition position) {
        return null;
    }
}
