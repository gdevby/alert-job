             
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(1, 'Менеджмент');          
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(2, 'Разработка сайтов');           
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(3, 'Дизайн и Арт');        
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(5, 'Программирование');            
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(6, 'Оптимизация (SEO)');               
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(7, 'Переводы');    
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(8, 'Тексты');              
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(9, '3D Графика');      
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(10, 'Фотография');         
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(11, 'Аудио/Видео');        
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(12, 'Реклама и Маркетинг');    
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(13, 'Аутсорсинг и консалтинг');
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(14, 'Архитектура/Интерьер');           
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(16, 'Разработка игр');         
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(17, 'Полиграфия');         
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(19, 'Анимация и флеш');        
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(20, 'Инжиниринг');         
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(22, 'Обучение и консультации');
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(23, 'Мобильные приложения');           
INSERT IGNORE INTO fl_category(ID,NATIVE_LOC_NAME) VALUES(24, 'Сети и инфосистемы');          
       
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(1, 'FLRU', 'https://www.fl.ru/projects/', TRUE, TRUE);
--INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(2, 'HABR', 'https://freelance.habr.com/tasks', TRUE, TRUE); not in use anymore
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(3, 'FREELANCE.RU', 'https://freelance.ru/project/search/pro', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(4, 'WEBLANCER.NET', 'https://www.weblancer.net/freelance/', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(5, 'FREELANCEHUNT.COM', 'https://freelancehunt.com/projects', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(6, 'YOUDO.COM', 'https://youdo.com/tasks-all-opened-all', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(7, 'KWORK.RU', 'https://kwork.ru/categories', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(8, 'FREELANCER.COM', 'https://www.freelancer.com/api/projects/0.1/projects/all', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(9, 'TRUELANCER.COM', 'https://api.truelancer.com/api/v1/projects', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(10, 'PEOPLEPERHOUR.COM', 'https://www.peopleperhour.com', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(11, 'WORKSPACE.RU', 'https://workspace.ru/tenders', TRUE, TRUE);
INSERT IGNORE INTO site_source_job(id,name,parseduri,parse,active) VALUES(12, 'WORKANA.COM', 'https://www.workana.com/jobs', TRUE, TRUE);
