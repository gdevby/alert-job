package by.gdev.alert.job.parser.domain.rss;

import java.util.Date;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import by.gdev.alert.job.parser.adapter.DateAdapter;
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
