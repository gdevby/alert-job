package by.gdev.alert.job.parser.configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import lombok.SneakyThrows;

public class DateAdapter extends XmlAdapter<String, Date> {

	//example hubr date Wed, 07 Dec 2022 15:07:06 +0300
    private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
	
    @Override
    public String marshal(Date v) {
        return new SimpleDateFormat(DATE_FORMAT).format(v);
    }

    @SneakyThrows
    @Override
    public Date unmarshal(String v) throws ParseException {
    	return new SimpleDateFormat(DATE_FORMAT).parse(v);
    }
}