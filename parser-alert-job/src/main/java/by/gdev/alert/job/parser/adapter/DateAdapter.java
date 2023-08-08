package by.gdev.alert.job.parser.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DateAdapter extends XmlAdapter<String, Date> {

	// example hubr date: Wed, 07 Dec 2022 15:07:06 +0300
	// fl.ru: Mon, 07 Aug 2023 06:13:59 GMT
	private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

	@Override
	public String marshal(Date v) {
		return new SimpleDateFormat(DATE_FORMAT).format(v);
	}

	@Override
	public Date unmarshal(String v) throws ParseException {
		return new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH).parse(v);
	}
}