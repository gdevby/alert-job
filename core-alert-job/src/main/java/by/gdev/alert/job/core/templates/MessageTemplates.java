package by.gdev.alert.job.core.templates;

import by.gdev.alert.job.core.model.category.CategoryChangeDTO;
import by.gdev.alert.job.core.service.change.dto.ModuleInfo;
import by.gdev.alert.job.core.service.change.dto.RemovedCategoryInfo;
import by.gdev.alert.job.core.service.change.dto.SiteInfo;
import by.gdev.alert.job.core.service.change.dto.UserInfo;
import by.gdev.alert.job.core.service.cleanup.UserModuleCleanupData;

import java.util.List;

//Класс для определения шаблонов писем в различных ситуациях
public final class MessageTemplates {

    private MessageTemplates() {}

    public static final class Cleanup {

        private static final  String CATEGORY_REMOVE_MESSAGE_FULL = """
        Уважаемый пользователь!
        Мы обновили категории сайта %s.

        Некоторые категории были удалены или изменены.
        Были удалены следующие ключевые слова из Ваших модулей:

        %s

        Вам необходимо заново настроить фильтры поиска Ваших заказов.
        """;

        private static final String CATEGORY_REMOVE_MESSAGE_ONLY_CATEGORIES = """
        Уважаемый пользователь!
        Мы обновили категории сайта %s.

        Некоторые категории были удалены или изменены.
        Ваши модули были затронуты, так как категории были обновлены:

        %s

        Вам необходимо заново настроить фильтры поиска Ваших заказов.
        """;

        private Cleanup() {}

        public static String buildTextMessage(
                String siteName,
                List<UserModuleCleanupData> modules,
                boolean wordsDeleted
        ) {
            StringBuilder sb = new StringBuilder();

            for (UserModuleCleanupData m : modules) {
                sb.append("Модуль ").append(m.getModuleName()).append(".\n");

                // категории
                if (!m.getCategories().isEmpty()) {
                    sb.append("  Категории:\n");
                    m.getCategories().stream()
                            .map(dto -> dto.subCategoryName() != null
                                    ? "    • " + dto.categoryName() + " → " + dto.subCategoryName()
                                    : "    • " + dto.categoryName())
                            .distinct()
                            .forEach(line -> sb.append(line).append("\n"));
                }

                // Слова выводим ТОЛЬКО если они есть
                if (!m.getPositiveWords().isEmpty() || !m.getNegativeWords().isEmpty()) {
                    sb.append("  Позитивные: ")
                            .append(String.join(", ", m.getPositiveWords()))
                            .append("\n")
                            .append("  Негативные: ")
                            .append(String.join(", ", m.getNegativeWords()))
                            .append("\n");
                }
                sb.append("\n");
            }
            String modulesBlock = sb.toString().trim();

            String msg;

            if (wordsDeleted) {
                // Используем FULL — когда есть удалённые слова
                msg = CATEGORY_REMOVE_MESSAGE_FULL.formatted(
                        siteName,
                        modulesBlock
                );
            } else {
                // Используем ONLY_CATEGORIES — когда слов нет, но модули есть
                msg = CATEGORY_REMOVE_MESSAGE_ONLY_CATEGORIES.formatted(
                        siteName,
                        modulesBlock
                );
            }

            return msg;
        }

        public static String buildCleanupHtmlMessage(
                String siteName,
                List<UserModuleCleanupData> modules,
                boolean wordsDeleted
        ) {

            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");

            for (UserModuleCleanupData m : modules) {

                sb.append("<li>");
                sb.append("<b>").append(m.getModuleName()).append("</b><br>");

                // категории
                if (!m.getCategories().isEmpty()) {
                    sb.append("Категории:<br><ul>");
                    m.getCategories().stream()
                            .map(dto -> dto.subCategoryName() != null
                                    ? "<li>" + dto.categoryName() + " → " + dto.subCategoryName() + "</li>"
                                    : "<li>" + dto.categoryName() + "</li>")
                            .distinct()
                            .forEach(sb::append);
                    sb.append("</ul>");
                }

                // слова
                if (!m.getPositiveWords().isEmpty() || !m.getNegativeWords().isEmpty()) {

                    if (!m.getPositiveWords().isEmpty()) {
                        sb.append("Позитивные: <b>")
                                .append(String.join(", ", m.getPositiveWords()))
                                .append("</b><br>");
                    }

                    if (!m.getNegativeWords().isEmpty()) {
                        sb.append("Негативные: <b>")
                                .append(String.join(", ", m.getNegativeWords()))
                                .append("</b><br>");
                    }
                }

                sb.append("</li>");
            }

            sb.append("</ul>");

            String modulesBlock = sb.toString();

            return wordsDeleted
                    ? """
        <p>Уважаемый пользователь!</p>
        <p>Мы обновили категории сайта <b>%s</b>.</p>

        <p>Некоторые категории были удалены или изменены.<br>
        Были удалены следующие ключевые слова из Ваших модулей:</p>

        %s

        <p>Вам необходимо заново настроить фильтры поиска Ваших заказов.</p>
        """.formatted(siteName, modulesBlock)
                    : """
        <p>Уважаемый пользователь!</p>
        <p>Мы обновили категории сайта <b>%s</b>.</p>

        <p>Некоторые категории были удалены или изменены.<br>
        Ваши модули были затронуты, так как категории были обновлены:</p>

        %s

        <p>Вам необходимо заново настроить фильтры поиска Ваших заказов.</p>
        """.formatted(siteName, modulesBlock);
        }
    }

    public static final class CategoryDiff {

        public static String buildCategoryDiffHtml(List<CategoryChangeDTO> changes, List<UserInfo> usersInfo) {
            StringBuilder sb = new StringBuilder();
            sb.append("<h2>Изменения категорий</h2>");
            for (CategoryChangeDTO change : changes) {
                sb.append("<h3 style=\"font-size: 20px; color: #1a73e8; margin-top: 25px;\">")
                        .append("<span style=\"background-color:#00ff00;\"><b>Сайт:</b></span> ")
                        .append(change.siteName())
                        .append("</h3>");
                var diff = change.diff();
                // Новые категории ---
                if (!diff.getNewCategories().isEmpty()) {
                    sb.append("<p><b>Новые категории:</b></p><ul>");
                    diff.getNewCategories().forEach(c ->
                            sb.append("<li>").append(c.getName()).append("</li>")
                    );
                    sb.append("</ul>");
                }

                // Удалённые категории ---
                if (!diff.getRemovedCategories().isEmpty()) {
                    sb.append("<p><b>Удалённые категории:</b></p><ul>");
                    diff.getRemovedCategories().forEach(c ->
                            sb.append("<li>").append(c.getName()).append("</li>")
                    );
                    sb.append("</ul>");
                }

                // Новые подкатегории ---
                if (!diff.getNewSubcategories().isEmpty()) {
                    sb.append("<p><b>Новые подкатегории:</b></p><ul>");
                    diff.getNewSubcategories().forEach(s ->
                            sb.append("<li>")
                                    .append(s.getParentName())
                                    .append(" → ")
                                    .append(s.getSubcategory().getName())
                                    .append("</li>")
                    );
                    sb.append("</ul>");
                }

                // Удалённые подкатегории ---
                if (!diff.getRemovedSubcategories().isEmpty()) {
                    sb.append("<p><b>Удалённые подкатегории:</b></p><ul>");
                    diff.getRemovedSubcategories().forEach(s ->
                            sb.append("<li>")
                                    .append(s.getParentName())
                                    .append(" → ")
                                    .append(s.getSubcategory().getName())
                                    .append("</li>")
                    );
                    sb.append("</ul>");
                }

                // Перемещённые подкатегории ---
                if (!diff.getMovedSubcategories().isEmpty()) {
                    sb.append("<p><b>Перемещённые подкатегории:</b></p><ul>");
                    diff.getMovedSubcategories().forEach(m ->
                            sb.append("<li>")
                                    .append(m.getOldParentName())
                                    .append(" → ")
                                    .append(m.getNewParentName())
                                    .append(": ")
                                    .append(m.getSubcategory().getName())
                                    .append("</li>")
                    );
                    sb.append("</ul>");
                }

                sb.append("<hr>");
            }

            // ДОБАВЛЯЕМ ВЫВОД usersInfo ---
            if (usersInfo != null && !usersInfo.isEmpty()) {
                sb.append("<h2>Пользователи и модули с удаляемыми категориями</h2>");
                for (UserInfo userInfo : usersInfo) {
                    sb.append("<h3 style=\"margin-top: 20px;\">Пользователь: ")
                            .append(userInfo.user().getEmail())
                            .append("</h3>");

                    for (SiteInfo site : userInfo.sites()) {
                        sb.append("<h4 style=\"margin-left: 10px;\">")
                                .append("<span style=\"background-color:#00ff00;\"><b>Сайт:</b></span> ")
                                .append(site.siteName())
                                .append("</h4>");

                        for (ModuleInfo module : site.modules()) {
                            sb.append("<p style=\"margin-left: 20px;\">")
                                    .append("<span style=\"background-color:#ffff00;\"><b>Модуль:</b></span> ")
                                    .append(module.moduleName())
                                    .append("</p>");

                            sb.append("<ul style=\"margin-left: 40px;\">");
                            for (RemovedCategoryInfo rc : module.removed()) {

                                sb.append("<li>");

                                if (rc.subcategoryName() == null) {
                                    sb.append(rc.categoryName());
                                } else {
                                    sb.append(rc.categoryName())
                                            .append(" → ")
                                            .append(rc.subcategoryName());
                                }
                                sb.append("</li>");
                            }
                            sb.append("</ul>");
                        }
                    }
                }
            }
            return sb.toString();
        }

        public static String buildCategoryDiffText(List<CategoryChangeDTO> changes, List<UserInfo> usersInfo) {
            StringBuilder sb = new StringBuilder();
            sb.append("Изменения категорий\n");
            for (CategoryChangeDTO change : changes) {
                sb.append("\nСайт: ").append(change.siteName()).append("\n");
                var diff = change.diff();
                if (!diff.getNewCategories().isEmpty()) {
                    sb.append("Новые категории:\n");
                    diff.getNewCategories().forEach(c ->
                            sb.append(" - ").append(c.getName()).append("\n")
                    );
                }

                if (!diff.getRemovedCategories().isEmpty()) {
                    sb.append("Удалённые категории:\n");
                    diff.getRemovedCategories().forEach(c ->
                            sb.append(" - ").append(c.getName()).append("\n")
                    );
                }

                if (!diff.getNewSubcategories().isEmpty()) {
                    sb.append("Новые подкатегории:\n");
                    diff.getNewSubcategories().forEach(s ->
                            sb.append(" - ").append(s.getParentName())
                                    .append(" → ").append(s.getSubcategory().getName()).append("\n")
                    );
                }

                if (!diff.getRemovedSubcategories().isEmpty()) {
                    sb.append("Удалённые подкатегории:\n");
                    diff.getRemovedSubcategories().forEach(s ->
                            sb.append(" - ").append(s.getParentName())
                                    .append(" → ").append(s.getSubcategory().getName()).append("\n")
                    );
                }

                if (!diff.getMovedSubcategories().isEmpty()) {
                    sb.append("Перемещённые подкатегории:\n");
                    diff.getMovedSubcategories().forEach(m ->
                            sb.append(" - ").append(m.getOldParentName())
                                    .append(" → ").append(m.getNewParentName())
                                    .append(": ").append(m.getSubcategory().getName()).append("\n")
                    );
                }
            }

            if (usersInfo != null && !usersInfo.isEmpty()) {
                sb.append("\nПользователи и модули с удалёнными категориями:\n");
                for (UserInfo userInfo : usersInfo) {
                    sb.append("\nПользователь: ").append(userInfo.user().getEmail()).append("\n");
                    for (SiteInfo site : userInfo.sites()) {
                        sb.append(" Сайт: ").append(site.siteName()).append("\n");

                        for (ModuleInfo module : site.modules()) {
                            sb.append("  Модуль: ").append(module.moduleName()).append("\n");

                            for (RemovedCategoryInfo rc : module.removed()) {
                                if (rc.subcategoryName() == null) {
                                    sb.append("   - ").append(rc.categoryName()).append("\n");
                                } else {
                                    sb.append("   - ").append(rc.categoryName())
                                            .append(" → ").append(rc.subcategoryName()).append("\n");
                                }
                            }
                        }
                    }
                }
            }
            return sb.toString();
        }

        public static String buildUserNotificationHtml(UserInfo ui) {
            String email = ui.user().getEmail();
            StringBuilder sb = new StringBuilder();
            sb.append("<p>Уважаемый(ая) ").append(email).append("!</p>");
            sb.append("<p>В связи с изменением структуры категорий на сайтах из ваших модулей были удалены следующие категории:</p>");
            sb.append("<ul>");
            for (SiteInfo site : ui.sites()) {
                sb.append("<li>");
                sb.append("<b>Сайт:</b> ").append(site.siteName());
                sb.append("<ul>");
                for (ModuleInfo module : site.modules()) {
                    sb.append("<li>");
                    sb.append("<b>Модуль:</b> ").append(module.moduleName());
                    sb.append("<ul>");
                    for (RemovedCategoryInfo rc : module.removed()) {
                        if (rc.subcategoryName() == null) {
                            sb.append("<li>").append(rc.categoryName()).append("</li>");
                        } else {
                            sb.append("<li>")
                                    .append(rc.categoryName())
                                    .append(" &rarr; ")
                                    .append(rc.subcategoryName())
                                    .append("</li>");
                        }
                    }
                    sb.append("</ul>");
                    sb.append("</li>");
                }
                sb.append("</ul>");
                sb.append("</li>");
            }
            sb.append("</ul>");
            sb.append("<p>Пожалуйста, обновите настройки ваших модулей, чтобы продолжить получать актуальные заказы.</p>");
            return sb.toString();
        }
    }
}
