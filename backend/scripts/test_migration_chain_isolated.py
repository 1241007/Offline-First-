import os
import shutil
import tempfile
from pathlib import Path
from alembic.config import Config
from alembic import command
import sqlalchemy as sa
from sqlalchemy import create_engine, inspect

# 1. Base directory
backend_dir = Path(__file__).resolve().parent.parent
versions_dir = backend_dir / "migrations" / "versions"

# We will create a temporary folder for alembic testing
temp_dir = tempfile.mkdtemp()
try:
    temp_migrations_dir = Path(temp_dir) / "migrations"
    temp_versions_dir = temp_migrations_dir / "versions"
    temp_versions_dir.mkdir(parents=True)

    # Copy base migrations from backend/migrations/versions
    # 041d77f52987, 0c9897f27723, fd5a2e32997d
    for fname in [
        "041d77f52987_create_conversations_and_messages.py",
        "0c9897f27723_add_composite_index_on_messages.py",
        "fd5a2e32997d_phase1_core_schema_add_course_.py"
    ]:
        shutil.copy(versions_dir / fname, temp_versions_dir / fname)

    # Copy env.py and script.py.mako from backend/migrations
    shutil.copy(backend_dir / "migrations" / "env.py", temp_migrations_dir / "env.py")
    shutil.copy(backend_dir / "migrations" / "script.py.mako", temp_migrations_dir / "script.py.mako")

    # Get master migrations using git show
    import subprocess
    for rev_file in [
        "a1b2c3d4e5f6_add_auth_tables_and_conversation_ownership.py",
        "b2c3d4e5f6a7_add_chat_metadata_columns.py",
        "c4d5e6f7a8b9_add_roadmap_assessment_sessions_and_structure.py"
    ]:
        res = subprocess.run(
            ["git", "show", f"master:backend/migrations/versions/{rev_file}"],
            cwd=str(backend_dir.parent),
            capture_output=True,
            text=True,
            check=True
        )
        (temp_versions_dir / rev_file).write_text(res.stdout, encoding="utf-8")

    # Add reconstructed f1a2b3c4d5e6_add_syllabus_rag_tables.py
    f1a2_content = '''"""add_syllabus_rag_tables

Revision ID: f1a2b3c4d5e6
Revises: c4d5e6f7a8b9
Create Date: 2026-10-10 12:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.types import UserDefinedType

revision: str = 'f1a2b3c4d5e6'
down_revision: Union[str, None] = 'c4d5e6f7a8b9'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

class PGVector(UserDefinedType):
    def __init__(self, dim: int = 384):
        self.dim = dim
    def get_col_spec(self, **kw) -> str:
        return f"vector({self.dim})"

def upgrade() -> None:
    conn = op.get_bind()
    is_postgres = conn.dialect.name == 'postgresql'

    if is_postgres:
        op.execute("CREATE EXTENSION IF NOT EXISTS vector;")

    op.create_table(
        'syllabus_documents',
        sa.Column('id', sa.String(length=36), primary_key=True),
        sa.Column('source', sa.String(length=500), nullable=True),
        sa.Column('title', sa.String(length=500), nullable=False),
        sa.Column('board', sa.String(length=50), nullable=False),
        sa.Column('class_grade', sa.String(length=20), nullable=False),
        sa.Column('subject', sa.String(length=100), nullable=False),
        sa.Column('language', sa.String(length=20), nullable=False, server_default='en'),
        sa.Column('chapter', sa.String(length=200), nullable=True),
        sa.Column('source_url', sa.String(length=1000), nullable=True),
        sa.Column('content_hash', sa.String(length=64), nullable=True),
        sa.Column('corpus_version', sa.String(length=50), nullable=True),
        sa.Column('ingestion_status', sa.String(length=20), nullable=False, server_default='pending'),
        sa.Column('chunk_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index('idx_sd_board_class_subject', 'syllabus_documents', ['board', 'class_grade', 'subject'])

    op.create_table(
        'syllabus_chunks',
        sa.Column('id', sa.String(length=36), primary_key=True),
        sa.Column('document_id', sa.String(length=36), nullable=True),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('chunk_index', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('board', sa.String(length=50), nullable=False),
        sa.Column('class_grade', sa.String(length=20), nullable=False),
        sa.Column('subject', sa.String(length=100), nullable=False),
        sa.Column('chapter', sa.String(length=200), nullable=True),
        sa.Column('section', sa.String(length=200), nullable=True),
        sa.Column('language', sa.String(length=20), nullable=False, server_default='en'),
        sa.Column('textbook_title', sa.String(length=500), nullable=True),
        sa.Column('page_reference', sa.String(length=50), nullable=True),
        sa.Column('source_id', sa.String(length=36), nullable=True),
        sa.Column('corpus_version', sa.String(length=50), nullable=True),
        sa.Column('content_hash', sa.String(length=64), nullable=True),
        sa.Column('embedding_model', sa.String(length=100), nullable=False, server_default='intfloat/multilingual-e5-small'),
        sa.Column('embedding_version', sa.String(length=50), nullable=False, server_default='1.0'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('embedding', PGVector(384).with_variant(sa.JSON(), 'sqlite'), nullable=True),
        sa.ForeignKeyConstraint(['document_id'], ['syllabus_documents.id'], ondelete='CASCADE'),
    )
    op.create_index('idx_sc_board', 'syllabus_chunks', ['board'])
    op.create_index('idx_sc_class', 'syllabus_chunks', ['class_grade'])
    op.create_index('idx_sc_subject', 'syllabus_chunks', ['subject'])
    op.create_index('idx_sc_hash', 'syllabus_chunks', ['content_hash'])

    if is_postgres:
        op.execute("CREATE INDEX idx_syllabus_chunks_embedding ON syllabus_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);")
        op.execute("CREATE INDEX idx_syllabus_chunks_fts ON syllabus_chunks USING gin (to_tsvector('english'::regconfig, content));")

def downgrade() -> None:
    conn = op.get_bind()
    is_postgres = conn.dialect.name == 'postgresql'
    if is_postgres:
        op.execute("DROP INDEX IF EXISTS idx_syllabus_chunks_fts;")
        op.execute("DROP INDEX IF EXISTS idx_syllabus_chunks_embedding;")
    op.drop_table('syllabus_chunks')
    op.drop_table('syllabus_documents')
'''
    (temp_versions_dir / "f1a2b3c4d5e6_add_syllabus_rag_tables.py").write_text(f1a2_content, encoding="utf-8")

    # Add corrected e3b8a1c92d54 chained to f1a2b3c4d5e6
    e3b8_content = '''"""Add course_categories slug, courses prerequisites, lessons resources, and user_revision_items

Revision ID: e3b8a1c92d54
Revises: f1a2b3c4d5e6
Create Date: 2026-10-14 23:55:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = 'e3b8a1c92d54'
down_revision: Union[str, None] = 'f1a2b3c4d5e6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

def upgrade() -> None:
    # 1. Add slug to course_categories with safe canonical CASE backfill
    op.add_column('course_categories', sa.Column('slug', sa.String(length=100), nullable=True))
    op.execute(
        sa.text("""
            UPDATE course_categories SET slug = CASE name
                WHEN 'Programming' THEN 'programming'
                WHEN 'Data & AI' THEN 'data-ai'
                WHEN 'Development' THEN 'development'
                WHEN 'Computer Science' THEN 'computer-science'
                WHEN 'Cloud & Cybersecurity' THEN 'cloud-cybersecurity'
                WHEN 'Practical & Career Skills' THEN 'practical-skills'
                ELSE LOWER(REPLACE(REPLACE(name, ' & ', '-'), ' ', '-'))
            END WHERE slug IS NULL;
        """)
    )
    # Ensure any remaining NULLs are defaulted before setting nullable=False
    op.execute(sa.text("UPDATE course_categories SET slug = 'category-' || id WHERE slug IS NULL;"))
    op.alter_column('course_categories', 'slug', nullable=False)
    op.create_unique_constraint('uq_course_categories_slug', 'course_categories', ['slug'])

    # 2. Add prerequisites to courses (portable JSON/JSONB default)
    op.add_column(
        'courses',
        sa.Column(
            'prerequisites',
            postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), 'sqlite'),
            server_default='[]',
            nullable=False
        )
    )

    # 3. Add resource_url and resource_provider to lessons
    op.add_column('lessons', sa.Column('resource_url', sa.Text(), nullable=True))
    op.add_column('lessons', sa.Column('resource_provider', sa.String(length=100), nullable=True))

    # 4. Create user_revision_items table
    op.create_table(
        'user_revision_items',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=255), nullable=False),
        sa.Column('course_id', sa.String(length=36), nullable=False),
        sa.Column('lesson_id', sa.String(length=36), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['course_id'], ['courses.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['lesson_id'], ['lessons.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_user_revision_items_user_lesson', 'user_revision_items', ['user_id', 'lesson_id'], unique=True)
    op.create_index('ix_user_revision_items_user_course', 'user_revision_items', ['user_id', 'course_id'], unique=False)

def downgrade() -> None:
    op.drop_index('ix_user_revision_items_user_course', table_name='user_revision_items')
    op.drop_index('ix_user_revision_items_user_lesson', table_name='user_revision_items')
    op.drop_table('user_revision_items')
    op.drop_column('lessons', 'resource_provider')
    op.drop_column('lessons', 'resource_url')
    op.drop_column('courses', 'prerequisites')
    op.drop_constraint('uq_course_categories_slug', 'course_categories', type_='unique')
    op.drop_column('course_categories', 'slug')
'''
    (temp_versions_dir / "e3b8a1c92d54_add_course_slug_prerequisites_resources_and_revisions.py").write_text(e3b8_content, encoding="utf-8")

    # Create test SQLite database
    db_path = Path(temp_dir) / "test_fresh.db"
    db_url = f"sqlite:///{db_path.as_posix()}"

    # Create alembic.ini in temp_dir
    alembic_ini_path = Path(temp_dir) / "alembic.ini"
    alembic_ini_content = f"""[alembic]
script_location = {temp_migrations_dir.as_posix()}
prepend_sys_path = {backend_dir.as_posix()}
sqlalchemy.url = {db_url}

[loggers]
keys = root,sqlalchemy,alembic

[handlers]
keys = console

[formatters]
keys = generic

[logger_root]
level = WARN
handlers = console
qualname =

[logger_sqlalchemy]
level = WARN
handlers =
qualname = sqlalchemy.engine

[logger_alembic]
level = INFO
handlers =
qualname = alembic

[handler_console]
class = StreamHandler
args = (sys.stderr,)
level = NOTSET
formatter = generic

[formatter_generic]
format = %(levelname)-5.5s [%(name)s] %(message)s
datefmt = %H:%M:%S
"""
    alembic_ini_path.write_text(alembic_ini_content, encoding="utf-8")

    cfg = Config(str(alembic_ini_path))

    print("=== TESTING FRESH SQLITE INSTALLATION (0 -> HEAD) ===")
    command.upgrade(cfg, "head")

    engine = create_engine(db_url)
    insp = inspect(engine)
    tables = sorted(insp.get_table_names())
    print(f"Created {len(tables)} tables successfully in fresh SQLite:")
    for t in tables:
        cols = [c["name"] for c in insp.get_columns(t)]
        print(f"  - {t} ({len(cols)} columns: {', '.join(cols[:4])}...)")

    with engine.connect() as c:
        v = c.execute(sa.text("SELECT version_num FROM alembic_version")).scalar()
        print(f"Final Alembic revision in database: {v}")
        assert v == "e3b8a1c92d54", f"Expected e3b8a1c92d54, got {v}"

    print("\n=== TESTING DOWNGRADE (HEAD -> BASE) ===")
    command.downgrade(cfg, "base")
    insp_after = inspect(engine)
    print(f"Tables remaining after downgrade to base: {insp_after.get_table_names()}")
    with engine.connect() as c:
        v_base = c.execute(sa.text("SELECT version_num FROM alembic_version")).scalar()
        print(f"Alembic revision after downgrade to base: {v_base}")

    print("\n=== TESTING RE-UPGRADE (BASE -> HEAD) ===")
    command.upgrade(cfg, "head")
    with engine.connect() as c:
        v_reup = c.execute(sa.text("SELECT version_num FROM alembic_version")).scalar()
        print(f"Alembic revision after re-upgrade: {v_reup}")
        assert v_reup == "e3b8a1c92d54"

    print("\n=== ALL TEST PASSES COMPLETED WITH ZERO ERRORS ===")

finally:
    shutil.rmtree(temp_dir, ignore_errors=True)
