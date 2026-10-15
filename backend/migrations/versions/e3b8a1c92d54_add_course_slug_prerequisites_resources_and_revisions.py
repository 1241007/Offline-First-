"""Add course_categories slug, courses prerequisites, lessons resources, and user_revision_items

Revision ID: e3b8a1c92d54
Revises: fd5a2e32997d
Create Date: 2026-10-14 23:55:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = 'e3b8a1c92d54'
down_revision: Union[str, None] = 'fd5a2e32997d'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add slug to course_categories with safe backfill for existing rows
    op.add_column('course_categories', sa.Column('slug', sa.String(length=100), nullable=True))
    op.execute(
        sa.text("UPDATE course_categories SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL")
    )
    op.alter_column('course_categories', 'slug', nullable=False)
    op.create_unique_constraint('uq_course_categories_slug', 'course_categories', ['slug'])

    # 2. Add prerequisites to courses (defaulting to empty JSON array)
    op.add_column(
        'courses',
        sa.Column('prerequisites', postgresql.JSONB(astext_type=sa.Text()), server_default='[]', nullable=False)
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
    # 1. Drop user_revision_items
    op.drop_index('ix_user_revision_items_user_course', table_name='user_revision_items')
    op.drop_index('ix_user_revision_items_user_lesson', table_name='user_revision_items')
    op.drop_table('user_revision_items')

    # 2. Drop lesson resource fields
    op.drop_column('lessons', 'resource_provider')
    op.drop_column('lessons', 'resource_url')

    # 3. Drop courses prerequisites
    op.drop_column('courses', 'prerequisites')

    # 4. Drop course_categories slug
    op.drop_constraint('uq_course_categories_slug', 'course_categories', type_='unique')
    op.drop_column('course_categories', 'slug')
