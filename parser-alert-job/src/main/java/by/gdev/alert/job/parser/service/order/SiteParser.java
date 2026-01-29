package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;

import java.util.List;

public interface SiteParser {
    List<OrderDTO> parse();
    SiteName getSiteName();
    boolean isActive();
}
