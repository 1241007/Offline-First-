"""Add user_saved_courses and user_saved_roadmaps tables

Revision ID: c7d8e9f0a1b2
Revises: a7c8e9f1b2d3, f1a2b3c4d5e6
Create Date: 2026-10-17 03:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c7d8e9f0a1b2'
down_revision: Union[str, Sequence[str], None] = ('a7c8e9f1b2d3', 'f1a2b3c4d5e6')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create user_saved_courses table
    op.create_table(
        'user_saved_courses',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=255), nullable=False),
        sa.Column('course_id', sa.String(length=36), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['course_id'], ['courses.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_user_saved_courses_user_course', 'user_saved_courses', ['user_id', 'course_id'], unique=True)
    op.create_index('ix_user_saved_courses_user_id', 'user_saved_courses', ['user_id'], unique=False)

    # 2. Create user_saved_roadmaps table
    op.create_table(
        'user_saved_roadmaps',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=255), nullable=False),
        sa.Column('roadmap_id', sa.String(length=36), nullable=False),
        sa.Column('roadmap_type', sa.String(length=50), server_default='system', nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['roadmap_id'], ['roadmaps.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_user_saved_roadmaps_user_roadmap', 'user_saved_roadmaps', ['user_id', 'roadmap_id'], unique=True)
    op.create_index('ix_user_saved_roadmaps_user_id', 'user_saved_roadmaps', ['user_id'], unique=False)


def downgrade() -> None:
    # 1. Drop user_saved_roadmaps
    op.drop_index('ix_user_saved_roadmaps_user_id', table_name='user_saved_roadmaps')
    op.drop_index('ix_user_saved_roadmaps_user_roadmap', table_name='user_saved_roadmaps')
    op.drop_table('user_saved_roadmaps')

    # 2. Drop user_saved_courses
    op.drop_index('ix_user_saved_courses_user_id', table_name='user_saved_courses')
    op.drop_index('ix_user_saved_courses_user_course', table_name='user_saved_courses')
    op.drop_table('user_saved_courses')
