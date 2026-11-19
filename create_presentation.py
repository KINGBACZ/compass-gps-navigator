import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
import qrcode

# ==== НАСТРОЙКИ: впиши свои данные ====
AUTHOR = "Цыплов Арсений"
GROUP = "Группа: Пдо-34"
DATE = "Дата: 19.01.26"
REPO_URL = "https://github.com/KINGBACZ/compass-gps-navigator"
SCREENSHOTS_DIR = "screenshots"
OUTPUT_FILE = "Презентация_Компас_навигатор.pptx"
# ======================================

PRIMARY = RGBColor(0x00, 0x79, 0x6B)
BACKGROUND = RGBColor(0xF2, 0xF5, 0xF7)
TEXT = RGBColor(0x21, 0x21, 0x21)
GRAY = RGBColor(0x60, 0x7D, 0x8B)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]


def new_slide():
    slide = prs.slides.add_slide(BLANK)
    fill = slide.background.fill
    fill.solid()
    fill.fore_color.rgb = BACKGROUND
    return slide


def add_text(slide, text, left, top, width, height,
             size=18, bold=False, color=TEXT, align=PP_ALIGN.LEFT):
    box = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(height))
    tf = box.text_frame
    tf.word_wrap = True
    for i, line in enumerate(text.split("\n")):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        run = p.add_run()
        run.text = line
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
    return box


def add_title(slide, text):
    add_text(slide, text, 0.6, 0.4, 12.1, 1.0, size=34, bold=True, color=PRIMARY)


def add_bullets(slide, items, size=22):
    box = slide.shapes.add_textbox(Inches(0.8), Inches(1.7), Inches(11.7), Inches(5.0))
    tf = box.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(14)
        run = p.add_run()
        run.text = "• " + item
        run.font.size = Pt(size)
        run.font.color.rgb = TEXT


def add_screenshot_slide(title, filename, caption):
    slide = new_slide()
    add_title(slide, title)
    path = os.path.join(SCREENSHOTS_DIR, filename)
    if os.path.exists(path):
        pic = slide.shapes.add_picture(path, 0, Inches(1.5), height=Inches(5.2))
        pic.left = int((prs.slide_width - pic.width) / 2)
    else:
        add_text(slide, f"Нет файла {path}", 0.6, 3.5, 12.1, 1,
                 size=20, color=GRAY, align=PP_ALIGN.CENTER)
        print(f"ВНИМАНИЕ: не найден файл {path}")
    add_text(slide, caption, 0.6, 6.8, 12.1, 0.5,
             size=18, color=GRAY, align=PP_ALIGN.CENTER)


# 1. Титульный
s = new_slide()
add_text(s, "Компас и офлайн-навигатор по точкам", 0.8, 2.2, 11.7, 1.5,
         size=40, bold=True, color=PRIMARY, align=PP_ALIGN.CENTER)
add_text(s, f"{AUTHOR}\n{GROUP}\n{DATE}", 0.8, 4.2, 11.7, 1.5,
         size=22, color=TEXT, align=PP_ALIGN.CENTER)

# 2. Проблема
s = new_slide()
add_title(s, "Проблема")
add_bullets(s, [
    "Как вернуться к нужному месту без интернета и сложных карт?",
    "Примеры: дача, грибное место, парковка, точка в лесу",
    "Телефон есть, но связи и карт часто нет",
])

# 3. Решение
s = new_slide()
add_title(s, "Решение")
add_bullets(s, [
    "Приложение показывает направление и расстояние до сохранённой точки",
    "Работает офлайн, нужен только GPS",
    "Компас и стрелка на цель в одном экране",
])

# 4. Функции
s = new_slide()
add_title(s, "Функции")
add_bullets(s, [
    "Компас с направлением в градусах",
    "Сохранение точек по названию и координатам",
    "Выбор точки как цели и стрелка на неё",
    "Расстояние до цели",
    "Список точек с удалением",
    "Работа без интернета",
], size=22)

# 5. Альтернативы (таблица)
s = new_slide()
add_title(s, "Альтернативы")
rows = [
    ["Приложение", "Офлайн", "Компас к точке", "Сохранение точек", "Минусы"],
    ["Google Maps", "Частично", "Нет", "Да", "Много лишнего, нужен аккаунт"],
    ["OsmAnd", "Да", "Да", "Да", "Сложный интерфейс, большие карты"],
    ["Organic Maps", "Да", "Нет", "Да (закладки)", "Нужна карта, избыточно для одной цели"],
    ["Gaia GPS", "Да", "Нет", "Да", "Платная подписка"],
    ["Обычный компас", "Да", "Нет", "Нет", "Нет расстояния и точек"],
    ["Наше приложение", "Да", "Да", "Да", "Нет карты, только стрелка и расстояние"],
]
table = s.shapes.add_table(len(rows), len(rows[0]),
                           Inches(0.6), Inches(1.6), Inches(12.1), Inches(4.8)).table
for r, row in enumerate(rows):
    for c, value in enumerate(row):
        cell = table.cell(r, c)
        cell.text = value
        for p in cell.text_frame.paragraphs:
            for run in p.runs:
                run.font.size = Pt(15)
                run.font.bold = r in (0, len(rows) - 1)

# 6. Преимущества
s = new_slide()
add_title(s, "Наши преимущества")
add_bullets(s, [
    "Простота: три основных действия (добавить, выбрать, идти)",
    "Не нужны карты и интернет",
    "Маленький размер APK",
    "Сразу видно направление и расстояние",
])

# 7. Ограничения
s = new_slide()
add_title(s, "Ограничения")
add_bullets(s, [
    "Нет карты, только стрелка и расстояние",
    "Точность GPS зависит от условий (лес, здания, погода)",
    "Координаты вводятся вручную",
    "Работает только на Android",
])

# 8. Технологии
s = new_slide()
add_title(s, "Технологии")
add_bullets(s, [
    "Kotlin и Android SDK",
    "SensorManager: компас",
    "LocationManager: GPS",
    "SharedPreferences: хранение точек",
    "ViewBinding и Material Design",
])

# 9-12. Скриншоты
add_screenshot_slide("Главный экран", "main.png", "Компас и направление")
add_screenshot_slide("Добавление точки", "add.png", "Сохранение точки по координатам")
add_screenshot_slide("Навигация к цели", "navigate.png", "Расстояние и стрелка на цель")
add_screenshot_slide("Список точек", "list.png", "Список точек и удаление")

# 13. Планы развития
s = new_slide()
add_title(s, "Планы развития")
add_bullets(s, [
    "Карта (OSMDroid) для наглядного отображения точек",
    "Экспорт точек в формат GPX",
    "Тёмная тема",
    "Уведомление о прибытии к точке",
])

# 14. Заключение
s = new_slide()
add_title(s, "Заключение")
add_text(s, "Приложение решает простую задачу: вернуться к сохранённой точке\n"
            "без интернета и карт. Код и APK открыты для скачивания.",
         0.6, 1.7, 7.8, 2.5, size=22)
add_text(s, f"Репозиторий:\n{REPO_URL}", 0.6, 4.6, 7.8, 1.5,
         size=20, bold=True, color=PRIMARY)
qr = qrcode.make(REPO_URL)
qr.save("qr_tmp.png")
s.shapes.add_picture("qr_tmp.png", Inches(9.3), Inches(2.0), width=Inches(3.2))
os.remove("qr_tmp.png")
add_text(s, "QR-код на репозиторий", 9.0, 5.4, 3.8, 0.5,
         size=16, color=GRAY, align=PP_ALIGN.CENTER)

prs.save(OUTPUT_FILE)
print(f"Готово: {OUTPUT_FILE}")