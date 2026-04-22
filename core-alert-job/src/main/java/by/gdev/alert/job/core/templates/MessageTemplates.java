package by.gdev.alert.job.core.templates;

import by.gdev.alert.job.core.service.cleanup.UserModuleCleanupData;

import java.util.List;

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
}
