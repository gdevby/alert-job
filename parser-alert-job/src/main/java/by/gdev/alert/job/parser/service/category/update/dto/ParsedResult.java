package by.gdev.alert.job.parser.service.category.update.dto;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;

//контейнер, который связывает параллельное обновление категорий - чтобы не потерять связь job и parsedTree
public record ParsedResult(SiteSourceJob job, SiteDTO parsedTree) {}

