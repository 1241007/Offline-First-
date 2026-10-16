"""add_syllabus_documents_and_chunks

Revision ID: f1a2b3c4d5e6
Revises: e1f2a3b4c5d6
Create Date: 2026-10-15 12:30:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f1a2b3c4d5e6'
down_revision: Union[str, None] = 'e1f2a3b4c5d6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    bind = op.get_bind()
    is_postgres = bind.dialect.name == "postgresql"

    # 1. Enable pgvector extension if PostgreSQL
    if is_postgres:
        op.execute("CREATE EXTENSION IF NOT EXISTS vector;")

    # 2. Syllabus documents parent table
    op.create_table(
        'syllabus_documents',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('source', sa.String(length=500), nullable=True),
        sa.Column('title', sa.String(length=500), nullable=False),
        sa.Column('board', sa.String(length=50), nullable=False),
        sa.Column('class_grade', sa.String(length=20), nullable=False),
        sa.Column('subject', sa.String(length=100), nullable=False),
        sa.Column('language', sa.String(length=20), server_default='en', nullable=False),
        sa.Column('chapter', sa.String(length=200), nullable=True),
        sa.Column('source_url', sa.String(length=1000), nullable=True),
        sa.Column('content_hash', sa.String(length=64), nullable=True),
        sa.Column('corpus_version', sa.String(length=50), nullable=True),
        sa.Column('ingestion_status', sa.String(length=20), server_default='pending', nullable=False),
        sa.Column('chunk_count', sa.Integer(), server_default='0', nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_sd_board_class_subject', 'syllabus_documents', ['board', 'class_grade', 'subject'])

    # 3. Syllabus chunks child table
    op.create_table(
        'syllabus_chunks',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('document_id', sa.String(length=36), nullable=True),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('chunk_index', sa.Integer(), server_default='0', nullable=False),
        sa.Column('board', sa.String(length=50), nullable=False),
        sa.Column('class_grade', sa.String(length=20), nullable=False),
        sa.Column('subject', sa.String(length=100), nullable=False),
        sa.Column('chapter', sa.String(length=200), nullable=True),
        sa.Column('section', sa.String(length=200), nullable=True),
        sa.Column('language', sa.String(length=20), server_default='en', nullable=False),
        sa.Column('textbook_title', sa.String(length=500), nullable=True),
        sa.Column('page_reference', sa.String(length=50), nullable=True),
        sa.Column('source_id', sa.String(length=36), nullable=True),
        sa.Column('corpus_version', sa.String(length=50), nullable=True),
        sa.Column('content_hash', sa.String(length=64), nullable=True),
        sa.Column('embedding_model', sa.String(length=100), server_default='intfloat/multilingual-e5-small', nullable=False),
        sa.Column('embedding_version', sa.String(length=50), server_default='1.0', nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['document_id'], ['syllabus_documents.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )

    # 4. Standard indexes for curriculum filtering and deduplication
    op.create_index('idx_sc_board', 'syllabus_chunks', ['board'])
    op.create_index('idx_sc_class', 'syllabus_chunks', ['class_grade'])
    op.create_index('idx_sc_subject', 'syllabus_chunks', ['subject'])
    op.create_index('idx_sc_hash', 'syllabus_chunks', ['content_hash'])

    # 5. PostgreSQL-specific vector and full-text search indexes
    if is_postgres:
        # Add 384-dimensional vector column
        op.execute("ALTER TABLE syllabus_chunks ADD COLUMN IF NOT EXISTS embedding vector(384);")

        # IVFFlat cosine similarity index
        op.execute("""
            CREATE INDEX IF NOT EXISTS idx_syllabus_chunks_embedding 
            ON syllabus_chunks 
            USING ivfflat (embedding vector_cosine_ops)
            WITH (lists = 10);
        """)

        # GIN index for full-text search
        op.execute("""
            CREATE INDEX IF NOT EXISTS idx_syllabus_chunks_fts 
            ON syllabus_chunks 
            USING gin (to_tsvector('english', content));
        """)


def downgrade() -> None:
    bind = op.get_bind()
    is_postgres = bind.dialect.name == "postgresql"

    if is_postgres:
        op.execute("DROP INDEX IF EXISTS idx_syllabus_chunks_fts;")
        op.execute("DROP INDEX IF EXISTS idx_syllabus_chunks_embedding;")

    op.drop_table('syllabus_chunks')
    op.drop_table('syllabus_documents')
