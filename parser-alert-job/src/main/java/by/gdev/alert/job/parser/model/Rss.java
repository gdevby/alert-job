package by.gdev.alert.job.parser.model;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class Rss {
	private Channel channel;

}
