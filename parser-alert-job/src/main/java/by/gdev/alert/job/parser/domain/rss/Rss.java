package by.gdev.alert.job.parser.domain.rss;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class Rss {
	private Channel channel;

}
