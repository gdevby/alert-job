package by.gdev.alert.job.parser.service.order;

import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;

import java.util.List;

public interface SiteParser {
    List<OrderDTO> parse();
    SiteName getSiteName();
    boolean isActive();
}
