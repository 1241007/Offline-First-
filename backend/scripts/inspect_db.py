import sqlite3
import os

db_path = 'edunova_dev.db'
if not os.path.exists(db_path):
    print(f"{db_path} does not exist.")
    exit(0)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

tables = cursor.execute("SELECT name FROM sqlite_master WHERE type='table';").fetchall()
print('Tables in edunova_dev.db:', [t[0] for t in tables])

if any(t[0] == 'alembic_version' for t in tables):
    vers = cursor.execute('SELECT version_num FROM alembic_version;').fetchall()
    print('Alembic versions:', vers)
else:
    print('alembic_version table not found.')

if any(t[0] == 'courses' for t in tables):
    cols = [col[1] for col in cursor.execute("PRAGMA table_info(courses);").fetchall()]
    print('Courses columns:', cols)
    courses = cursor.execute('SELECT id, slug, name, is_featured FROM courses;').fetchall()
    print(f'Total courses: {len(courses)}')
    for c in courses:
        print(' -', c)

if any(t[0] == 'course_categories' for t in tables):
    cols = [col[1] for col in cursor.execute("PRAGMA table_info(course_categories);").fetchall()]
    print('Categories columns:', cols)
    cats = cursor.execute('SELECT id, name, slug FROM course_categories;').fetchall()
    print('Categories:', cats)

if any(t[0] == 'lessons' for t in tables):
    cols = [col[1] for col in cursor.execute("PRAGMA table_info(lessons);").fetchall()]
    print('Lessons columns:', cols)
    lesson_count = cursor.execute('SELECT count(*) FROM lessons;').fetchone()[0]
    print('Lessons count:', lesson_count)

if any(t[0] == 'user_revision_items' for t in tables):
    print('user_revision_items table exists!')

conn.close()
