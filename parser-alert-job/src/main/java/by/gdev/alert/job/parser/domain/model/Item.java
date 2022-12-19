package by.gdev.alert.job.parser.domain.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import by.gdev.alert.job.parser.configuration.DateAdapter;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Item {
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date pubDate;
	private String title;
	private String link;
	private String description;
}
