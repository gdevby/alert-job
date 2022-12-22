package by.gdev.alert.job.parser.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageData {
	private int chat_id;
	private String text;
}
