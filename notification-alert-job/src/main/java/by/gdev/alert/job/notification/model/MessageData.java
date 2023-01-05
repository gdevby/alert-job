package by.gdev.alert.job.notification.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageData {
	private Long chat_id;
	private String text;
}
