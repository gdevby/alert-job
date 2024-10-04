package by.gdev.alert.job.parser.domain.rss;

import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class Rss {
	private Channel channel;

}
