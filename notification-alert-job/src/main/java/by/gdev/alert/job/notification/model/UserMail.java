package by.gdev.alert.job.notification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//TODO moved to common and changed to UserNotification
public class UserMail {

	private String toMail;
	private String message;
	
}
